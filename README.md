# handletility

Java-библиотека для построения **обработчиков** (`Handler`) и **преобразователей** (`Processor`) с единообразной моделью результата: `DONE` / `SKIPPED` / `ERROR`.

Подходит для пайплайнов команд, маршрутизации по ключу, цепочек side-effect’ов и трансформаций без разрастания `if/else` и try/catch.

## Демо

Один сценарий — почти весь синтаксис API:

```java
record Order(String id, String userId, String payload) {}

// Function → ProcessorCompose: имя в логах, MDC, hooks, recover
Processor<Order, Integer> parseAmount =
        Processors.compose((Order o) -> Integer.valueOf(o.payload()))
                .named("parseAmount")
                .logging(Level.DEBUG)
                .mdc("orderId", Order::id)
                .mdc("userId", Order::userId)
                .when(o -> o.payload() != null) // false → SKIPPED, hooks не бегут
                .before(o -> metrics.mark("parse.start"))
                .after(result -> metrics.mark(result.isDone() ? "parse.ok" : "parse.fail"))
                .onException(failed -> Processed.of(0)) // recover
                .build();

// Consumer → HandlerCompose: тот же стек обёрток
Handler<Order> persist =
        Handlers.compose((Order o) -> repository.save(o))
                .named("persist")
                .logging()
                .mdc("orderId", Order::id)
                .before(audit::before)
                .after(handled -> audit.after(handled))
                .onException(failed -> Handled.error(failed.exception()))
                .build();

// Цепочка handler’ов + модификаторы остановки
Handler<Order> pipeline =
        HandlerList.<Order>builder()
                .handler(persist)
                .handler(Handlers.compose(o -> notify(o)).build())
                .modifier(HandlerModifier.CONTINUE_ON_DONE)
                .modifier(HandlerModifier.CONTINUE_ON_ERROR)
                .build();

// Маршрутизация по ключу
Handler<Order> router =
        HandlerMap.<String, Order>handlerMap()
                .handler(new IdentifiedByConstHandler<>("create", Handlers.compose(this::create).build()))
                .handler(new IdentifiedByConstHandler<>("cancel", Handlers.compose(this::cancel).build()))
                .keyExtractor(new SimpleProcessor<>(Order::payload))
                .defaultHandler(o -> Handled.skipped())
                .build();

// Processor chain: of / thenCompose / thenApply / thenHandle (skip/error пробрасываются)
Processor<String, String> doubleAsString =
        Processors.of(Integer::parseInt)
                .thenCompose(Processors.of(n -> n * 2))
                .thenApply(Object::toString);

Handler<String> parseThenPersist =
        Processors.of(json -> gson.fromJson(json, Order.class)).thenHandle(persist);

// Processed: map / flatMap / thenHandle / orElseThrow
Handled handled =
        parseAmount
                .process(order)
                .map(amount -> amount * 100)
                .flatMap(Processors.compose(cents -> taxService.apply(cents)).build())
                .thenHandle(Handlers.compose((Integer c) -> charge(c)).build());

Integer cents = parseAmount.process(order).orElseThrow();
Optional<Integer> maybe = parseAmount.process(order).toOptional();
```

Слои compose при `build()`: `Mdc(Logging(Catching(When(Around(core)))))`. `When` и `Around` в стеке только если слот задан. Исключение из функционального core и из `before`/`after`(consumer) доходит до `Catching` и вашего `.onException` (исходный тип, в том числе checked), не throws. `Error` не ловится. `Handlers.of` / `Processors.of` и `Simple*` сами переводят исключение в `ERROR` и до `onException` композера не доходят.

## Требования

- Java 17+
- Gradle 8.8 (wrapper в репозитории)

## Подключение

Maven Central (`groupId` = `io.github.v1as`):

```gradle
dependencies {
    implementation 'io.github.v1as:handletility:0.0.1'
}
```

Локально:

```bash
./gradlew publishToMavenLocal
```

## Быстрый старт

### Handler

```java
Handler<String> print =
        new SimpleHandler<>(s -> !s.isBlank(), System.out::println);

Handled result = print.handle("hello");
// DONE → Handled.ok()
// не подходит check → SKIPPED
// исключение → ERROR
```

### Processor

```java
Processor<String, Integer> parse =
        new SimpleProcessor<>(s -> s.matches("\\d+"), Integer::valueOf);

Processed<Integer> out = parse.process("42");
Integer value = out.orElseThrow(); // 42
```

### Именование, логирование, MDC (compose)

Порядок слоёв при `build()` фиксирован: `Mdc(Logging(Catching(When(Around(core)))))` — MDC снаружи logging, чтобы в логах был контекст; Catching всегда есть и ловит `Exception` (не `Error`). `When` стоит внутри Catching и снаружи Around: предикат ложен → `SKIPPED`, before/after и core не вызываются. `When`, `Around`, logging и MDC опускаются, если слот не задан. Результат — `Handler` / `Processor`.

`.named(...)` — опциональный суффикс логгера. Сам логгер — класс ядра, переданного в `compose`, например `CreateNoteCommandHandler[note]`. Если имя не задано, суффикс не добавляется.  
`.when(ThrowablePredicate)` — входной guard; повторный вызов затирает предыдущий. Исключение из предиката, в том числе checked, идёт в `.onException` исходным типом. Это не `Processor.filter`: `filter` режет успешный выход.  
`.before(...)` / `.after(...)` — hooks вокруг core (внутри `When`): before получает input, after — результат (`Handled` / `Processed`), на возврат не влияет. Исключение из consumer-хука останавливает операцию и уходит в `.onException`.  
`.onException(...)` — кастомный маппинг `Failed` → результат (можно recover в `ok()` / успешный `Processed`); без вызова — `LoggingException*` (warn + `ERROR`). Для `compose(функция)` и consumer-хуков исключение доходит сюда. `of()` и явный `Simple*` держат свою границу `ERROR` и этот слот не вызывают.

Цепочка типов — на самом `Processor` (не в compose): `Processors.of(fn).thenCompose(...).thenApply(...).filter(...).thenHandle(...)`. Skip/error пробрасываются, как у `Processed`; `filter` при неуспехе предиката даёт `SKIPPED`.

```java
Handler<Request> traced = Handlers.compose(delegate) // или Consumer<Request>
        .named("persist")             // суффикс логгера; без него суффикса нет
        .logging()                    // DEBUG по умолчанию; или .logging(Level.TRACE)
        .mdc("requestId", Request::id)
        .mdc("userId", Request::userId)
        .when(req -> req.id() != null) // false → SKIPPED, hooks не бегут
        .before(auditBefore)          // Handler или Consumer
        .after(auditAfter)            // Handler<Handled> или Consumer<Handled>
        .onException(failed -> Handled.error(failed.exception())) // опционально
        .build();
```

Для кастомного `Processor` (SKIP/ERROR): `new MdcEntry<>("key", processor)` или `.mdc(entry)`.

Аналогично для процессоров (`Function` / `Processor` на входе, hooks — `Handler`/`Consumer`):

```java
Processor<String, String> pipeline =
        Processors.of(Integer::parseInt)
                .thenCompose(Processors.of(n -> n * 2))
                .thenApply(Object::toString);

Processor<String, String> traced =
        Processors.compose(pipeline)
                .before(pre)
                .after(post)                  // Handler|Consumer<Processed<?>>
                .onException(failed -> Processed.error(failed.exception()))
                .logging()
                .build();
```

Обёртки вручную: `IdentifiedByConst*`, `Around*`, `Catching*`, `When*`, `Propagating*`, `Named*` (только для логов), `Logging*`, `Mdc*` / `MdcEntry`.

### Цепочка handler’ов

```java
Handler<String> pipeline = HandlerList.<String>builder()
        .handler(new SimpleHandler<>(log::info))
        .handler(new SimpleHandler<>(this::persist))
        .modifier(HandlerModifier.CONTINUE_ON_DONE)   // не останавливаться на первом DONE
        .modifier(HandlerModifier.CONTINUE_ON_ERROR)  // собирать ошибки дальше
        .build();
```

По умолчанию `HandlerList` останавливается на первом `DONE` и на первом `ERROR`.

### Маршрутизация по ключу

```java
Handler<Command> router = HandlerMap.<String, Command>handlerMap()
        .handler(new HelloCommand())   // IdentifiedHandler<"hello", Command>
        .handler(new ByeCommand())
        .keyExtractor(new SimpleProcessor<>(Command::name))
        .defaultHandler(cmd -> Handled.skipped())
        .build();
```

Аналог для процессоров — `ProcessorMap` + `IdentifiedProcessor`.

### Цепочка Processor / Handler

```java
// of + then* / filter: мягкая семантика (skip/error пробрасываются; filter → SKIPPED)
Processor<String, Integer> doubled =
        Processors.of(Integer::parseInt)
                .filter(n -> n > 0)
                .thenCompose(Processors.of(n -> n * 2));

Handler<String> asIntThenHandle =
        Processors.of(Integer::valueOf).thenHandle(Handlers.of(n -> sum.addAndGet(n)));

// Явные обёртки с тем же контрактом
Handler<String> mapped =
        new MappedHandler<>(Processors.of(Integer::valueOf), Handlers.of(n -> sum.addAndGet(n)));
```

`Processed` умеет продолжать цепочку:

```java
Processed.of(value)
        .flatMap(nextProcessor)
        .thenHandle(sideEffectHandler);
```

### Before / after

`AroundHandler` / `AroundProcessor` — before(input) → main → after(result); результат всегда от main. Hooks — `Handler` (у processor тоже).

## Структура пакета

```
ru.operation
├── handler/          # Handler, Handled, AbstractHandler, Handlers, HandlerCompose
│   └── impl/
│       ├── wrap/     # NamedHandler, LoggingHandler
│       ├── list/     # HandlerList, modifiers
│       └── map/      # HandlerMap
├── processor/        # Processor, Processed, AbstractProcessor, Processors, ProcessorCompose
│   └── impl/
│       ├── wrap/     # NamedProcessor, LoggingProcessor
│       ├── list/     # ProcessorList
│       └── map/      # ProcessorMap
├── identifier/       # IdentifyProcessor, Identified
└── util/             # Checks, Colls, ThrowableConsumer
```
## Лицензия

[Apache License 2.0](LICENSE)
