# handletility

Java-библиотека для построения **обработчиков** (`Handler`) и **преобразователей** (`Processor`) с единообразной моделью результата: `DONE` / `SKIPPED` / `ERROR`.

Подходит для пайплайнов команд, маршрутизации по ключу, цепочек side-effect’ов и трансформаций без разрастания `if/else` и try/catch.

## Требования

- Java 17+
- Gradle 8.8 (wrapper в репозитории)

## Подключение

Локальная публикация в Maven Local:

```bash
./gradlew publishToMavenLocal
```

```gradle
dependencies {
    implementation 'ru.operation:handletility:0.0.1'
}
```

Публикация в удалённый репозиторий настраивается через `maven-publish` в `build.gradle`.

## Идея

| Абстракция | Назначение | Результат |
|---|---|---|
| `Handler<I>` | side-effect над входом (лог, сохранение, отправка) | `Handled` |
| `Processor<I, O>` | преобразование `I → O` | `Processed<O>` |

Оба результата несут `ResultState` и опциональный `Exception`. Базовые классы `AbstractHandler` / `AbstractProcessor` добавляют:

- precondition через `check(input)`
- перехват исключений → `ERROR`
- логирование (SLF4J, `compileOnly`)
- именование через `setName` / `AbstractOperation.withName(...)`

## Быстрый старт

### Handler

```java
Handler<String> print =
        new SimpleHandler<>(s -> !s.isBlank(), System.out::println);

Handled result = print.handle("hello");
// DONE → Handled.handled()
// не подходит check → SKIPPED
// исключение → ERROR
```

### Processor

```java
Processor<String, Integer> parse =
        new SimpleProcessor<>(s -> s.matches("\\d+"), Integer::valueOf);

Processed<Integer> out = parse.process("42");
Integer value = out.orThrow(); // 42
```

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

Fan-out method-reference на список объектов:

```java
Handler<Event> fanOut = HandlerList.fromMethodRefs(listeners, Listener::onEvent);
```

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
processed
        .process(nextProcessor)
        .handle(sideEffectHandler);
```

### Async

```java
Handler<Event> async = new AsyncHandler<>(executor, syncHandler);
// handle() сразу возвращает DONE; ошибка в фоне уходит в onException
```

### MDC

`MdcHandler` / `MdcProcessor` ставят значение в MDC на время вызова делегата и восстанавливают предыдущее значение после:

```java
Handler<Request> traced = new MdcHandler<>(
        "requestId",
        new SimpleProcessor<>(Request::id),
        delegate);
```

## Структура пакета

```
ru.operation
├── handler/          # Handler, Handled, AbstractHandler, Async…
│   └── impl/
│       ├── list/     # HandlerList (+ fromMethodRefs), modifiers
│       └── map/      # HandlerMap
├── processor/        # Processor, Processed, AbstractProcessor
│   └── impl/
│       ├── list/     # ProcessorList
│       └── map/      # ProcessorMap
├── identifier/       # IdentifyProcessor, Identified
└── util/function/    # ThrowableConsumer
```

## Сборка и тесты

```bash
./gradlew test
./gradlew spotlessApply   # Google Java Format (AOSP)
./gradlew build
```

## Лицензия

Не указана — добавьте при публикации (см. улучшения ниже).
