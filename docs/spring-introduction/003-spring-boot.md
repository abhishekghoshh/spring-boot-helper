### Spring Boot

---

#### How Spring Boot is Better Than Spring MVC

Spring MVC solved the Servlet boilerplate problem, but it still required significant manual setup: two `@Configuration` classes, a `WebAppInitializer`, explicit `@Bean` declarations for `DataSource`, `EntityManagerFactory`, `TransactionManager`, Jackson configuration, an external Tomcat installation, and WAR packaging.

Spring Boot (released in 2014) was built on top of Spring MVC with one goal: **eliminate all that setup** through **Auto-Configuration** and **opinionated defaults**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring MVC (Pre-Boot) vs Spring Boot — Same App, Different Bootstrap:           │
│                                                                                  │
│  Spring MVC                           Spring Boot                                │
│  ──────────────────────────────────── ──────────────────────────────────────     │
│                                                                                  │
│  WebAppInitializer.java    ✗           @SpringBootApplication         ✓          │
│  AppConfig.java            ✗           application.properties         ✓          │
│  WebConfig.java            ✗           (auto-configured)              ✓          │
│  @EnableWebMvc             ✗           (auto-configured)              ✓          │
│  DataSource @Bean          ✗           spring.datasource.url=...      ✓          │
│  EntityManagerFactory @Bean✗           spring.jpa.hibernate.ddl=...   ✓          │
│  TransactionManager @Bean  ✗           (auto-configured)              ✓          │
│  Jackson ObjectMapper @Bean✗           (auto-configured)              ✓          │
│  packaging = war           ✗           packaging = jar                ✓          │
│  External Tomcat install   ✗           Embedded Tomcat                ✓          │
│  Copy WAR to webapps/      ✗           java -jar app.jar              ✓          │
│  pom.xml: manage 20+ deps  ✗           One spring-boot-starter-web    ✓          │
│                                                                                  │
│  Lines of config code: ~150            Lines of config code: ~10                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

The exact same `@RestController`, `@Service`, and `@Repository` code works in both. Only the bootstrap changes.

---

#### Features of Spring Boot

##### 1. Spring Initializr — Start in Seconds

Spring Initializr (`start.spring.io`) generates a ready-to-run project with selected dependencies. In IntelliJ IDEA or VS Code you can generate from the IDE directly.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Initializr — What Gets Generated:                                        │
│                                                                                  │
│  You select:                                                                     │
│    Project: Maven | Gradle                                                       │
│    Language: Java | Kotlin | Groovy                                              │
│    Spring Boot: 3.3.x                                                            │
│    Dependencies: Spring Web, Spring Data JPA, MySQL Driver, Spring Security      │
│                                                                                  │
│  You get:                                                                        │
│  myapp/                                                                          │
│  ├── pom.xml                    (or build.gradle)                                │
│  ├── src/main/java/com/example/                                                  │
│  │   └── MyappApplication.java  (@SpringBootApplication + main)                  │
│  ├── src/main/resources/                                                         │
│  │   └── application.properties (empty, ready for your config)                  │
│  └── src/test/java/com/example/                                                  │
│      └── MyappApplicationTests.java                                              │
│                                                                                  │
│  Ready to run: mvn spring-boot:run                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 2. `@SpringBootApplication` — The Entry Point

One annotation replaces three:

```java
// The entire bootstrap for a production-ready Spring Boot app:
@SpringBootApplication   // = @Configuration + @ComponentScan + @EnableAutoConfiguration
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // Starts embedded Tomcat, loads Spring context, deploys DispatcherServlet
        // Logs "Started MyApplication in 2.1 seconds"
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @SpringBootApplication = three annotations:                                     │
│                                                                                  │
│  @Configuration          → This class is a source of bean definitions           │
│  @ComponentScan          → Scan the current package and sub-packages             │
│  @EnableAutoConfiguration→ Enable Spring Boot's auto-configuration magic        │
│                                                                                  │
│  SpringApplication.run() does:                                                   │
│    1. Create ApplicationContext                                                  │
│    2. Load all auto-configuration classes (from spring.factories / META-INF)    │
│    3. Scan for @Component, @Service, @Repository, @Controller                   │
│    4. Start embedded Tomcat (or Jetty/Undertow)                                  │
│    5. Deploy DispatcherServlet                                                   │
│    6. App ready to accept requests                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 3. Auto-Configuration

Auto-configuration is the heart of Spring Boot. It inspects the **classpath** and **properties** to decide what to configure automatically.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Auto-Configuration — How It Works:                                              │
│                                                                                  │
│  Spring Boot ships with ~150 auto-configuration classes.                         │
│  They are listed in:                                                             │
│    META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration     │
│    .imports  (Spring Boot 3.x)                                                   │
│                                                                                  │
│  Each class uses @Conditional annotations to decide whether to activate:         │
│                                                                                  │
│  @Configuration                                                                  │
│  @ConditionalOnClass(DataSource.class)         // only if DataSource is on CP    │
│  @ConditionalOnMissingBean(DataSource.class)   // only if YOU haven't defined one│
│  public class DataSourceAutoConfiguration {                                      │
│      @Bean                                                                       │
│      public DataSource dataSource(DataSourceProperties props) {                  │
│          return DataSourceBuilder.create()                                       │
│              .url(props.getUrl())                                                │
│              .username(props.getUsername())                                      │
│              .password(props.getPassword())                                      │
│              .build();                                                           │
│      }                                                                           │
│  }                                                                               │
│                                                                                  │
│  Key @Conditional variants:                                                      │
│  @ConditionalOnClass        → bean activated only if class is on classpath       │
│  @ConditionalOnMissingBean  → bean activated only if user hasn't defined one     │
│  @ConditionalOnProperty     → bean activated only if property is set             │
│  @ConditionalOnWebApplication → activated only in a web context                  │
│                                                                                  │
│  Result: Add a JAR → Spring Boot auto-wires it.                                  │
│  You don't agree? Define your own @Bean and Boot backs off.                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Example — what happens when you add `spring-boot-starter-data-jpa`:**

```text
You add dependency:
    spring-boot-starter-data-jpa
    + spring.datasource.url=jdbc:mysql://localhost:3306/mydb
    + spring.datasource.username=root
    + spring.datasource.password=secret

Spring Boot auto-configures:
    ✓ HikariCP DataSource (connection pool)
    ✓ Hibernate EntityManagerFactory
    ✓ JpaTransactionManager
    ✓ Spring Data JPA repositories (scan for JpaRepository subinterfaces)

You write:
    public interface UserRepository extends JpaRepository<User, Long> {}

That's it. No XML. No @Bean methods. Hibernate is running.
```

##### 4. Starter Dependencies — Curated Dependency Bundles

Instead of declaring 10 individual Maven dependencies with compatible versions, you declare one **starter**:

```xml
<!-- ❌ Spring MVC (pre-Boot) — manage every dependency manually -->
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <version>5.3.27</version>    <!-- must track versions yourself -->
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>    <!-- must be compatible with spring-webmvc -->
    </dependency>
    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <version>6.0.0</version>
        <scope>provided</scope>
    </dependency>
    <!-- + validation, logging, testing, ... 10+ more entries -->
</dependencies>

<!-- ✅ Spring Boot — one starter pulls in everything compatible -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- No version needed — inherited from spring-boot-starter-parent -->
</dependency>
```

**What `spring-boot-starter-web` includes (transitively):**

```text
spring-boot-starter-web
├── spring-boot-starter              (Core: Spring context, auto-config, logging)
│   ├── spring-core
│   ├── spring-context
│   ├── spring-boot-autoconfigure
│   └── spring-boot-starter-logging (Logback + SLF4J)
├── spring-boot-starter-tomcat       (Embedded Tomcat 10.x)
│   └── tomcat-embed-core
├── spring-webmvc                    (DispatcherServlet, @Controller, etc.)
├── jackson-databind                 (JSON serialization)
└── spring-boot-starter-validation   (Hibernate Validator / Jakarta Validation)
```

**Common starters:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Starter                              What it provides                           │
│  ────────────────────────────────────────────────────────────────────────────    │
│  spring-boot-starter-web              Spring MVC + Jackson + embedded Tomcat     │
│  spring-boot-starter-data-jpa         Hibernate + Spring Data JPA + HikariCP    │
│  spring-boot-starter-security         Spring Security (auth, authz)              │
│  spring-boot-starter-data-redis       Spring Data Redis + Lettuce client         │
│  spring-boot-starter-data-mongodb     Spring Data MongoDB                        │
│  spring-boot-starter-kafka            Spring Kafka                               │
│  spring-boot-starter-test             JUnit 5 + Mockito + MockMvc + Testcontainers│
│  spring-boot-starter-actuator         Health checks, metrics, /actuator endpoints│
│  spring-boot-starter-cache            Spring Cache abstraction                   │
│  spring-boot-starter-mail             JavaMailSender                             │
│  spring-boot-starter-webflux          Spring WebFlux (reactive web)             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 5. Dependency Version Management — `spring-boot-starter-parent`

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>

<!-- Now ALL Spring Boot starters and most popular libraries have
     pre-tested, compatible versions. You NEVER specify <version> for them.
     Spring Boot's BOM (Bill of Materials) manages 300+ dependency versions. -->

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- no <version> needed -->
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <!-- version managed by Spring Boot BOM — currently 8.x -->
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <!-- version managed -->
        <optional>true</optional>
    </dependency>
</dependencies>
```

##### 6. Embedded Server

Spring Boot embeds the servlet container inside the JAR. No separate installation required.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Embedded vs External Server:                                                    │
│                                                                                  │
│  Spring MVC (external):                                                          │
│    Install Tomcat 10 on server → configure server.xml → copy WAR to webapps/    │
│    → restart Tomcat → app runs                                                   │
│    App depends on Tomcat version installed on the server                         │
│                                                                                  │
│  Spring Boot (embedded):                                                         │
│    java -jar myapp.jar → Tomcat starts inside the JVM → app runs                 │
│    Tomcat version is inside the JAR — always the right version                   │
│                                                                                  │
│  Fat JAR structure:                                                              │
│  myapp.jar                                                                       │
│  ├── BOOT-INF/                                                                   │
│  │   ├── classes/                 ← your compiled code                           │
│  │   │   └── com/example/...                                                     │
│  │   └── lib/                     ← ALL dependencies (including Tomcat!)         │
│  │       ├── tomcat-embed-core-10.1.x.jar                                        │
│  │       ├── spring-webmvc-6.x.jar                                               │
│  │       ├── jackson-databind-2.x.jar                                            │
│  │       └── ... (100+ JARs)                                                     │
│  ├── META-INF/                                                                   │
│  │   └── MANIFEST.MF             ← Main-Class: JarLauncher                      │
│  └── org/springframework/boot/   ← Spring Boot's JAR launcher                   │
│      └── loader/JarLauncher.class                                                │
│                                                                                  │
│  Switch embedded server:                                                         │
│  <!-- Exclude default Tomcat, use Jetty instead -->                              │
│  <dependency>                                                                    │
│      <groupId>org.springframework.boot</groupId>                                 │
│      <artifactId>spring-boot-starter-web</artifactId>                            │
│      <exclusions>                                                                │
│          <exclusion>                                                             │
│              <groupId>org.springframework.boot</groupId>                         │
│              <artifactId>spring-boot-starter-tomcat</artifactId>                 │
│          </exclusion>                                                            │
│      </exclusions>                                                               │
│  </dependency>                                                                   │
│  <dependency>                                                                    │
│      <groupId>org.springframework.boot</groupId>                                 │
│      <artifactId>spring-boot-starter-jetty</artifactId>                          │
│  </dependency>                                                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### 7. `application.properties` / `application.yml` — Externalised Configuration

All configuration is in one place, with environment-specific overrides:

```properties
# application.properties

# ── Server ──────────────────────────────────────────────────────────
server.port=8080
server.servlet.context-path=/api
server.tomcat.max-threads=200

# ── Database ─────────────────────────────────────────────────────────
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.connection-timeout=30000

# ── JPA / Hibernate ──────────────────────────────────────────────────
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# ── Logging ──────────────────────────────────────────────────────────
logging.level.root=INFO
logging.level.com.example=DEBUG
logging.file.name=logs/app.log

# ── Custom properties ────────────────────────────────────────────────
app.jwt.secret=my-secret-key
app.jwt.expiration-ms=86400000
```

**Profile-specific config** — override per environment:

```text
application.properties         ← shared defaults
application-dev.properties     ← dev overrides (H2, debug SQL, etc.)
application-staging.properties ← staging overrides
application-prod.properties    ← prod overrides (MySQL, no SQL logging)

# Activate a profile:
java -jar app.jar --spring.profiles.active=prod
# Or:
export SPRING_PROFILES_ACTIVE=prod
```

##### 8. Spring Boot Actuator — Production Observability

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Actuator adds HTTP endpoints for monitoring:

```text
GET /actuator/health      → {"status":"UP", "components": {"db": {"status":"UP"}}}
GET /actuator/metrics     → list of available metrics
GET /actuator/metrics/jvm.memory.used → JVM memory usage
GET /actuator/info        → app info (version, git commit, etc.)
GET /actuator/env         → environment properties
GET /actuator/beans       → all Spring beans in the context
GET /actuator/mappings    → all @RequestMapping endpoints
GET /actuator/loggers     → logging levels (can change at runtime!)
GET /actuator/threaddump  → current JVM thread dump
```

---

#### Advantages and Disadvantages

**Advantages:**

| Advantage | Explanation |
|---|---|
| **All Spring MVC advantages** | DI, AOP, `@Transactional`, `@Cacheable`, REST support, validation — all inherited |
| **Auto-configuration** | 150+ things configured automatically based on classpath/properties — zero boilerplate |
| **Starter dependencies** | One `<dependency>` pulls in a curated, version-compatible set of JARs |
| **Dependency version management** | `spring-boot-starter-parent` manages 300+ library versions — no "dependency hell" |
| **Embedded server** | No Tomcat installation, no WAR deployment — `java -jar app.jar` |
| **Conventions over configuration** | Sane defaults everywhere — only configure what differs from the default |
| **Rapid development** | From `start.spring.io` to a running REST API in under 5 minutes |
| **DevTools hot-reload** | Code changes auto-restart the app during development |
| **Actuator** | Production-ready health, metrics, and management endpoints out of the box |
| **Cloud-native** | Fat JAR packaging fits perfectly into Docker containers and Kubernetes |
| **Testability** | `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest` — layered test slices |
| **Large community** | Most popular Java framework; extensive Stack Overflow, docs, tutorials |

**Disadvantages and Limitations:**

| Disadvantage | Explanation |
|---|---|
| **Slow startup time** | Spring context initialization (classpath scanning, proxying, wiring) takes 3–15s for large apps — bad for AWS Lambda/serverless. GraalVM native image solves this but adds build complexity. |
| **High memory footprint** | Embedded Tomcat + Spring context + all beans = 200–500 MB baseline RAM. Not ideal for very resource-constrained environments. |
| **Auto-configuration magic** | When something goes wrong, the implicit behavior is hard to debug ("why is this bean being created?"). Use `--debug` flag to print auto-configuration report. |
| **Fat JAR size** | A minimal Spring Boot app JAR is 15–40 MB. Fine for microservices, but noticeable in bandwidth-constrained environments. |
| **Over-engineering for simple tasks** | For a simple script or CLI tool, Spring Boot is heavy overhead. |
| **Version upgrade friction** | Major version upgrades (Boot 2 → 3) often require Jakarta EE namespace changes and dependency updates. |
| **Learning curve** | Auto-configuration hides complexity that you need to understand when things break. |

---

#### Industries Where Spring Boot is Used

Spring Boot is the dominant Java backend framework across virtually every industry that uses Java:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot Adoption by Industry:                                               │
│                                                                                  │
│  FinTech / Banking                                                               │
│    → Payment processing microservices, fraud detection, trading platforms        │
│    → Examples: JPMorgan, Goldman Sachs, Visa, Stripe, Revolut                   │
│    → Why: Transaction management (@Transactional), security, audit logging       │
│                                                                                  │
│  E-Commerce / Retail                                                             │
│    → Product catalog, order management, inventory, recommendation APIs          │
│    → Examples: Amazon (parts), Alibaba, Flipkart                                 │
│    → Why: Scalable REST APIs, JPA for product data, Kafka for events            │
│                                                                                  │
│  Healthcare                                                                      │
│    → Electronic health records (EHR), patient management, lab results           │
│    → Examples: Epic Systems integrations, Cerner                                 │
│    → Why: Data persistence, security (HIPAA compliance), integration            │
│                                                                                  │
│  Telecommunications                                                              │
│    → Billing systems, network monitoring, subscriber management                 │
│    → Examples: AT&T, Comcast, Deutsche Telekom                                   │
│                                                                                  │
│  Travel / Logistics                                                              │
│    → Booking engines, fleet tracking, route optimization APIs                   │
│    → Examples: Booking.com, Uber (parts), FedEx                                  │
│                                                                                  │
│  SaaS / Cloud Platforms                                                          │
│    → Multi-tenant backend APIs, developer platforms                             │
│    → Examples: Netflix (Spring Cloud), Atlassian, Salesforce integrations       │
│                                                                                  │
│  Government / Public Sector                                                      │
│    → Citizen-facing portals, data management systems                            │
│                                                                                  │
│  Common pattern across all industries:                                           │
│  Spring Boot REST API ← → React/Angular Frontend                                 │
│  Spring Boot microservices on Kubernetes (EKS, GKE, AKS)                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### JAR vs WAR

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  JAR vs WAR — Full Comparison:                                                   │
│                                                                                  │
│  ┌─────────────────────────┬──────────────────────────┬────────────────────────┐ │
│  │ Aspect                  │ JAR (Spring Boot)         │ WAR (Spring MVC)       │ │
│  ├─────────────────────────┼──────────────────────────┼────────────────────────┤ │
│  │ Full name               │ Java ARchive             │ Web ARchive            │ │
│  │ Contains                │ Classes + ALL deps        │ Classes + app deps     │ │
│  │                         │ (fat/uber JAR)            │ (servlet-api excluded) │ │
│  │ Server                  │ Embedded (Tomcat inside)  │ External (separate     │ │
│  │                         │                           │ Tomcat installation)   │ │
│  │ Run command             │ java -jar app.jar         │ Copy to webapps/ +     │ │
│  │                         │                           │ startup.sh             │ │
│  │ Context path            │ Configured in properties  │ WAR file name becomes  │ │
│  │                         │ (default: /)              │ context path           │ │
│  │ Multiple apps on port   │ Each JAR = separate port  │ Multiple WARs on same  │ │
│  │                         │ (or reverse proxy)        │ Tomcat instance        │ │
│  │ Deployment model        │ Cloud-native, Docker,     │ Traditional datacenter │ │
│  │                         │ Kubernetes                │ hosting                │ │
│  │ Startup                 │ Self-contained            │ Depends on Tomcat      │ │
│  │                         │                           │ being started          │ │
│  │ File size               │ 20–60 MB (fat JAR)        │ 5–20 MB (app only)     │ │
│  │ pom.xml packaging       │ jar (default)             │ war                    │ │
│  │ Spring Boot support     │ ✓ Primary                 │ ✓ Supported (legacy)   │ │
│  └─────────────────────────┴──────────────────────────┴────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**JAR Structure (Spring Boot fat JAR):**

```text
app.jar
├── BOOT-INF/
│   ├── classes/                   ← your compiled .class files
│   │   └── com/example/
│   │       ├── MyApplication.class
│   │       ├── controller/
│   │       ├── service/
│   │       └── repository/
│   ├── classpath.idx              ← ordered index of all JARs
│   └── lib/                       ← ALL dependency JARs (fat!)
│       ├── spring-webmvc-6.x.jar
│       ├── tomcat-embed-core-10.x.jar
│       ├── jackson-databind-2.x.jar
│       └── ... (100+ JARs)
├── META-INF/
│   ├── MANIFEST.MF
│   │   Main-Class: org.springframework.boot.loader.JarLauncher
│   │   Start-Class: com.example.MyApplication
│   └── build-info.properties
└── org/springframework/boot/loader/
    └── JarLauncher.class          ← Spring Boot's custom class loader
```

**WAR Structure (classic):**

```text
app.war
├── WEB-INF/
│   ├── web.xml                    ← deployment descriptor (or absent if using initializer)
│   ├── classes/                   ← your compiled .class files
│   │   └── com/example/
│   └── lib/                       ← app JARs (NOT servlet-api — provided by Tomcat)
│       ├── spring-webmvc-5.x.jar
│       └── jackson-databind-2.x.jar
└── static/                        ← static resources (HTML, CSS, JS)
```

**Can Spring Boot produce a WAR?** Yes — for legacy deployment to existing Tomcat:

```java
// Change main class to extend SpringBootServletInitializer
@SpringBootApplication
public class MyApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MyApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
        // Still works as embedded JAR when run directly
    }
}
```

```xml
<!-- pom.xml: change packaging to war -->
<packaging>war</packaging>

<!-- Mark embedded Tomcat as provided (external Tomcat will be used) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

---

#### How Spring Boot Applications Are Deployed Today

There are multiple deployment patterns depending on the infrastructure target:

##### Pattern 1 — Run Directly (JAR)

```bash
# Build
mvn clean package -DskipTests
# → target/myapp-1.0.0.jar

# Run (development / simple server)
java -jar target/myapp-1.0.0.jar

# Run with profile and JVM tuning (production)
java \
  -Xms512m -Xmx2g \
  -XX:+UseG1GC \
  -Dspring.profiles.active=prod \
  -jar myapp-1.0.0.jar \
  --server.port=8080
```

##### Pattern 2 — Docker Container (most common in production)

```dockerfile
# Dockerfile — multi-stage build
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -f pom.xml clean package -DskipTests

# Stage 2: Run — slim JRE image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user (security best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy JAR from build stage
COPY --from=builder /app/target/myapp-*.jar app.jar

# Expose port
EXPOSE 8080

# Use layered JAR extraction for faster Docker layer caching
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
```

```bash
# Build image
docker build -t myapp:1.0.0 .

# Run container
docker run -d \
  --name myapp \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/mydb \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  myapp:1.0.0
```

**Spring Boot Layered JAR** (better Docker caching):

```xml
<!-- pom.xml: enable layered JAR -->
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <layers>
            <enabled>true</enabled>
        </layers>
    </configuration>
</plugin>
```

```dockerfile
# Layered Dockerfile — only your code layer changes on redeploy
FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /app
COPY target/myapp.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=layers /app/dependencies/          ./
COPY --from=layers /app/spring-boot-loader/    ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/           ./
# Each COPY is a separate Docker layer — dependencies cached, only app layer rebuilt
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

##### Pattern 3 — Kubernetes (K8s)

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 3                         # 3 instances for HA
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
        - name: myapp
          image: myregistry/myapp:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          # Health checks via Actuator
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: myapp-service
spec:
  selector:
    app: myapp
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

```bash
kubectl apply -f deployment.yaml
kubectl get pods
kubectl logs -f deployment/myapp
kubectl rollout status deployment/myapp
```

##### Pattern 4 — Cloud Platform as a Service (PaaS)

```bash
# AWS Elastic Beanstalk
eb init myapp --platform java-21
eb create production
eb deploy          # upload JAR, Beanstalk handles the rest

# Heroku
heroku create myapp
git push heroku main   # Heroku detects Spring Boot, builds, deploys

# Azure App Service
az webapp deploy --resource-group myRG \
                 --name myapp \
                 --src-path target/myapp.jar \
                 --type jar

# Google Cloud Run (containerized, serverless)
gcloud run deploy myapp \
  --image gcr.io/myproject/myapp:1.0.0 \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

##### Pattern 5 — CI/CD Pipeline (Complete Flow)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Modern Spring Boot Deployment Pipeline:                                         │
│                                                                                  │
│  Developer                                                                       │
│      │ git push origin main                                                      │
│      ▼                                                                           │
│  GitHub / GitLab / Bitbucket                                                     │
│      │ webhook triggers CI/CD                                                    │
│      ▼                                                                           │
│  CI Pipeline (GitHub Actions / Jenkins / GitLab CI)                              │
│      ├── mvn clean verify           (compile + unit tests)                       │
│      ├── mvn verify -Pintegration   (integration tests with Testcontainers)      │
│      ├── mvn sonar:sonar            (code quality analysis)                      │
│      ├── docker build -t myapp:$SHA .                                            │
│      ├── docker push myregistry/myapp:$SHA                                       │
│      └── Scan image for CVEs (Trivy / Snyk)                                     │
│      ▼                                                                           │
│  Deploy to Staging                                                               │
│      ├── kubectl set image deployment/myapp myapp=myregistry/myapp:$SHA          │
│      ├── kubectl rollout status deployment/myapp                                 │
│      └── Run smoke tests against staging                                         │
│      ▼                                                                           │
│  Manual approval gate (or auto-promote on green)                                 │
│      ▼                                                                           │
│  Deploy to Production                                                            │
│      ├── kubectl set image deployment/myapp myapp=myregistry/myapp:$SHA -n prod  │
│      └── kubectl rollout status deployment/myapp -n prod                         │
│             ← Kubernetes performs rolling update (zero downtime)                 │
│             ← Old pods stay up until new pods pass readinessProbe                │
│      ▼                                                                           │
│  Monitoring                                                                      │
│      ├── Prometheus scrapes /actuator/prometheus                                 │
│      ├── Grafana dashboards show JVM, HTTP, DB metrics                          │
│      └── PagerDuty / Alertmanager fires alerts on SLA breach                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### GitHub Actions Example (CI/CD)

```yaml
# .github/workflows/ci-cd.yml
name: Build and Deploy

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and Test
        run: mvn clean verify

      - name: Build Docker image
        run: docker build -t myregistry/myapp:${{ github.sha }} .

      - name: Push to registry
        run: |
          echo "${{ secrets.REGISTRY_PASSWORD }}" | docker login myregistry -u ${{ secrets.REGISTRY_USER }} --password-stdin
          docker push myregistry/myapp:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy to Kubernetes
        run: |
          kubectl set image deployment/myapp \
            myapp=myregistry/myapp:${{ github.sha }} \
            --namespace production
          kubectl rollout status deployment/myapp --namespace production
```

---

#### Summary: The Evolution of Java Web Development

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Evolution Summary:                                                              │
│                                                                                  │
│  Era             Tech               Problem Solved         Remaining Pain        │
│  ──────────────────────────────────────────────────────────────────────────────  │
│  1997–2003       Raw Servlets       Server-side Java        One class per URL,   │
│                  + web.xml          (vs CGI scripts)        HTML in Java strings │
│                                                                                  │
│  2003–2014       Spring MVC         IoC, DI, one           External Tomcat,      │
│                  + Hibernate        DispatcherServlet,       WAR packaging,       │
│                  + JPA              clean separation         100s of lines of     │
│                                                             @Configuration code  │
│                                                                                  │
│  2014–present    Spring Boot        Auto-config,            Slow startup,        │
│                  + Spring Cloud     embedded server,         memory footprint,    │
│                  + Docker/K8s       fat JAR, starters,      magic debugging      │
│                                     one-line deploy                               │
│                                                                                  │
│  2022–present    Spring Boot 3      GraalVM native image,   Native compilation   │
│                  + GraalVM          millisecond startup,     constraints,        │
│                                     10 MB binary            less mature tooling  │
└──────────────────────────────────────────────────────────────────────────────────┘
```


---

#### `@SpringBootApplication` — How It Is Constructed

`@SpringBootApplication` is a **meta-annotation** — an annotation composed of other annotations. Looking at its actual source code reveals exactly how it works:

```java
// Spring Boot source — SpringBootApplication.java (simplified)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration          // (1) itself a meta-annotation for @Configuration
@EnableAutoConfiguration          // (2) triggers auto-configuration
@ComponentScan(                   // (3) scans current package + sub-packages
    excludeFilters = {
        @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
    }
)
public @interface SpringBootApplication {

    // Convenience: exclude specific auto-config classes
    @AliasFor(annotation = EnableAutoConfiguration.class)
    Class<?>[] exclude() default {};

    // Convenience: exclude by class name string
    @AliasFor(annotation = EnableAutoConfiguration.class)
    String[] excludeName() default {};

    // Convenience: override @ComponentScan base packages
    @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
    String[] scanBasePackages() default {};

    // Convenience: type-safe alternative to scanBasePackages
    @AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
    Class<?>[] scanBasePackageClasses() default {};
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  @SpringBootApplication — Annotation Composition:                                │
│                                                                                  │
│  @SpringBootApplication                                                          │
│      │                                                                           │
│      ├── @SpringBootConfiguration                                                │
│      │       └── @Configuration                                                  │
│      │               → Marks class as a bean definition source                  │
│      │               → Enables CGLIB proxy for @Bean methods (singleton safety) │
│      │                                                                           │
│      ├── @EnableAutoConfiguration                                                │
│      │       └── @AutoConfigurationPackage                                       │
│      │               → Registers the base package for JPA entity scanning etc.  │
│      │       → Imports AutoConfigurationImportSelector                           │
│      │         → Reads META-INF/spring/                                          │
│      │             org.springframework.boot.autoconfigure                        │
│      │             .AutoConfiguration.imports                                    │
│      │         → Filters with @Conditional annotations                           │
│      │         → Loads matching auto-config @Configuration classes               │
│      │                                                                           │
│      └── @ComponentScan                                                          │
│              → Scans the package of the annotated class + all sub-packages      │
│              → Finds: @Component, @Service, @Repository, @Controller,           │
│                        @RestController, @Configuration, @Aspect, ...            │
│              → Registers found classes as Spring beans                           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**The three annotations work together in a specific order:**

```text
SpringApplication.run(MyApplication.class, args)
    │
    ▼
1. Create ApplicationContext
   (AnnotationConfigServletWebServerApplicationContext for web apps)
    │
    ▼
2. @ComponentScan fires
   → Scans com.example.** for @Component stereotypes
   → Registers UserController, UserService, UserRepository as bean definitions
    │
    ▼
3. @Configuration fires
   → MyApplication itself can now define @Bean methods
    │
    ▼
4. @EnableAutoConfiguration fires
   → AutoConfigurationImportSelector reads AutoConfiguration.imports
   → ~150 candidate auto-config classes loaded
   → Each evaluated against @Conditional checks:
       DataSourceAutoConfiguration:
           @ConditionalOnClass(DataSource.class)       ✓ (HikariCP on classpath)
           @ConditionalOnMissingBean(DataSource.class) ✓ (user hasn't defined one)
           → ACTIVATED → DataSource bean created
       WebMvcAutoConfiguration:
           @ConditionalOnClass(DispatcherServlet.class) ✓
           @ConditionalOnWebApplication                 ✓
           → ACTIVATED → DispatcherServlet, HandlerMappings, MessageConverters created
    │
    ▼
5. All beans instantiated and wired (dependency injection)
    │
    ▼
6. Embedded Tomcat started (EmbeddedWebServerFactoryCustomizerAutoConfiguration)
    │
    ▼
7. DispatcherServlet registered in Tomcat context
    │
    ▼
8. ApplicationReadyEvent published
   → App is live
```

**Customising `@SpringBootApplication`:**

```java
// Exclude an auto-config class you don't want
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,     // no DB in this service
    SecurityAutoConfiguration.class        // custom security config
})
public class MyApplication { ... }

// Restrict component scan to specific packages
@SpringBootApplication(scanBasePackages = {
    "com.example.web",
    "com.example.service"
    // Does NOT scan com.example.legacy — intentionally excluded
})
public class MyApplication { ... }

// Type-safe base package selection
@SpringBootApplication(scanBasePackageClasses = {
    UserController.class,   // scans the package containing UserController
    OrderService.class      // scans the package containing OrderService
})
public class MyApplication { ... }
```

**Debug auto-configuration decisions:**

```bash
# Print the auto-configuration report at startup
java -jar myapp.jar --debug

# Output includes:
# Positive matches (activated):
#   DataSourceAutoConfiguration matched:
#     - @ConditionalOnClass found required class 'javax.sql.DataSource' (OnClassCondition)
#
# Negative matches (not activated):
#   MongoAutoConfiguration:
#     Did not match:
#       - @ConditionalOnClass did not find required class 'com.mongodb.MongoClient'
```

---

#### Layered Architecture of Spring Boot

Spring Boot applications follow a **strict layered architecture** that separates concerns into distinct layers. Each layer has a single responsibility and communicates only with adjacent layers.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot Layered Architecture:                                               │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │                        Presentation Layer                                 │  │
│  │                                                                            │  │
│  │   @RestController / @Controller                                           │  │
│  │   Handles HTTP requests, validates input, returns HTTP responses          │  │
│  │   NO business logic here — only request/response translation              │  │
│  │                                                                            │  │
│  │   UserController, OrderController, ProductController                      │  │
│  └──────────────────────────────────┬─────────────────────────────────────────┘  │
│                                     │ calls (DTOs / domain objects)             │
│  ┌──────────────────────────────────▼─────────────────────────────────────────┐  │
│  │                         Service Layer                                      │  │
│  │                                                                            │  │
│  │   @Service                                                                 │  │
│  │   Contains ALL business logic, orchestrates domain operations             │  │
│  │   Manages transactions (@Transactional)                                   │  │
│  │   Does NOT know about HTTP — works with plain Java objects                 │  │
│  │                                                                            │  │
│  │   UserService, OrderService, EmailService                                  │  │
│  └──────────────────────────────────┬─────────────────────────────────────────┘  │
│                                     │ calls (domain objects / entities)         │
│  ┌──────────────────────────────────▼─────────────────────────────────────────┐  │
│  │                       Repository Layer                                     │  │
│  │                                                                            │  │
│  │   @Repository / JpaRepository                                              │  │
│  │   Data access — reads/writes to DB, cache, external storage               │  │
│  │   Wraps JPA/JDBC/MongoDB/Redis — hides persistence details from service   │  │
│  │                                                                            │  │
│  │   UserRepository, OrderRepository, ProductRepository                      │  │
│  └──────────────────────────────────┬─────────────────────────────────────────┘  │
│                                     │ SQL / NoSQL queries                       │
│  ┌──────────────────────────────────▼─────────────────────────────────────────┐  │
│  │                          Database / External                               │  │
│  │                                                                            │  │
│  │   MySQL / PostgreSQL / MongoDB / Redis / Elasticsearch                    │  │
│  │   Managed by Hibernate ORM or Spring Data                                 │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
│  Cross-cutting concerns (handled by AOP, not in layers):                         │
│   Security → @PreAuthorize on service methods                                   │
│   Logging  → @Aspect / SLF4J throughout                                         │
│   Caching  → @Cacheable on service methods                                      │
│   Validation → @Valid on controller params                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Project Structure That Reflects the Layers

```text
src/main/java/com/example/myapp/
├── MyAppApplication.java              ← Entry point
│
├── controller/                        ← Presentation layer
│   ├── UserController.java
│   ├── OrderController.java
│   └── advice/
│       └── GlobalExceptionHandler.java
│
├── service/                           ← Business/Service layer
│   ├── UserService.java               ← interface (optional but recommended)
│   ├── impl/
│   │   └── UserServiceImpl.java       ← implementation
│   └── OrderService.java
│
├── repository/                        ← Data Access layer
│   ├── UserRepository.java            ← extends JpaRepository
│   └── OrderRepository.java
│
├── model/                             ← Domain / Entity classes
│   ├── entity/
│   │   ├── User.java                  ← @Entity (JPA)
│   │   └── Order.java
│   └── dto/
│       ├── UserRequest.java           ← inbound DTO (what controller receives)
│       ├── UserResponse.java          ← outbound DTO (what controller returns)
│       └── OrderRequest.java
│
├── mapper/                            ← Entity ↔ DTO conversion
│   └── UserMapper.java               ← (MapStruct or manual)
│
├── config/                            ← Configuration classes
│   ├── SecurityConfig.java
│   ├── CacheConfig.java
│   └── OpenApiConfig.java
│
├── exception/                         ← Custom exception classes
│   ├── UserNotFoundException.java
│   └── DuplicateEmailException.java
│
└── util/                              ← Utility / helper classes
    └── JwtUtil.java
```

##### Full Working Example — All Layers

```java
// ── model/entity/User.java ───────────────────────────────────────────────────────
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}


// ── model/dto/UserRequest.java ───────────────────────────────────────────────────
@Getter @Setter
public class UserRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Email(message = "Must be a valid email")
    private String email;
}


// ── model/dto/UserResponse.java ──────────────────────────────────────────────────
@Getter @Setter @AllArgsConstructor
public class UserResponse {
    private Long   id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}


// ── repository/UserRepository.java ──────────────────────────────────────────────
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByNameContainingIgnoreCase(String name);
}


// ── service/UserService.java (interface) ─────────────────────────────────────────
public interface UserService {
    List<UserResponse>  findAll();
    UserResponse        findById(Long id);
    UserResponse        create(UserRequest request);
    UserResponse        update(Long id, UserRequest request);
    void                delete(Long id);
}


// ── service/impl/UserServiceImpl.java ───────────────────────────────────────────
@Service
@Transactional(readOnly = true)    // default: all methods read-only
@RequiredArgsConstructor           // Lombok: generates constructor for final fields
@Slf4j                             // Lombok: injects Logger log = LoggerFactory.getLogger(...)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper     userMapper;

    @Override
    public List<UserResponse> findAll() {
        log.debug("Fetching all users");
        return userRepository.findAll()
                             .stream()
                             .map(userMapper::toResponse)
                             .collect(Collectors.toList());
    }

    @Override
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional    // override: this method WRITES
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }
        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);
        log.info("Created user id={}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        // No explicit save() needed — JPA dirty checking detects changes
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user id={}", id);
    }
}


// ── controller/UserController.java ──────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management API")   // OpenAPI / Swagger
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @RequestBody @Valid UserRequest request) {
        UserResponse created = userService.create(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
        return ResponseEntity.created(location).body(created);  // 201 Created
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();   // 204 No Content
    }
}


// ── controller/advice/GlobalExceptionHandler.java ───────────────────────────────
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(UserNotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicateEmailException ex) {
        return new ErrorResponse("DUPLICATE_EMAIL", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return new ErrorResponse("VALIDATION_ERROR", msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception ex) {
        log.error("Unexpected error", ex);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        // Never expose ex.getMessage() to client — it may leak internal details
    }
}
```

##### The Data Flow Between Layers

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  POST /api/v1/users — Data Flow Through Layers:                                  │
│                                                                                  │
│  HTTP Request Body: {"name":"Alice","email":"alice@example.com"}                  │
│       │                                                                          │
│       ▼  [Presentation Layer]                                                    │
│  UserController.create(@RequestBody @Valid UserRequest request)                   │
│       │  Spring deserialises JSON → UserRequest DTO                              │
│       │  @Valid triggers validation (name/email constraints)                     │
│       │  Calls: userService.create(request)                                      │
│       │                                                                          │
│       ▼  [Service Layer]                                                         │
│  UserServiceImpl.create(UserRequest request)                                     │
│       │  Check: userRepository.existsByEmail(request.getEmail())                 │
│       │  Map: userMapper.toEntity(request) → User entity                         │
│       │  Calls: userRepository.save(user)                                        │
│       │                                                                          │
│       ▼  [Repository Layer]                                                      │
│  UserRepository.save(user)                                                       │
│       │  Hibernate generates: INSERT INTO users (name, email, created_at)        │
│       │                       VALUES (?, ?, ?)                                   │
│       │  Returns saved User with generated id                                    │
│       │                                                                          │
│       ↑  [Service Layer]                                                         │
│  userMapper.toResponse(saved) → UserResponse DTO                                 │
│       │                                                                          │
│       ↑  [Presentation Layer]                                                    │
│  ResponseEntity.created(location).body(userResponse)                             │
│       │  Spring serialises UserResponse → JSON                                   │
│       │                                                                          │
│  HTTP Response: 201 Created                                                      │
│  Location: /api/v1/users/42                                                      │
│  Body: {"id":42,"name":"Alice","email":"alice@example.com","createdAt":"..."}    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Overall Request Flow in Spring Boot

Spring Boot's request flow is identical to Spring MVC's — because Spring Boot IS Spring MVC underneath, with embedded Tomcat added. Here is the complete flow with all Spring Boot-specific components:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot Request Lifecycle — Full Detail:                                    │
│                                                                                  │
│  Client (Browser / Mobile / Postman / API)                                       │
│      │                                                                           │
│      │  HTTP GET /api/v1/users/42                                                │
│      │  Headers: Authorization: Bearer <jwt>, Accept: application/json           │
│      ▼                                                                           │
│  ┌──────────────────────────────────────────────────────────────────────────┐    │
│  │               Embedded Tomcat (started by Spring Boot)                   │    │
│  │                                                                          │    │
│  │  [1] Accept TCP connection on port 8080 (NIO connector)                  │    │
│  │  [2] Parse HTTP bytes → HttpServletRequest + HttpServletResponse         │    │
│  │  [3] Pass to Servlet Filter Chain                                        │    │
│  │                                                                          │    │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │    │
│  │  │             Servlet Filter Chain                                   │  │    │
│  │  │  (configured by Spring Security auto-config + your custom filters) │  │    │
│  │  │                                                                    │  │    │
│  │  │  CharacterEncodingFilter   → ensures UTF-8 encoding               │  │    │
│  │  │  CorsFilter                → handles CORS preflight               │  │    │
│  │  │  SecurityFilterChain       → Spring Security filters:             │  │    │
│  │  │      UsernamePasswordAuthFilter  (for login endpoint)             │  │    │
│  │  │      JwtAuthenticationFilter    (your custom filter)              │  │    │
│  │  │          → validates JWT                                          │  │    │
│  │  │          → sets SecurityContext with authenticated user           │  │    │
│  │  │      ExceptionTranslationFilter  (401/403 handling)               │  │    │
│  │  │      FilterSecurityInterceptor   (authz check)                    │  │    │
│  │  │  LoggingFilter             → MDC correlation ID injection         │  │    │
│  │  └────────────────────────────────┬───────────────────────────────────┘  │    │
│  │                                   │                                      │    │
│  │  [4] Request reaches              ▼                                      │    │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │    │
│  │  │                    DispatcherServlet                               │  │    │
│  │  │                    (registered at "/" by Spring Boot auto-config)  │  │    │
│  │  │                                                                    │  │    │
│  │  │  [5] HandlerMapping.getHandler("/api/v1/users/42", GET)           │  │    │
│  │  │      → RequestMappingHandlerMapping scans @RequestMapping cache   │  │    │
│  │  │      → Returns: UserController#getById + [LoggingInterceptor]     │  │    │
│  │  │                                                                    │  │    │
│  │  │  [6] Select HandlerAdapter                                         │  │    │
│  │  │      → RequestMappingHandlerAdapter (handles @RequestMapping)      │  │    │
│  │  │                                                                    │  │    │
│  │  │  [7] Interceptor.preHandle()                                       │  │    │
│  │  │      LoggingInterceptor: log "START GET /api/v1/users/42"         │  │    │
│  │  │                                                                    │  │    │
│  │  │  [8] HandlerAdapter resolves method arguments:                    │  │    │
│  │  │      @PathVariable Long id → 42L                                   │  │    │
│  │  │      (no @RequestBody for GET)                                     │  │    │
│  │  │                                                                    │  │    │
│  │  │  [9] Call UserController.getById(42L)                              │  │    │
│  │  │      → UserService.findById(42L)  [service layer]                 │  │    │
│  │  │          → @Transactional(readOnly=true) proxy begins TX          │  │    │
│  │  │          → UserRepository.findById(42L) [repository layer]        │  │    │
│  │  │              → Hibernate: SELECT * FROM users WHERE id=42         │  │    │
│  │  │              → Returns User entity                                 │  │    │
│  │  │          → userMapper.toResponse(user) → UserResponse DTO         │  │    │
│  │  │          → @Transactional proxy commits/closes TX                 │  │    │
│  │  │      ← returns ResponseEntity<UserResponse>                       │  │    │
│  │  │                                                                    │  │    │
│  │  │  [10] Interceptor.postHandle()                                     │  │    │
│  │  │       LoggingInterceptor: log timing                               │  │    │
│  │  │                                                                    │  │    │
│  │  │  [11] Resolve return value:                                        │  │    │
│  │  │       @RestController → @ResponseBody is implicit                  │  │    │
│  │  │       Content negotiation: client sent Accept: application/json    │  │    │
│  │  │       → MappingJackson2HttpMessageConverter selected               │  │    │
│  │  │       → UserResponse serialised to JSON bytes                      │  │    │
│  │  │       → Written to HttpServletResponse output stream               │  │    │
│  │  │       → Status 200 OK                                              │  │    │
│  │  │                                                                    │  │    │
│  │  │  [12] Interceptor.afterCompletion()                                │  │    │
│  │  │       LoggingInterceptor: clear MDC context                        │  │    │
│  │  └────────────────────────────────────────────────────────────────────┘  │    │
│  │                                                                          │    │
│  │  [13] Tomcat flushes response → TCP → Client                             │    │
│  └──────────────────────────────────────────────────────────────────────────┘    │
│      │                                                                           │
│  HTTP 200 OK                                                                     │
│  Content-Type: application/json                                                  │
│  {"id":42,"name":"Alice","email":"alice@example.com","createdAt":"2026-05-11..."}│
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Filters vs Interceptors — What's the Difference?

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Filters vs Interceptors:                                                        │
│                                                                                  │
│  ┌───────────────────────────┬──────────────────────────────────────────────┐    │
│  │ Filter (javax/jakarta)    │ HandlerInterceptor (Spring MVC)              │    │
│  ├───────────────────────────┼──────────────────────────────────────────────┤    │
│  │ Part of Servlet spec      │ Part of Spring MVC                           │    │
│  │ Runs BEFORE DispatcherServlet│ Runs INSIDE DispatcherServlet             │    │
│  │ Sees raw Request/Response │ Sees resolved handler (which @Controller)    │    │
│  │ Can block ALL requests    │ Can be mapped to specific URL patterns       │    │
│  │ Used for: Security, CORS, │ Used for: logging, auth-by-handler,          │    │
│  │  encoding, compression    │  performance tracking, model population      │    │
│  │ Has no Spring context     │ Has full Spring context access               │    │
│  │ access (unless declared   │                                              │    │
│  │  as Spring beans)         │                                              │    │
│  └───────────────────────────┴──────────────────────────────────────────────┘    │
│                                                                                  │
│  Order of execution:                                                             │
│  Filter1.doFilter()                                                              │
│    → Filter2.doFilter()                                                          │
│       → DispatcherServlet.service()                                              │
│            → Interceptor.preHandle()                                             │
│                 → Controller method                                              │
│            ← Interceptor.postHandle()                                            │
│       ← Interceptor.afterCompletion()                                            │
│    ← Filter2 (after chain.doFilter returns)                                      │
│  ← Filter1 (after chain.doFilter returns)                                        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Best Practices for Spring Boot Applications

##### 1. Package by Feature, Not by Layer

```text
❌ Package by layer (common but problematic):
com.example/
├── controller/   ← ALL controllers together
├── service/      ← ALL services together
└── repository/   ← ALL repositories together

✓ Package by feature (preferred for large apps):
com.example/
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   ├── User.java
│   └── UserDto.java
├── order/
│   ├── OrderController.java
│   ├── OrderService.java
│   └── Order.java
└── payment/
    ├── PaymentController.java
    └── PaymentService.java

Why: feature packages have high cohesion, low coupling.
Each feature can be extracted into a microservice with minimal refactoring.
```

##### 2. Use DTOs — Never Expose Entities Directly

```java
// ❌ NEVER return JPA entities directly from controllers
@GetMapping("/{id}")
public User getById(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow(...);
    // Problems:
    // 1. Lazy-loaded collections trigger N+1 queries
    // 2. Circular references cause infinite JSON recursion
    // 3. Exposes DB schema — any column rename breaks the API
    // 4. @Version, @CreatedBy, etc. fields leak to clients
}

// ✅ Always map to a DTO
@GetMapping("/{id}")
public UserResponse getById(@PathVariable Long id) {
    User user = userRepository.findById(id).orElseThrow(...);
    return userMapper.toResponse(user);   // only expose what clients need
}
```

##### 3. Constructor Injection (Not Field Injection)

```java
// ❌ Field injection — hard to test, hides dependencies
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;    // cannot be set in unit tests without Spring context
    @Autowired
    private EmailService emailService;
}

// ✅ Constructor injection — testable, final fields, explicit dependencies
@Service
@RequiredArgsConstructor   // Lombok generates the constructor
public class UserService {
    private final UserRepository userRepository;   // final = immutable after construction
    private final EmailService   emailService;
    // Unit test: new UserService(mockRepo, mockEmail) — no Spring needed
}
```

##### 4. Externalise All Configuration

```java
// ❌ Hardcoded configuration
@Service
public class PaymentService {
    private static final String API_URL = "https://api.stripe.com/v1";  // hardcoded
    private static final int TIMEOUT = 5000;                             // hardcoded
}

// ✅ Externalised via @ConfigurationProperties
@ConfigurationProperties(prefix = "payment")
@Validated
@Getter @Setter
public class PaymentProperties {
    @NotBlank
    private String apiUrl;
    @Min(1000) @Max(30000)
    private int timeoutMs = 5000;   // default value
    @NotBlank
    private String secretKey;
}

// application.properties:
// payment.api-url=https://api.stripe.com/v1
// payment.timeout-ms=5000
// payment.secret-key=${STRIPE_SECRET_KEY}  ← read from env var

// Register in main config:
@SpringBootApplication
@ConfigurationPropertiesScan   // auto-detects @ConfigurationProperties classes
public class MyApplication { ... }
```

##### 5. Use Profiles for Environment Config

```properties
# application.properties (shared)
spring.application.name=my-service
server.port=8080

# application-dev.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.show-sql=true
logging.level.com.example=DEBUG

# application-prod.properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.show-sql=false
logging.level.root=WARN
```

```java
// Beans active only in specific profiles
@Configuration
@Profile("dev")
public class DevDataLoader implements CommandLineRunner {
    @Override
    public void run(String... args) {
        // Insert test data — only runs in dev profile
    }
}
```

##### 6. Always Validate at the Boundary

```java
// ✅ Validate ALL inbound data at the controller boundary
@PostMapping
public ResponseEntity<UserResponse> create(
        @RequestBody @Valid UserRequest request) {   // @Valid triggers bean validation
    // By the time this method body runs, request is guaranteed valid
    return ResponseEntity.status(201).body(userService.create(request));
}

// ✅ Validate @ConfigurationProperties at startup
@ConfigurationProperties(prefix = "app")
@Validated               // validates the properties at startup — fail fast
@Getter @Setter
public class AppProperties {
    @NotBlank
    private String jwtSecret;
    @Positive
    private long jwtExpirationMs;
}
```

##### 7. Never Log Sensitive Data

```java
// ❌ Logging sensitive data
log.info("User login: email={}, password={}", email, password);    // NEVER log passwords
log.debug("Payment request: card={}, cvv={}", card, cvv);          // NEVER log card data
log.info("JWT token: {}", token);                                  // NEVER log tokens

// ✅ Log identifiers and non-sensitive metadata only
log.info("User login attempt: email={}", email);
log.info("Payment initiated: userId={}, amount={}", userId, amount);
log.debug("Request: method={}, path={}, userId={}", method, path, userId);
```

##### 8. Use Pagination for List Endpoints

```java
// ❌ Return all records — unbounded query, OOM risk
@GetMapping
public List<User> list() {
    return userRepository.findAll();   // could return millions of rows
}

// ✅ Paginate
@GetMapping
public Page<UserResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id") String sortBy) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
    return userRepository.findAll(pageable)
                         .map(userMapper::toResponse);
}
// GET /users?page=0&size=20&sortBy=name
// Response: {"content":[...],"totalElements":500,"totalPages":25,"number":0}
```

##### 9. Structured Logging with Correlation IDs

```java
// ✅ Add correlation ID to every request for distributed tracing
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final String HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {
        String id = Optional.ofNullable(req.getHeader(HEADER))
                            .orElse(UUID.randomUUID().toString());
        MDC.put("correlationId", id);   // added to every log line
        res.setHeader(HEADER, id);      // echo back to client
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();   // MUST clear to avoid thread pool contamination
        }
    }
}

// logback-spring.xml pattern:
// [%d{ISO8601}] [%X{correlationId}] [%-5level] %logger{36} - %msg%n
```

##### 10. Test in Layers — Use Test Slices

```java
// ✅ Unit test: Service layer — no Spring context, pure Java + Mockito
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    @Test
    void findById_throwsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.findById(99L));
    }
}

// ✅ Slice test: Controller layer — loads ONLY web context (fast)
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean  UserService userService;   // mock the service

    @Test
    void getById_returns200() throws Exception {
        when(userService.findById(1L))
            .thenReturn(new UserResponse(1L, "Alice", "a@b.com", LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/users/1")
               .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Alice"));
    }
}

// ✅ Slice test: Repository layer — loads ONLY JPA context
@DataJpaTest   // uses H2 in-memory, no web layer
class UserRepositoryTest {
    @Autowired UserRepository userRepository;
    @Autowired TestEntityManager em;

    @Test
    void findByEmail_returnsUser() {
        em.persist(new User("Alice", "a@b.com"));
        Optional<User> found = userRepository.findByEmail("a@b.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
    }
}

// ✅ Integration test: Full context (use sparingly — slow)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired TestRestTemplate restTemplate;

    @Test
    void createAndRetrieveUser() {
        UserRequest req = new UserRequest("Alice", "alice@example.com");
        ResponseEntity<UserResponse> created = restTemplate.postForEntity(
            "/api/v1/users", req, UserResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().getId()).isPositive();
    }
}
```

##### Quick Reference — Best Practices Checklist

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot Best Practices — Quick Reference:                                   │
│                                                                                  │
│  Architecture                                                                    │
│  ✓ Controller → Service → Repository (strict layer direction)                   │
│  ✓ DTOs in/out of controllers — never expose @Entity directly                   │
│  ✓ Package by feature for large apps                                             │
│  ✓ Interfaces for service layer (enables mocking, proxy-friendliness)           │
│                                                                                  │
│  Configuration                                                                   │
│  ✓ @ConfigurationProperties with @Validated for type-safe config                │
│  ✓ Environment-specific profiles (dev/staging/prod)                             │
│  ✓ Secrets via environment variables, never in application.properties           │
│  ✓ Constructor injection — never @Autowired field injection                     │
│                                                                                  │
│  API Design                                                                      │
│  ✓ Versioned APIs (/api/v1/...)                                                  │
│  ✓ @Valid on all @RequestBody parameters                                        │
│  ✓ Pagination for list endpoints (Page<T> + Pageable)                           │
│  ✓ @ControllerAdvice for centralised exception handling                         │
│  ✓ Consistent error response format                                             │
│                                                                                  │
│  Data                                                                            │
│  ✓ @Transactional(readOnly=true) by default on service, override for writes     │
│  ✓ Flyway/Liquibase for schema migrations — never ddl-auto=update in prod       │
│  ✓ Avoid N+1 queries — use JOIN FETCH or @EntityGraph                           │
│                                                                                  │
│  Security                                                                        │
│  ✓ Never log passwords, tokens, card numbers, PII                               │
│  ✓ Input validation at controller boundary                                      │
│  ✓ Secrets from environment variables or Vault                                  │
│  ✓ Run container as non-root user                                               │
│                                                                                  │
│  Observability                                                                   │
│  ✓ Structured logging with correlation IDs (MDC)                                │
│  ✓ spring-boot-starter-actuator for health + metrics                            │
│  ✓ /actuator/health used as Kubernetes liveness/readiness probe                 │
│                                                                                  │
│  Testing                                                                         │
│  ✓ Unit test services with Mockito (no Spring context)                          │
│  ✓ @WebMvcTest for controller tests                                             │
│  ✓ @DataJpaTest for repository tests                                            │
│  ✓ Testcontainers for integration tests (real DB)                               │
│  ✓ Avoid @SpringBootTest for every test (slow)                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```
