# Spring GraphQL course

> **Primer:** Spring for GraphQL exposes a typed schema (`schema.graphqls`) instead
> of REST endpoints: clients query exactly the fields they need in one request,
> and `@QueryMapping`/`@MutationMapping`/`@BatchMapping` methods resolve them.
> It sits on WebMVC or WebFlux and reuses your services, validation, and security.

## Real-life use case

Mobile product page needing product + reviews + seller in one round trip: one
GraphQL query fetches all three (with `@BatchMapping` collapsing the N+1 into
batched loads), where REST would need 3 sequential calls or an over-fetching
BFF endpoint.

## Mental model

```text
  schema.graphqls:  type Query { product(id: ID!): Product }
                    type Product { id, name, reviews: [Review] }

  Client query → QueryMapping.product(id) → Product
                                              └─ @BatchMapping reviews(List<Product>)
                                                 → ONE batched load (no N+1)

  Mutations: @MutationMapping createOrder(input) → payload | validation errors
```

## Minimal example

```graphql
# src/main/resources/graphql/schema.graphqls
type Query { product(id: ID!): Product }
type Product { id: ID!, name: String!, reviews: [Review!]! }
type Review { id: ID!, rating: Int!, text: String }
```

```java
package com.example.catalog;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.List;
import java.util.Map;

@Controller
public class ProductController {

    @QueryMapping
    public Product product(@Argument Long id) {
        return productService.find(id);
    }

    @BatchMapping(typeName = "Product", field = "reviews") // fixes N+1
    public Map<Product, List<Review>> reviews(List<Product> products) {
        return reviewService.findByProducts(products);
    }
}
```

## Pitfalls

- Without `@BatchMapping`/`DataLoader`, nested lists cause N+1 (one query per parent).
- Deep/recursive queries need `max-query-depth` + complexity limits or one client can DoS the DB.
- Versioning is additive (deprecate fields with `@deprecated`) — no URL versioning.

## Interview Q&A

**Q1. GraphQL vs REST — when does GraphQL win?**
A: Clients with divergent field needs (mobile vs web), aggregating several
resources in one round trip, or rapidly evolving UIs. REST wins for simple
CRUD, HTTP caching, and file uploads.

**Q2. How do you avoid N+1 in GraphQL resolvers?**
A: `@BatchMapping` (or `DataLoader`) collects all parents in the selection set
and loads children in one batched query, returning a `Map<Parent, List<Child>>`
— the same fix as JPA `JOIN FETCH`, at the resolver layer.

---


## Udemy

- [GraphQL API with Java Spring Boot & Spring Framework](https://www.udemy.com/course/graphql-with-java-spring-boot-query-mutation-schema-resolver-edge-jpa/)



## Youtube

- [Spring Boot and GraphQL Tutorial](https://www.youtube.com/watch?v=uNB2N_w_ypo)
- [Master Graphql with Spring Boot in one video in Hindi](https://www.youtube.com/watch?v=K_MGBQ_1Nic)
- [Master Graphql with Spring Boot](https://www.youtube.com/watch?v=eD-1KTK7fGc)
- [Mastering GraphQL & Spring Boot APIs | Real-world Example](https://www.youtube.com/watch?v=GJc36OKFQ_M)
- [GraphQL](https://www.youtube.com/playlist?list=PLZV0a2jwt22slmUC9iwGGWfRQRIhs1ELa)