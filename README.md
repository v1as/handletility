# handletility

`Handler<I>` выполняет действие над входом: списывает платёж, сохраняет заказ, отправляет письмо. Назад приходит исход этого действия — `Handled`.

`Processor<I, O>` по входу считает значение: число из строки, сумму из заказа. Назад приходит `Processed<O>`: исход и, когда расчёт удался, само значение.

Исход и у действия, и у расчёта — одно из трёх состояний.

**Done** — шаг завершился. У процессора есть payload, и этот payload может быть `null`.

**Skipped** — шаг к этому входу не относится. Тот же вход может уйти другому шагу.

**Error** — шаг сломался. Исключение лежит на результате.

Java 17. Транзитивно подтягивается `slf4j-api`; реализацию логирования (Logback, Log4j2 и т.п.) подключает приложение.

```gradle
dependencies {
    implementation 'io.github.v1as:handletility:0.0.12'
}
```

```xml
<dependency>
  <groupId>io.github.v1as</groupId>
  <artifactId>handletility</artifactId>
  <version>0.0.12</version>
</dependency>
```

## Один вызов — три ответа

```java
record Payment(String method, int cents) {}

Handler<Payment> card =
        Processor.<Payment>when(payment -> "card".equals(payment.method()))
                .thenHandle(
                        Handler.of(
                                payment -> {
                                    if (payment.cents() < 0) {
                                        throw new IllegalArgumentException("negative");
                                    }
                                    gateway.charge(payment);
                                }));

card.handle(new Payment("card", 500));    // done
card.handle(new Payment("wallet", 500));  // skipped
card.handle(new Payment("card", -1));     // error, исключение лежит на Handled
```

```java
Processor<String, Integer> cents =
        Processor.<String>when(raw -> !raw.isBlank())
                .thenApply(raw -> Integer.valueOf(raw) * 100);

cents.process("42");    // done, 4200
cents.process("   ");   // skipped
cents.process("nope");  // error
```

`Handler.of` и `Processor.of` кладут брошенное исключение в error. Неподходящий вход отсекает `Processor.when`: это skip, и следующий шаг не вызывается.

## Skipped отдаёт тот же вход следующему кандидату

```java
Handler<Payment> wallet =
        Processor.<Payment>when(payment -> "wallet".equals(payment.method()))
                .thenHandle(Handler.of(payment -> wallets.draw(payment)));

Handler<Payment> charge = card.or(wallet);

charge.handle(new Payment("wallet", 500)); // card пропустил, wallet списал
charge.handle(new Payment("card", 500));   // card завершился
charge.handle(new Payment("card", -1));    // ошибка card — результат всего charge
charge.handle(new Payment("cash", 500));   // skipped: пропустили оба
```

Done и error заканчивают цепочку. Цепочка остаётся skipped, только когда пропустил каждый кандидат. Следующий кандидат вызывается после skip.

`Processor.or` устроен так же:

```java
Processor<String, Integer> decimal =
        raw ->
                raw.chars().allMatch(Character::isDigit)
                        ? Processed.done(Integer.parseInt(raw))
                        : Processed.skipped();

Processor<String, Integer> hex =
        raw ->
                raw.startsWith("0x")
                        ? Processed.done(Integer.parseInt(raw.substring(2), 16))
                        : Processed.skipped();

Processor<String, Integer> amount = decimal.or(hex);

amount.process("42");    // 42
amount.process("0x2A");  // 42
amount.process("nope");  // skipped
```

## Оба шага обязательны

```java
Handler<Payment> receipt = Handler.of(payment -> mailer.sendReceipt(payment));

Handler<Payment> chargeThenReceipt = card.and(receipt);

chargeThenReceipt.handle(new Payment("card", 500));   // card done, receipt вызван
chargeThenReceipt.handle(new Payment("wallet", 500)); // card skipped, receipt не вызван
chargeThenReceipt.handle(new Payment("card", -1));    // ошибка card, receipt не вызван
```

```java
record Order(String rawAmount, String rawCurrency) {}
record Money(int cents, String currency) {}

Processor<Order, Integer> cents =
        order ->
                order.rawAmount() == null
                        ? Processed.skipped()
                        : Processed.done(Integer.valueOf(order.rawAmount()));

Processor<Order, String> currency =
        order ->
                order.rawCurrency() == null
                        ? Processed.skipped()
                        : Processed.done(order.rawCurrency());

Processor<Order, Money> money =
        cents.and(currency, (amount, code) -> new Money(amount, code));

money.process(new Order("15", "EUR")); // done, Money(15, "EUR")
money.process(new Order(null, "EUR")); // skipped: cents пропустил, currency не вызван
money.process(new Order("15", null));  // cents done, currency skipped → skipped
```

## Сбой становится значением

```java
record Charged(String method, int cents, String failure) {}

Processor<Payment, Charged> charge =
        Processor.<Payment>when(payment -> "card".equals(payment.method()))
                .thenApply(
                        payment -> {
                            if (payment.cents() < 0) {
                                throw new IllegalArgumentException("negative");
                            }
                            gateway.charge(payment);
                            return new Charged(payment.method(), payment.cents(), null);
                        })
                .recover(
                        failure ->
                                new Charged(
                                        failure.input().method(),
                                        failure.input().cents(),
                                        failure.exception().getMessage()));

charge.process(new Payment("card", 500));   // done, failure null
charge.process(new Payment("wallet", 500)); // skipped
charge.process(new Payment("card", -1));    // done, failure "negative"
```

## Дальше проходит только done

```java
Processor<String, Integer> positive =
        Processor.of(Integer::parseInt).thenFilter(n -> n > 0);

Processor<String, String> printed = positive.thenApply(n -> Integer.toString(n * 2));

printed.process("21");    // done, "42"
printed.process("0");     // skipped: thenFilter закрыл цепочку
printed.process("nope");  // error: предикат thenFilter не вызывается

Handler<String> save = printed.thenHandle(Handler.of(repository::save));
save.handle("0");         // skipped
```

`map`, `flatMap`, `filter`, `thenApply`, `thenPipe`, `thenFilter` и `thenHandle` проносят skip и error дальше и не вызывают следующий шаг.

Done с `null` вперёд идёт. Skip — нет:

```java
Processor<Order, String> code = order -> Processed.done(order.couponCode());
Processor<Order, Integer> discount =
        code.thenApply(coupon -> coupon == null ? 0 : rates.get(coupon));
// couponCode() == null → done, шаг со ставкой выполняется и возвращает 0

Processor<Order, String> presentCode =
        order ->
                order.couponCode() == null
                        ? Processed.skipped()
                        : Processed.done(order.couponCode());
Processor<Order, Integer> discountIfPresent = presentCode.thenApply(rates::get);
// couponCode() == null → skipped, rates.get не вызывается
```

Ответ действия можно продолжить значением:

```java
Processed<Receipt> receipt =
        Handler.of(orders::save)
                .handle(order)
                .produce(() -> receipts.issue(order));
// save бросил → error, issue не вызывается
```

## Несколько обработчиков — одна политика

По умолчанию список останавливается на первом done и на первом error. Skip переходит к следующему.

```java
Handler<Event> dispatch =
        HandlerList.<Event>builder()
                .handler(security)
                .handler(notify)
                .handler(index)
                .build();
```

`CONTINUE_ON_DONE` не даёт done остановить список: это рассылка действий. `CONTINUE_ON_ERROR` не даёт error остановить список. С обоими выполняется каждый обработчик. Если среди ответов был сбой, итогом становится error: одно исключение возвращается как есть, несколько собираются в одно.

```java
Handler<Event> fanout =
        HandlerList.<Event>builder()
                .handler(audit)
                .handler(notify)
                .handler(index)
                .modifier(HandlerModifier.CONTINUE_ON_DONE)
                .modifier(HandlerModifier.CONTINUE_ON_ERROR)
                .build();
```

`ProcessorList` по умолчанию — это `or` для набора: результатом становится первое done-значение, skip пробует следующий процессор, error останавливает список, пока не задан `CONTINUE_ON_ERROR`. `combiner` проходит все процессоры и сливает payload всех done слева направо; skip в склейку не входит, один done возвращается как есть. `CONTINUE_ON_ERROR` и в свёртке только про ошибку: обход продолжается, но если ошибка была, итогом становится error без слитого значения.

```java
Processor<User, List<String>> validate =
        ProcessorList.<User, List<String>>processorList()
                .processor(this::checkName)
                .processor(this::checkAge)
                .combiner(
                        (left, right) -> {
                            List<String> all = new ArrayList<>(left);
                            all.addAll(right);
                            return all;
                        })
                .build();
```

## Идентификатор выбирает операцию

```java
record Command(String name) {}

Handler<Command> commands =
        RoutingHandler.<String, Command>routingHandler()
                .handler(Handler.of(this::create).identifiedBy("create"))
                .handler(Handler.of(this::cancel).identifiedBy("cancel"))
                .identifier(
                        command ->
                                command.name() == null
                                        ? Processed.error("no name")
                                        : Processed.done(command.name()))
                .defaultHandler(command -> Handled.skipped())
                .build();

commands.handle(new Command("create")); // create
commands.handle(new Command("rename")); // defaultHandler → skipped
commands.handle(new Command(null));     // error идентификатора
```

На неизвестное имя вызывается `defaultHandler`: в примере он отвечает skipped. Если чтение идентификатора закончилось error, этот error становится результатом вызова, а `defaultHandler` не вызывается.

`RoutingProcessor` устроен так же и выбирает `IdentifiedProcessor`. Идентификатор задаётся через `Processor.identifiedBy`.

## Обвязка остаётся снаружи функции

```java
record Order(String id, String rawAmount) {}

Processor<Order, Integer> readAmount =
        ProcessorBuilder.from((Order order) -> Integer.valueOf(order.rawAmount()))
                .named("amount")
                .logging()
                .mdc("orderId", Order::id)
                .when(order -> order.rawAmount() != null)
                .before(order -> metrics.mark("amount.start"))
                .after(result -> metrics.mark(result.state().name()))
                .onException(failure -> Processed.done(0))
                .build();

readAmount.process(new Order("a1", null));   // skipped: when остановил вызов
readAmount.process(new Order("a1", "15"));   // done, 15
readAmount.process(new Order("a1", "nope")); // done, 0 — восстановил onException
```

Статический `Processor.when` проверяет вход в начале цепочки и при успехе передаёт дальше тот же вход. Метод экземпляра `thenFilter` проверяет уже успешный выход. `when` в `ProcessorBuilder` стоит снаружи before, after и ядра: ложный предикат пропускает их все, а checked-исключение предиката доходит до `onException`.

`onException` получает исключение функции и может вернуть done. Когда функция бросает, `after` не выполняется.

`before` и `after` — тоже `Handler`. Их done и skipped оставляют результат ядра на месте. Их error становится результатом операции, и сломанный `before` ядро не запускает.

`named` добавляет суффикс к имени логгера, `logging` пишет исход вызова, `mdc` на время вызова кладёт id заказа в MDC.

У `Processor.of` брошенное исключение уже записано как error, поэтому восстановление живёт на билдере.

## На границе три состояния ещё различимы

```java
Processed<Integer> result = readAmount.process(order);

result.isDone();     // payload — result.value(), в том числе null
result.hasValue();   // true только у непустого payload
result.isSkipped();
result.isError();    // result.exception()

Integer cents = readAmount.process(order).requireDone();
```

`requireDone()` возвращает payload done, включая `null`. Error бросается: runtime — как есть, checked оборачивается в `ProcessorException`. Skip бросает `ProcessorException("skipped")` без cause: значения нет.

`throwIfError()` бросает эту ошибку и возвращает тот же `Processed` и для done, и для skipped.

```java
Handled handled = charge.handle(payment);
handled.throwIfError(); // при done и skipped возвращается тот же Handled
handled.requireDone();  // при done возвращается; skip и error бросают
```

## Лицензия

[Apache License 2.0](LICENSE)
