# Reactive Programming with Spring Boot

> **Primer:** Project Reactor (`Mono` = 0..1, `Flux` = 0..N) + WebFlux lets one
> small thread pool serve thousands of concurrent requests by never blocking:
> I/O suspends the stream and resumes on callback. Backpressure propagates
> demand upstream so fast producers can't drown slow consumers. Use it for
> high-concurrency I/O-bound gateways — not for CPU-bound work.

## Real-life use case

API gateway fanning out to 5 downstream services per request at 20k RPS:
WebFlux handles it on ~50 event-loop threads where MVC would need thousands
of blocked request threads (and their 1MB stacks). CPU-bound report rendering
stays on MVC — reactive buys nothing there.

## Mental model

```text
  Request → event-loop thread (never blocked)
      ├─ WebClient.get() → suspend, thread serves other requests
      ├─ response arrives → resume pipeline (map/filter/flatMap)
      └─ backpressure: subscriber requests N → publisher emits ≤ N

  MVC:  1 request = 1 blocked thread (200 threads → 200 concurrent)
  Flux: 1 request = 0 threads while waiting (50 threads → 20k concurrent)
```

## Minimal example

```java
package com.example.gateway;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CatalogGateway {
    private final WebClient webClient = WebClient.create("http://catalog");

    public Mono<Product> product(Long id) { // 0..1 async value
        return webClient.get().uri("/products/{id}", id)
                .retrieve().bodyToMono(Product.class);
    }

    public Flux<Product> search(String q) { // 0..N stream with backpressure
        return webClient.get().uri("/products?q={q}", q)
                .retrieve().bodyToFlux(Product.class)
                .filter(p -> p.inStock())
                .take(20); // downstream demand: only first 20
    }
}
```

## Pitfalls

- **Never `.block()` on an event-loop thread** — it freezes the loop for every request sharing it; block only in tests or on `boundedElastic` schedulers.
- Blocking drivers (JDBC, old SDKs) inside a reactive chain silently pin threads — wrap with `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` or go R2DBC.
- Debugging stack traces are assembly-time, not call-site — add `Hooks.onOperatorDebug()` or `checkpoint("after-catalog")` in dev.

## Interview Q&A

**Q1. When is WebFlux the wrong choice?**
A: CPU-bound work (no I/O to overlap — event loop becomes the bottleneck),
teams without reactive debugging experience, or blocking-only dependencies
(JDBC everywhere). MVC + virtual threads covers most CRUD apps with far simpler
code.

**Q2. What is backpressure and what breaks without it?**
A: Backpressure is the subscriber telling the publisher how many items it can
handle (`request(n)`). Without it a fast Kafka/DB publisher buffers unboundedly
into a slow consumer → OOM. `Flux` operators (`take`, `buffer`, `onBackpressureDrop`)
enforce the contract.

---



## Udemy

- [Reactive Programming with Spring Framework 5](https://www.udemy.com/course/reactive-programming-with-spring-framework-5/)
- [Build Reactive MicroServices using Spring WebFlux/SpringBoot](https://www.udemy.com/course/build-reactive-restful-apis-using-spring-boot-webflux)
- [Spring WebFlux Masterclass: Reactive Microservices](https://www.udemy.com/course/spring-webflux)



## Youtube

- [Daily Code Buffer](https://www.youtube.com/@DailyCodeBuffer)
    - [Reactive Programming in JAVA | Project Reactor Full Tutorial](https://www.youtube.com/watch?v=O26jhgk682Q)
- [Java Techie](https://www.youtube.com/@Javatechie)
    - [Spring Boot - Reactive Programming | JavaTechie](https://www.youtube.com/playlist?list=PLVz2XdJiJQxyB4Sy29sAnU3Eqz0pvGCkD)
