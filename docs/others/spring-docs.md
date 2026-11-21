# Documentation in Spring Boot

> **Primer:** `springdoc-openapi` generates an OpenAPI 3 spec from your
> controllers at runtime (`/v3/api-docs`) and serves Swagger UI (`/swagger-ui.html`).
> You write normal Spring MVC code; annotations like `@Operation`/`@Schema` only
> polish the descriptions. The spec doubles as a contract for clients and tests.

## Real-life use case

Public payments API with 40 endpoints: frontend, mobile, and partner teams all
codegen their clients from `/v3/api-docs.yaml` instead of hand-writing HTTP
calls — endpoint drift becomes a compile error, not a production incident.

## Mental model

```text
  @RestController + @Operation/@Schema
        │  (springdoc scans at startup)
        v
  /v3/api-docs (JSON) ──→ /v3/api-docs.yaml ──→ client codegen
        │
        └──→ /swagger-ui.html (try-it-now console)
```

## Minimal example

```java
package com.example.orders;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @Operation(summary = "Fetch an order by id", tags = "orders")
    @GetMapping("/orders/{id}")
    public OrderDto get(@PathVariable Long id) {
        return orderService.find(id);
    }
}

@Schema(description = "Public order view")
public record OrderDto(
        @Schema(description = "Order id", example = "123") Long id,
        @Schema(description = "Order status", example = "PAID") String status) {}
```

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

## Pitfalls

- Unexposed internal endpoints (actuator, admin) leak into the spec — restrict with `springdoc.packages-to-scan` or path filters.
- `@Schema(hidden = true)` on password/token fields, or they appear in examples.
- Version the spec artifact in CI (fail the build on breaking diffs with `openapi-diff`).

## Interview Q&A

**Q1. springdoc vs Spring REST Docs — which and when?**
A: springdoc is generated from code (always in sync, less control over prose).
REST Docs is generated from *tests* (spec stays accurate because failing tests
fail the docs; more setup). Teams wanting contract guarantees pick REST Docs;
teams wanting zero-effort UI pick springdoc.

**Q2. How do you keep the spec from drifting from the implementation?**
A: Generate the YAML in CI and either publish it as a versioned artifact or
assert no diff against the checked-in spec. Treat spec changes like API
changes: reviewed, versioned, semver'd.

---

## Youtube

- [SpringDoc - An Open API 3.0 based Documentation Library in Spring Boot](https://www.youtube.com/watch?v=iaVBleTf88U)