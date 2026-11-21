## Spring Cloud Centralized Configuration



## Spring Config Management

- What is profiling meaning in spring boot? 
- what is application-{profile}.properties or application-{profile}.yaml?
- How and Why should we use it? 
- Explain with code example how to set the profile in spring boot in `spring.profiles.active` or using `-Dspring-boot.run.profiles` or using `pom.xml and mvn spring-boot:run -Pdev`? Which one has more priority?
- How the values are set in the application if we have the same key(`migration.enabled`) specified in different places like @Value, default profile and dev/prod profile
- Advantage disadvantage and limitation of this
- Can we use multiple profiles in comma separated way in `spring.profiles.active`? which values will be picked ultimately then?

### What Is Profiling in Spring Boot?

Profiling means running the same deployable artifact with a different named configuration set per environment (`dev`, `test`, `prod`) without rebuilding the code. Spring activates only the beans and property files that belong to the active profile(s).

Common uses: different DB URL per environment, enabling H2 console only in `dev`, enabling caching/metrics only in `prod`, turning a migration flag on/off per environment.

```text
                    +-------------------+
                    |   Same JAR/WAR    |
                    +--------+----------+
                             |
        +--------------------+--------------------+
        |                    |                    |
 +------v------+      +------v------+      +------v------+
 | profile=dev |      |profile=test |      |profile=prod |
 | H2, debug,  |      | Testcontainers,   | MySQL, info,|
 | ddl=create  |      | ddl=validate|      | ddl=none    |
 +-------------+      +-------------+      +-------------+
 application-dev.* active, others ignored
```

### What Is `application-{profile}.properties` / `application-{profile}.yaml`?

Spring Boot always loads `application.properties` (or `application.yml`) as the default. It then additionally loads `application-{profile}.*` for every active profile and lets profile-specific keys override the default file.

Rules:

- File naming is literal: `application-dev.properties`, `application-prod.yaml`. Do not mix `.properties` and `.yaml` for the same profile unless you understand ordering.
- Only the active profile's file is loaded. If `spring.profiles.active=dev`, `application-prod.properties` is ignored.
- YAML variant groups by profile in one file with `spring.config.activate.on-profile`, or split into `application-dev.yaml` / `application-prod.yaml` files — split files are simpler to review.
- Profile files contain only the deltas (overrides), not a full copy of the default file.

### How and Why to Use It

Why: one build promotes through all environments, secrets/URLs stay out of code, and environment drift is visible as a small diff instead of scattered `if (env == ...)` branches. Use `@Profile("dev")` on dev-only beans (seed data, mock clients) and `@Profile("!test")` to exclude beans from a profile.

How to set the active profile (all three do the same thing — they set `spring.profiles.active`):

1. `application.properties` / `application.yaml`: `spring.profiles.active=dev` — checked in, lowest priority, good as a local default.
2. Command line / JVM: `java -jar app.jar --spring.profiles.active=prod` or `-Dspring.profiles.active=prod`, Maven: `mvn spring-boot:run -Dspring-boot.run.profiles=dev` — best for CI/CD, no code change.
3. Maven profile in `pom.xml` mapping to `-Pdev` + `mvn spring-boot:run -Pdev` — useful when a Maven profile also switches dependencies/plugins, but it is a Maven concept layered on top of Spring profiles.

Priority order (highest wins): command-line args / `SPRING_PROFILES_ACTIVE` env var > `-D` JVM system properties > `spring.profiles.active` inside `application.properties` > `spring.profiles.default` fallback > no profile (default file only). Never hard-code `spring.profiles.active=prod` in the committed default file — the deploy environment must decide.

### Same-Key Resolution: `@Value`, Default vs `dev`/`prod`

If `migration.enabled` exists in multiple places, Spring resolves it through the `Environment` property-source chain, not through `@Value` itself. `@Value("${migration.enabled}")` just reads the already-resolved value.

Resolution order for the same key: active profile file (`application-dev.*`) > default `application.*` > environment variables > command line (command line actually beats everything). So with `spring.profiles.active=dev`, `application-dev.properties` value wins over `application.properties`.

### Multiple Comma-Separated Profiles

Yes: `spring.profiles.active=dev,postgres,metrics`. All listed profiles become active; their files are all loaded. If the same key appears in more than one active profile file, the last-listed profile wins (`metrics` beats `postgres` beats `dev`). Beans annotated with any of the active names are created. Use this to compose orthogonal concerns (`dev` + `postgres`) instead of exploding combinations (`dev-postgres`, `dev-mysql`).

### Advantages, Disadvantages, Limitations

Advantages: single artifact for all environments, clear per-environment diffs, type-safe binding via `@ConfigurationProperties`, externalized secrets via env vars/vault, easy local override without editing tracked files.

Disadvantages: property-source ordering is subtle and misordering causes "works on my machine" bugs, duplicated keys across many files drift, YAML indentation errors fail silently, `@Value` gives poor error messages compared to `@ConfigurationProperties`.

Limitations: profiles are not access control — anyone with the jar can activate `prod` config; profile files are still packaged in the jar unless moved to a Config Server/volume; changing a profile file requires restart unless Spring Cloud Config + `@RefreshScope` is used; profile names are free-form strings with no compile-time validation.

### Code Example 1 — Profile Files and Activation

```yaml
# application.yaml (default for all environments)
migration:
  enabled: false
spring:
  profiles:
    active: dev   # local default; overridden by -D / env var in higher envs

---
# application-dev.yaml (loaded only when profile=dev)
migration:
  enabled: true
logging:
  level:
    root: DEBUG

---
# application-prod.yaml (loaded only when profile=prod)
migration:
  enabled: false
logging:
  level:
    root: INFO
```

### Code Example 2 — Reading the Resolved Value and Profile-Scoped Beans

```java
package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
public class MigrationRunner {

    // Resolves to true when profile=dev (application-dev.yaml wins),
    // false otherwise (default application.yaml wins).
    @Value("${migration.enabled:false}")
    private boolean migrationEnabled;

    public boolean isMigrationEnabled() {
        return migrationEnabled;
    }
}

@Component
@Profile("dev") // created only when 'dev' is active
class DevDataSeeder {
}

@Component
@Profile("!test") // created in every profile except 'test'
class ExternalApiClient {
}
```

Activate it:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
java -jar app.jar --spring.profiles.active=prod
SPRING_PROFILES_ACTIVE=prod java -jar app.jar
```

Maven `-Pdev` variant (Maven profile forwards to the Spring profile):

```xml
<profiles>
    <profile>
        <id>dev</id>
        <properties>
            <spring-boot.run.profiles>dev</spring-boot.run.profiles>
        </properties>
    </profile>
</profiles>
<!-- mvn spring-boot:run -Pdev -->
```

### Interview Q&A

**Q1. If `migration.enabled=false` in `application.properties` but `true` in `application-dev.properties` and `spring.profiles.active=dev`, what does `@Value("${migration.enabled}")` inject? Why?**
A: `true`. Spring loads the default file first, then overlays the active profile file. The profile-specific `PropertySource` has higher precedence, so it overrides the same key from the default file.

**Q2. You set `spring.profiles.active=dev` in `application.properties` but launch with `-Dspring.profiles.active=prod`. Which profile is active?**
A: `prod`. JVM system properties / command-line arguments outrank values inside `application.properties`. The committed file is only a default; the launch environment always wins.

**Q3. You set `spring.profiles.active=dev,prod` and both files define `migration.enabled` differently. Which value wins?**
A: The last-listed profile wins — `prod` in this case. Spring loads profile files in listed order and each later file overrides earlier ones for the same key. Avoid putting conflicting values in composable profiles; keep orthogonal keys in each.

### Blogs and websites

- [Spring Cloud Config – Externalized Configuration with Git Backend](https://www.baeldung.com/spring-cloud-configuration)


### Medium

- [A Beginner’s Guide to Centralized Configuration with Spring Cloud Config](https://medium.com/@AlexanderObregon/a-beginners-guide-to-centralized-configuration-with-spring-cloud-config-6dfb6c70b5ad)
- [Microservices — Centralized Configuration with Spring Cloud](https://medium.com/javarevisited/microservices-centralized-configuration-with-spring-cloud-f9e41b511a56)
- [Spring Cloud Config for Centralized Microservice configuration](https://medium.com/@anupriyakumawat95/spring-cloud-config-for-centralised-microservice-configuration-bde36379cc4e)
- [Reducing Configuration Overload: Centralized Config Server and Automation in Spring Boot](https://blog.devgenius.io/reducing-configuration-overload-centralized-config-server-and-automation-in-spring-boot-8b898bcf52f0)
- [Microservices — Centralized Configuration with Spring Cloud](https://medium.com/@ijayakantha/microservices-centralized-configuration-with-spring-cloud-f2a1f7b78cc2)
- [Centralized Configuration for Microservices using Spring Cloud Config](https://medium.com/javarevisited/centralized-configuration-for-microservices-using-spring-cloud-config-c7845cbccc34)
- [Centralized Configuration Management with Spring Cloud Config Server](https://blog.devops.dev/centralized-configuration-management-with-spring-cloud-config-server-6613414cbb05)
- [Centralized Configuration in Microservices with Spring Cloud Config Server](https://codeburst.io/spring-cloud-config-centralized-configuration-in-microservices-f4ec243512db)



### Youtube

- [Spring Cloud Config Server & Client: Centralized Configuration Management for Microservices 3.0](https://www.youtube.com/watch?v=gZSqmmdmIf4)
- [Spring Cloud Config Explained | Centralized Config for Microservices (with Git + Refresh Demo)](https://www.youtube.com/watch?v=FNM6JwycIXI)
- [Complete Configurations Management 🔥 Externalising Configurations like Pro | Spring Boot | Hindi](https://www.youtube.com/watch?v=oP_iXYW79fc)
- [Spring Boot: ConfigurationProperties in-depth](https://www.youtube.com/watch?v=8IoR4AGf8xc&list=PL6W8uoQQ2c60g6_fcjDCLHSx1LBeVYqyZ&index=44)


- [Microservices Tutorial in Hindi 2023](https://www.youtube.com/playlist?list=PL0zysOflRCelb2Y4WOVckFC6B050BzV0D)
    - [API Gateway configuring multiple url of microservice | Microservices tutorial series in hindi](https://www.youtube.com/watch?v=bSAtR6oYJsc&list=PL0zysOflRCelb2Y4WOVckFC6B050BzV0D&index=20)
    - [Config Server in Microservices | Microservices Tutorial Series in Hindi](https://www.youtube.com/watch?v=VNYWCl7Z-_c&list=PL0zysOflRCelb2Y4WOVckFC6B050BzV0D&index=22)
    - [Implementing Config Server in Microservices | Microservices Tutorial Series in Hindi](https://www.youtube.com/watch?v=kaNHT6_MV-Q&list=PL0zysOflRCelb2Y4WOVckFC6B050BzV0D&index=23)
    - [Reading config from github | Implementing config client microservices | Microservices Tutorial](https://www.youtube.com/watch?v=q1D1TNfsdhA&list=PL0zysOflRCelb2Y4WOVckFC6B050BzV0D&index=24)


- [Master Spring Boot Configuration Management: Profiles, Cloud Config & Secure Secrets | Microservices](https://www.youtube.com/watch?v=OIXtqbNJ01I)

- [Spring Boot Microservices Level 3: Microservice configuration](https://www.youtube.com/playlist?list=PLqq-6Pq4lTTaoaVoQVfRJPqvNTCjcTvJB)











