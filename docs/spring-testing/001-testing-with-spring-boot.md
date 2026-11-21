# Testing a Spring Boot Application

> **Primer:** Spring Boot testing is a layered pyramid — fast unit tests for
> business logic (JUnit 5 + Mockito + AssertJ), slice tests for one layer
> (`@WebMvcTest`, `@DataJpaTest`), and full `@SpringBootTest` integration tests
> with real infrastructure via Testcontainers. The test slices reuse your real
> config but only start the beans you need, so suites stay fast.

## Real-life use case

Order service with 400 tests finishing in 90 seconds: pure unit tests for
pricing math, `@WebMvcTest` for controller validation rules, `@DataJpaTest`
against a Postgres Testcontainer for repository queries, and five
`@SpringBootTest` happy-path flows. A broken JPQL query fails the suite, not
production.

## Mental model

```text
  Pyramid (bottom = most tests, fastest):
    Unit ......... new OrderService(repo) + Mockito stub → no Spring at all
    Slice ........ @WebMvcTest (web only, MockMvc) / @DataJpaTest (JPA + Testcontainers DB)
    Integration .. @SpringBootTest (full context) + Testcontainers (Postgres/Kafka)
    Contract ..... spring-cloud-contract stubs (see spring-cloud/012-testing.md)

  @DynamicPropertySource → points DataSource/Kafka at Testcontainer ports
```

## Minimal example

```java
package com.example.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class) // starts web layer only
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService orders;

    @Test
    void getOrderReturnsDto() throws Exception {
        when(orders.find(1L)).thenReturn(new OrderDto(1L, "PAID"));
        mvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
```

```java
package com.example.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.ServiceConnection;

@DataJpaTest // JPA slice: real Hibernate, no web layer
class OrderRepositoryTest {

    @Container
    @ServiceConnection // wires container URL into spring.datasource.* automatically
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private OrderRepository orders;

    @Test
    void findsByStatus() {
        orders.save(new Order("PAID"));
        assert orders.findByStatus("PAID").size() == 1;
    }
}
```

## Pitfalls

- `@SpringBootTest` for everything makes suites take 20+ minutes — push logic down to unit/slice tests.
- `@MockBean` in a `@SpringBootTest` silently replaces the bean for the whole context — overuse hides wiring bugs that only full-context smoke tests catch.
- Forgetting `@ServiceConnection`/`@DynamicPropertySource` points tests at the dev DB — tests pass locally and corrupt shared data.

## Interview Q&A

**Q1. `@WebMvcTest` vs `@SpringBootTest` — when is each right?**
A: `@WebMvcTest` for controller behavior (validation, status codes, security
rules) with services mocked — fast, no DB. `@SpringBootTest` for end-to-end
flows where real wiring matters (transactions, serialization, security
filters). Most tests should be the former.

**Q2. Why Testcontainers instead of H2 for repository tests?**
A: H2 doesn't speak Postgres/MySQL dialect (JSONB, `ON CONFLICT`, sequences
behave differently), so queries that pass on H2 fail in prod. A Postgres
Testcontainer runs the real engine in Docker — same SQL, same semantics.

---

## Resources

- [Daily Code Buffer — Spring Boot 3 Integration Testing With TestContainers](https://www.youtube.com/watch?v=41Ry2ojnJFM)
- [Spring Cloud Contract testing](./../spring-cloud/012-testing.md)
