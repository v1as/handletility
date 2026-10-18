# handletility

Java-библиотека для построения **обработчиков** (`Handler`) и **преобразователей** (`Processor`) с единообразной моделью результата: `DONE` / `SKIPPED` / `ERROR`.

Подходит для пайплайнов команд, маршрутизации по ключу, цепочек side-effect’ов и трансформаций без разрастания `if/else` и try/catch.

## Демо

Один сценарий — почти весь синтаксис API:

```java
record Order(String id, String userId, String payload) {}

// Function → ProcessorCompose: id, имя в логах, MDC, hooks, recover
IdentifiedProcessor<String, Order, Integer> parseAmount =
        Processors.compose((Order o) -> Integer.valueOf(o.payload()))
                .identified("parse-amount")
                .named("parseAmount")
                .logging(Level.DEBUG)
                .mdc("orderId", Order::id)
                .mdc("userId", Order::userId)
                .before(o -> metrics.mark("parse.start"))
                .after(result -> metrics.mark(result.isDone() ? "parse.ok" : "parse.fail"))
                .onException(failed -> Processed.of(0)) // recover
                .build();

// Consumer → HandlerCompose: тот же стек обёрток
IdentifiedHandler<String, Order> persist =
        Handlers.compose((Order o) -> repository.save(o))
                .identified("order-persist")
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
                .handler(Handlers.compose(o -> notify(o)).identified("notify").build())
                .modifier(HandlerModifier.CONTINUE_ON_DONE)
                .modifier(HandlerModifier.CONTINUE_ON_ERROR)
                .build();

// Маршрутизация по ключу
Handler<Order> router =
        HandlerMap.<String, Order>handlerMap()
                .handler(Handlers.compose(this::create).identified("create").build())
                .handler(Handlers.compose(this::cancel).identified("cancel").build())
                .keyExtractor(new SimpleProcessor<>(Order::payload))
                .defaultHandler(o -> Handled.skipped())
                .build();

// Processed: map / flatMap / thenHandle / orElseThrow
Handled handled =
        parseAmount
                .process(order)
                .map(amount -> amount * 100)
                .flatMap(Processors.compose(cents -> taxService.apply(cents)).identified("tax").build())
                .thenHandle(Handlers.compose((Integer c) -> charge(c)).identified("charge").build());

Integer cents = parseAmount.process(order).orElseThrow();
Optional<Integer> maybe = parseAmount.process(order).toOptional();

// Mapping входа + делегирование
Handler<String> asOrderThenPersist =
        new MappedHandler<>(new SimpleProcessor<>(json -> gson.fromJson(json, Order.class)), persist);
```

Слои compose при `build()`: `Identified(Mdc(Logging(Catching(Around(core)))))`. Исключение из core/hooks → `ERROR` (или ваш `.onException`), не throws. `Error` не ловится.

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

### Публикация в Maven Central

1. Аккаунт на [central.sonatype.com](https://central.sonatype.com) (лучше Sign in with GitHub) — обычно сразу выдаётся namespace `io.github.v1as`. Если нет — [Add Namespace](https://central.sonatype.com/publishing/namespaces) и верификация через временный репозиторий.
2. [User Token](https://central.sonatype.com/account) → Generate User Token.
3. GPG-ключ, публичный ключ на keyserver (см. [документацию Sonatype](https://central.sonatype.org/publish/requirements/gpg/)).
4. В `~/.gradle/gradle.properties` (не в репозиторий):

```properties
mavenCentralUsername=<token username>
mavenCentralPassword=<token password>
signing.keyId=<последние 8 символов key id>
signing.password=<пароль ключа>
signing.secretKeyRingFile=<путь к secring.gpg>
```

Либо in-memory ключ: `signingInMemoryKey` / `signingInMemoryKeyPassword` (см. [vanniktech docs](https://vanniktech.github.io/gradle-maven-publish-plugin/central/)).

5. Загрузка и ручной Publish в Portal:

```bash
./gradlew publishToMavenCentral
```

Затем [Deployments](https://central.sonatype.com/publishing/deployments) → Publish. До появления в Central обычно 10–30 минут.

Авторелиз без клика в UI: `./gradlew publishAndReleaseToMavenCentral`.

## Идея

| Абстракция | Назначение | Результат |
|---|---|---|
| `Handler<I>` | side-effect над входом (лог, сохранение, отправка) | `Handled` |
| `Processor<I, O>` | преобразование `I → O` | `Processed<O>` |

Оба результата несут `ResultState` и опциональный `Exception`.

**Наследование** (`AbstractHandler` / `AbstractProcessor`) — только контракт результата:

- precondition через `check(input)` → `SKIPPED`
- перехват исключений → `ERROR` (+ `warn` в лог)

**Композиция** — для имени, outcome-логирования, MDC и перехвата исключений через обёртки / `Handlers.compose` / `Processors.compose`.

`compose(Consumer)` / `compose(Function)` уже безопасны через `Simple*` → `Abstract*` (исключение → `ERROR`). Сырой `Handler`/`Processor` без `Abstract*` мог бы пробрасывать исключение — compose всегда добавляет слой `Catching`, поэтому стек возвращает `Handled`/`Processed`, а не throws.

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

Порядок слоёв при `build()` фиксирован: `Identified(Mdc(Logging(Catching(Around(core)))))` — MDC снаружи logging, чтобы в логах был контекст; Catching всегда есть и ловит `Exception` (не `Error`). Нужен `.identified(...)`; результат — `IdentifiedHandler` / `IdentifiedProcessor`.

`.named(...)` — опциональная подпись для логов; если не указан, в логгер идёт `String.valueOf(id)`.  
`.before(...)` / `.after(...)` — hooks вокруг core (внутри Catching): before получает input, after — результат (`Handled` / `Processed`), на возврат не влияет.  
`.onException(...)` — кастомный маппинг `Failed` → результат (можно recover в `ok()` / успешный `Processed`); без вызова — `LoggingException*` (warn + `ERROR`).

```java
IdentifiedHandler<String, Request> traced = Handlers.compose(delegate) // или Consumer<Request>
        .identified("order-persist")
        .named("persist")             // опционально; иначе в логах будет order-persist
        .logging()                    // DEBUG по умолчанию; или .logging(Level.TRACE)
        .mdc("requestId", Request::id)
        .mdc("userId", Request::userId)
        .before(auditBefore)          // Handler или Consumer
        .after(auditAfter)            // Handler<Handled> или Consumer<Handled>
        .onException(failed -> Handled.error(failed.exception())) // опционально
        .build();
```

Для кастомного `Processor` (SKIP/ERROR): `new MdcEntry<>("key", processor)` или `.mdc(entry)`.

Аналогично для процессоров (`Function` / `Processor` на входе, hooks — `Handler`/`Consumer`):

```java
Processors.compose(Request::payload)
        .identified("parse")
        .before(pre)
        .after(post)                  // Handler|Consumer<Processed<?>>
        .onException(failed -> Processed.error(failed.exception()))
        .logging()
        .build();
```

Обёртки вручную: `IdentifiedByConst*`, `Around*`, `Catching*`, `Named*` (только для логов), `Logging*`, `Mdc*` / `MdcEntry`.

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

### Mapping + делегирование

```java
Handler<String> asIntThenHandle = new MappedHandler<>(
        new SimpleProcessor<>(Integer::valueOf),
        new SimpleHandler<>(n -> sum.addAndGet(n)));
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

## Сборка и тесты

```bash
./gradlew test
./gradlew spotlessApply   # Google Java Format (AOSP)
./gradlew build
```

## Лицензия

[Apache License 2.0](LICENSE)
