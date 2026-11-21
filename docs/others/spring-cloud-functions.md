# Spring Cloud function

> **Primer:** Spring Cloud Function lets you write a plain `Function`/`Consumer`/
> `Supplier` bean once and deploy it anywhere: as an HTTP endpoint, a Kafka
> stream processor, or an AWS Lambda / Azure Function. The framework adapts the
> same code to each runtime — no handler-interface lock-in.

## Real-life use case

Image-thumbnail pipeline: the same `Function<byte[], byte[]>` resizes uploads.
Locally it runs behind an HTTP endpoint; in prod the identical jar is deployed
as an AWS Lambda triggered by S3 events — zero code change.

## Mental model

```text
  Your code:  @Bean Function<Order, Invoice> bill() { ... }

  Adapters (pick one per deploy):
    HTTP ......... POST /bill  → bill.apply(body)
    Stream ....... Kafka topic → bill per record (via spring-cloud-stream)
    AWS Lambda ... S3/SQS event → SpringBootRequestHandler → bill.apply(...)
```

## Minimal example

```java
package com.example.billing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Function;

@Configuration
public class BillingFunctions {

    @Bean
    public Function<Order, Invoice> bill() {
        return order -> new Invoice(order.id(), order.total().multiply(TAX));
    }
}
```

```yaml
spring:
  cloud:
    function:
      definition: bill   # which bean to expose when several exist
```

```java
// AWS Lambda entry point (adapter only — business logic stays in bill())
public class BillLambda extends SpringBootRequestHandler<Order, Invoice> {}
```

## Pitfalls

- Cold starts: keep the function jar slim (no web starter for Lambda deploys) or use SnapStart/GraalVM native.
- Functions must be stateless and idempotent — the platform may retry or run concurrent instances.
- One `definition` per deploy: exposing `bill;notify` together couples their scaling; deploy separately.

## Interview Q&A

**Q1. How does the same function run on HTTP, Kafka, and Lambda?**
A: The business logic is a plain `java.util.function` bean. Bindings
(`spring-cloud-starter-function-web`, `-stream`, `-adapter-aws`) wrap it: web
maps HTTP to `apply()`, stream wires topics, and the Lambda adapter translates
cloud events into the input type.

**Q2. Supplier vs Function vs Consumer — when is each right?**
A: `Supplier` generates data on a schedule/poll (no input), `Function`
transforms input→output (request/reply or stream map), `Consumer` is fire-and-
forget (write to DB, send email, no output).

---


## Youtube

- [Daily Code Buffer](https://www.youtube.com/@DailyCodeBuffer)
    - [Spring Cloud Functions Tutorial | Spring Boot - Serverless Functions](https://www.youtube.com/watch?v=CAMjijWhGIg&list=PLhfxuQVMs-nx3YQa3XJ9-4g_EoK0J8WhU&index=20)