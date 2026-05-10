### What is @EnableTransactionManagement? Is it Compulsory?

`@EnableTransactionManagement` tells Spring to **enable annotation-driven transaction management** — it activates the infrastructure that detects `@Transactional` annotations and creates AOP proxies.

```java
// Explicit usage:
@Configuration
@EnableTransactionManagement
public class AppConfig {
    // ...
}
```

**Is it compulsory? NO — in Spring Boot, it is NOT required.**

Spring Boot's `TransactionAutoConfiguration` (from `spring-boot-autoconfigure`) **automatically enables** `@EnableTransactionManagement` when it detects a `PlatformTransactionManager` bean on the classpath. Adding `spring-boot-starter-data-jpa` or `spring-boot-starter-data-mongodb` triggers this auto-configuration.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @EnableTransactionManagement — When to Use:                                     │
│                                                                                  │
│  ┌───────────────────────────────┬───────────────────────────────────────────┐    │
│  │ Scenario                      │ @EnableTransactionManagement Needed?      │    │
│  ├───────────────────────────────┼───────────────────────────────────────────┤    │
│  │ Spring Boot + starter-data-*  │ ❌ NO — auto-configured                   │    │
│  │ Plain Spring (no Spring Boot) │ ✅ YES — must add explicitly              │    │
│  │ Custom TransactionManager     │ ❌ NO — Spring Boot still auto-detects    │    │
│  │ Want to customize proxy mode  │ ✅ YES — to set proxyTargetClass, mode    │    │
│  └───────────────────────────────┴───────────────────────────────────────────┘    │
│                                                                                  │
│  // Customization example (rare):                                                │
│  @EnableTransactionManagement(                                                   │
│      proxyTargetClass = true,    // force CGLIB proxies instead of JDK dynamic   │
│      mode = AdviceMode.ASPECTJ   // use AspectJ weaving instead of Spring AOP    │
│  )                                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

