### Prerequisites

Before diving into Spring Transactions, you need to understand two foundational concepts:

1. **Concurrency Control** — How multiple threads/users accessing the same data simultaneously is managed
2. **Spring AOP** — How Spring wraps your beans with proxies to add cross-cutting behavior (see [Spring AOP](spring-aop.md))

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Why These Prerequisites Matter:                                                 │
│                                                                                  │
│  Concurrency Control                    Spring AOP                               │
│  ┌─────────────────────┐               ┌──────────────────────┐                  │
│  │ Multiple users/      │               │ @Transactional works │                  │
│  │ threads accessing     │               │ via AOP Proxy that   │                  │
│  │ same bank account     │               │ wraps your service   │                  │
│  │ simultaneously        │               │ bean with transaction │                  │
│  │                       │               │ begin/commit/rollback│                  │
│  │ → Dirty Reads         │               │                      │                  │
│  │ → Lost Updates        │               │ → @Around advice     │                  │
│  │ → Phantom Reads       │               │ → Proxy pattern      │                  │
│  └─────────┬─────────────┘               └──────────┬───────────┘                  │
│            │                                        │                             │
│            └──────────────┬─────────────────────────┘                             │
│                           │                                                       │
│                           v                                                       │
│              ┌──────────────────────────┐                                         │
│              │  Spring @Transactional    │                                         │
│              │  Solves both:             │                                         │
│              │  1. Data consistency      │                                         │
│              │  2. Isolation between     │                                         │
│              │     concurrent access     │                                         │
│              └──────────────────────────┘                                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

