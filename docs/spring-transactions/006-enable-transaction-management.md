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

**Real-life use case — when this matters:** plain Spring (no Boot) fund-transfer
app where you forget `@EnableTransactionManagement` — `@Transactional` silently
does nothing and a mid-transfer crash leaves money debited but never credited.
In Spring Boot the same app works with zero config via auto-configuration.

**Minimal runnable Spring Boot app (no `@EnableTransactionManagement` needed):**

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

@SpringBootApplication // includes TransactionAutoConfiguration
public class TxDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TxDemoApplication.class, args);
    }
}

interface AccountRepository extends JpaRepository<Account, Long> {}

@Service
class TransferService {
    @Autowired private AccountRepository accounts;

    @Transactional // picked up automatically — proxy created without @Enable*
    public void transfer(Long fromId, Long toId, java.math.BigDecimal amt) {
        Account from = accounts.findById(fromId).orElseThrow();
        Account to = accounts.findById(toId).orElseThrow();
        from.setBalance(from.getBalance().subtract(amt));
        to.setBalance(to.getBalance().add(amt));
        accounts.save(from);
        accounts.save(to);
    }
}
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:txdemo
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop
```

**SQL log — single atomic unit (`show-sql=true`):**

```sql
BEGIN;
Hibernate: update accounts set balance=? where id=? -- [800, 1]
Hibernate: update accounts set balance=? where id=? -- [700, 2]
COMMIT;
-- Exception before COMMIT -> ROLLBACK (both updates undone)
```

**Caveat — multiple TransactionManagers (e.g. JPA + Mongo, primary + replica):**
auto-configuration backs off when it finds more than one
`PlatformTransactionManager` bean. Exactly one must be `@Primary`, and every
`@Transactional` that should NOT use it must name its manager explicitly —
otherwise startup fails with `NoUniqueBeanDefinitionException` or, worse, the
wrong manager commits the transaction.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;

@Configuration
public class TxConfig {
    @Bean @Primary // default for plain @Transactional
    JpaTransactionManager primaryTxManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean("replicaTxManager")
    JpaTransactionManager replicaTxManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}

// Usage:
@Transactional // -> primaryTxManager
public void write() { /* ... */ }

@Transactional("replicaTxManager") // or transactionManager = "replicaTxManager"
public void readFromReplica() { /* ... */ }
```

**Pitfalls:**
- Adding `@EnableTransactionManagement` in Boot just to "be safe" and
  accidentally switching `proxyTargetClass`/`mode` — self-invocation still
  bypasses the proxy; see `008`.
- Two managers without `@Primary`/`@Qualifier` — ambiguous injection at startup.
- Assuming `@Transactional("wrongName")` fails fast — a typo falls back
  confusingly; verify the bean name matches.

**Interview Q&A:**
- Q: Is `@EnableTransactionManagement` compulsory in Spring Boot?
  A: No. `TransactionAutoConfiguration` enables it when a
  `PlatformTransactionManager` is present (e.g. via `starter-data-jpa`).
  It is compulsory only in plain Spring or when customizing proxy mode.
- Q: You have two `PlatformTransactionManager` beans — how does Spring pick one?
  A: Mark one `@Primary` as the default; use
  `@Transactional("otherTxManager")` / `@Qualifier` for the rest, otherwise
  startup throws `NoUniqueBeanDefinitionException`.
- Q: Does `@EnableTransactionManagement` fix self-invocation (`this.foo()`)?
  A: No — the call never goes through the proxy regardless of how management
  was enabled. Extract the method to another bean or use `exposeProxy = true`.

**Next:**
- [007 — Method vs Class-level `@Transactional`](007-method-vs-class-level-transactional.md)
- [008 — `@Transactional` internals (Spring AOP)](008-transactional-internals-spring-aop.md)

---

