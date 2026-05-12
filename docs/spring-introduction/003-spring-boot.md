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


---

#### What is a Servlet Container?

A **Servlet Container** (also called a **Web Container** or **Servlet Engine**) is the runtime environment that manages the lifecycle of Java Servlets. It is the component that actually receives raw HTTP requests from clients over the network, converts them into Java objects (`HttpServletRequest` / `HttpServletResponse`), routes them to the correct Servlet, and sends the HTTP response bytes back to the client.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What a Servlet Container Does:                                                  │
│                                                                                  │
│  1. Listens on a TCP port (e.g., 8080) for incoming HTTP connections            │
│  2. Parses raw HTTP bytes into HttpServletRequest + HttpServletResponse objects  │
│  3. Manages Servlet lifecycle:                                                   │
│       • Loads Servlet class (on first request or at startup)                    │
│       • Calls init(ServletConfig) — once, when Servlet is created               │
│       • Calls service(request, response) — on every request                     │
│       • Calls destroy() — when Servlet is removed or container shuts down       │
│  4. Manages a thread pool — each request is handled on a separate thread        │
│  5. Manages sessions (HttpSession) across stateless HTTP                        │
│  6. Executes the Filter chain before reaching the Servlet                       │
│  7. Handles connection keep-alive, chunked encoding, SSL/TLS termination        │
│  8. Sends serialised HTTP response bytes back over the network                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Servlet Lifecycle — Managed Entirely by the Container

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Servlet Lifecycle (managed by the Servlet Container):                            │
│                                                                                  │
│  Container starts (or first request arrives)                                     │
│      │                                                                           │
│      ▼                                                                           │
│  1. Load Servlet class                                                           │
│      → Class.forName("com.example.MyServlet")                                   │
│      → Container uses ClassLoader to load the .class file                       │
│      │                                                                           │
│      ▼                                                                           │
│  2. Instantiate                                                                  │
│      → MyServlet servlet = new MyServlet()                                      │
│      → Only ONE instance created (singleton per Servlet definition)             │
│      │                                                                           │
│      ▼                                                                           │
│  3. init(ServletConfig config)                                                   │
│      → Called ONCE after instantiation                                           │
│      → Used for one-time setup (DB connections, resource loading)               │
│      → ServletConfig provides init parameters from web.xml / @WebServlet       │
│      │                                                                           │
│      ▼                                                                           │
│  4. service(HttpServletRequest req, HttpServletResponse res)                     │
│      → Called on EVERY request (from a thread pool thread)                       │
│      → HttpServlet.service() dispatches based on HTTP method:                   │
│           GET  → doGet(req, res)                                                │
│           POST → doPost(req, res)                                               │
│           PUT  → doPut(req, res)                                                │
│           DELETE → doDelete(req, res)                                            │
│      → Multiple threads may call service() concurrently on the SAME instance   │
│      │                                                                           │
│      ▼                                                                           │
│  5. destroy()                                                                    │
│      → Called ONCE when container shuts down or Servlet is unloaded             │
│      → Used for cleanup (close DB connections, flush caches)                    │
│                                                                                  │
│  Timeline:                                                                       │
│  ──────────────────────────────────────────────────────────────────────────────  │
│  init()   service()  service()  service()  service()  ...  destroy()            │
│   (1x)    (thread1)  (thread2)  (thread1)  (thread3)       (1x)                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Servlet Container Examples

```text
┌────────────────────┬─────────────────────────────────────────────────────────────┐
│ Container          │ Description                                                 │
├────────────────────┼─────────────────────────────────────────────────────────────┤
│ Apache Tomcat      │ Most widely used. Implements Servlet + JSP specs.           │
│                    │ Spring Boot embeds Tomcat by default.                       │
│ Eclipse Jetty      │ Lightweight, embeddable. Used in many frameworks.          │
│ Undertow           │ High-performance, non-blocking. Red Hat / WildFly.         │
│ GlassFish          │ Reference implementation of Jakarta EE (full app server).  │
│ WildFly            │ Full Jakarta EE app server (includes a Servlet container). │
└────────────────────┴─────────────────────────────────────────────────────────────┘
```

##### Servlet Container in Spring Boot

In Spring Boot, the Servlet Container is **embedded inside your application JAR**. You don't install Tomcat separately — Spring Boot starts it programmatically:

```java
// What Spring Boot does internally (simplified):
public class TomcatServletWebServerFactory {

    public WebServer getWebServer(ServletContextInitializer... initializers) {
        Tomcat tomcat = new Tomcat();                        // create Tomcat instance
        tomcat.setPort(8080);                                // set port from server.port
        tomcat.getConnector();                               // create NIO connector
        Context context = tomcat.addContext("", docBase);    // create servlet context

        // Register DispatcherServlet into Tomcat
        Tomcat.addServlet(context, "dispatcherServlet", dispatcherServlet);
        context.addServletMappingDecoded("/", "dispatcherServlet");

        tomcat.start();                                      // start listening
        return new TomcatWebServer(tomcat);
    }
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Traditional Deployment vs Spring Boot:                                          │
│                                                                                  │
│  Traditional:                                                                    │
│  ┌──────────────────────────────────────┐                                       │
│  │  Tomcat (installed on server)        │ ← YOU install and configure Tomcat    │
│  │  ├── webapps/                        │                                       │
│  │  │   ├── app1.war                    │ ← YOU deploy WAR files               │
│  │  │   └── app2.war                    │                                       │
│  │  └── conf/server.xml                 │ ← YOU configure ports, connectors    │
│  └──────────────────────────────────────┘                                       │
│                                                                                  │
│  Spring Boot:                                                                    │
│  ┌──────────────────────────────────────┐                                       │
│  │  myapp.jar                           │ ← single artifact                    │
│  │  ├── Your code (.class files)        │                                       │
│  │  ├── Tomcat JARs (embedded)          │ ← Tomcat is a library dependency     │
│  │  └── Spring Boot Loader              │                                       │
│  └──────────────────────────────────────┘                                       │
│  java -jar myapp.jar → Tomcat starts inside the JVM                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Raw Servlet Example (Before Spring)

```java
// A raw Servlet — what developers wrote before Spring MVC
@WebServlet(urlPatterns = "/users")
public class UserServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // One-time setup: initialize DB connection pool, load resources
        System.out.println("UserServlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        // Container calls this for every GET /users request
        String userId = req.getParameter("id");

        res.setContentType("application/json");
        res.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = res.getWriter();
        out.write("{\"id\":\"" + userId + "\",\"name\":\"Alice\"}");
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        // Container calls this for every POST /users request
        BufferedReader reader = req.getReader();
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        // Manually parse JSON, validate, save to DB, write response...
        // No DI, no automatic JSON conversion, no validation framework
    }

    @Override
    public void destroy() {
        // Cleanup: close DB connections
        System.out.println("UserServlet destroyed");
    }
}
```

---

#### What is DispatcherServlet?

The **DispatcherServlet** is Spring MVC's **front controller** — a single Servlet that receives ALL incoming HTTP requests and dispatches them to the appropriate `@Controller` method. It is the bridge between the Servlet Container world (raw `HttpServletRequest`/`HttpServletResponse`) and the Spring MVC world (`@RequestMapping`, `@PathVariable`, `@RequestBody`, etc.).

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  DispatcherServlet — The Single Entry Point:                                     │
│                                                                                  │
│  Without DispatcherServlet (raw Servlets):                                       │
│  ┌──────────────────────────────────────────┐                                   │
│  │  Servlet Container                        │                                   │
│  │  ┌──────────────────────────────────────┐ │                                   │
│  │  │ /users   → UserServlet.doGet()       │ │  ← one Servlet PER URL pattern  │
│  │  │ /orders  → OrderServlet.doGet()      │ │                                   │
│  │  │ /products→ ProductServlet.doGet()    │ │                                   │
│  │  │ /login   → LoginServlet.doPost()     │ │                                   │
│  │  └──────────────────────────────────────┘ │                                   │
│  └──────────────────────────────────────────┘                                   │
│                                                                                  │
│  With DispatcherServlet (Spring MVC):                                            │
│  ┌──────────────────────────────────────────┐                                   │
│  │  Servlet Container                        │                                   │
│  │  ┌──────────────────────────────────────┐ │                                   │
│  │  │ /*  → DispatcherServlet              │ │  ← ONE Servlet for ALL URLs      │
│  │  │        ├→ UserController.getById()   │ │                                   │
│  │  │        ├→ OrderController.create()   │ │  ← dispatches to @Controller     │
│  │  │        ├→ ProductController.list()   │ │    methods via HandlerMapping     │
│  │  │        └→ AuthController.login()     │ │                                   │
│  │  └──────────────────────────────────────┘ │                                   │
│  └──────────────────────────────────────────┘                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### DispatcherServlet Internals — Key Methods

The `DispatcherServlet` extends `HttpServlet` and overrides the `service()` method. Internally, the main dispatch logic lives in `doDispatch()`:

```java
// Simplified DispatcherServlet source code — what actually runs on each request
public class DispatcherServlet extends FrameworkServlet {

    // These are initialized at startup from the Spring ApplicationContext
    private List<HandlerMapping>        handlerMappings;
    private List<HandlerAdapter>        handlerAdapters;
    private List<HandlerExceptionResolver> exceptionResolvers;
    private List<ViewResolver>          viewResolvers;

    @Override
    protected void initStrategies(ApplicationContext context) {
        // Called once at startup — loads all strategy beans from IoC container
        initHandlerMappings(context);       // find all HandlerMapping beans
        initHandlerAdapters(context);       // find all HandlerAdapter beans
        initHandlerExceptionResolvers(context);
        initViewResolvers(context);
        initMultipartResolver(context);     // file upload handling
        initLocaleResolver(context);        // i18n
        initThemeResolver(context);
    }

    // ── THE CORE METHOD — called on every HTTP request ────────────────────────
    protected void doDispatch(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {

        HandlerExecutionChain mappedHandler = null;
        ModelAndView mv = null;

        try {
            // ── STEP 1: Find the handler (controller method) for this request ──
            mappedHandler = getHandler(request);
            //   → iterates through handlerMappings
            //   → RequestMappingHandlerMapping matches URL + HTTP method
            //   → returns HandlerExecutionChain (handler + interceptors)

            if (mappedHandler == null) {
                noHandlerFound(request, response);   // → 404
                return;
            }

            // ── STEP 2: Find the adapter that can invoke this handler ──────────
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
            //   → RequestMappingHandlerAdapter handles @RequestMapping methods

            // ── STEP 3: Execute pre-handle interceptors ────────────────────────
            if (!mappedHandler.applyPreHandle(request, response)) {
                return;   // interceptor said stop (e.g., auth check failed)
            }

            // ── STEP 4: Actually invoke the controller method ──────────────────
            mv = ha.handle(request, response, mappedHandler.getHandler());
            //   RequestMappingHandlerAdapter internally:
            //   1. Resolves method arguments:
            //      @PathVariable → extract from URL path
            //      @RequestParam → extract from query string
            //      @RequestBody  → read request body, deserialise via
            //                       HttpMessageConverter (Jackson for JSON)
            //      @RequestHeader → extract from HTTP headers
            //   2. Calls the actual controller method via reflection:
            //      Method.invoke(controllerInstance, resolvedArgs)
            //   3. Handles return value:
            //      @ResponseBody → serialise to JSON via Jackson
            //      String        → treat as view name
            //      ModelAndView  → use as-is

            // ── STEP 5: Execute post-handle interceptors ───────────────────────
            mappedHandler.applyPostHandle(request, response, mv);

        } catch (Exception ex) {
            // ── STEP 6: Exception handling ─────────────────────────────────────
            mv = processHandlerException(request, response,
                                         mappedHandler, ex);
            //   → @ControllerAdvice / @ExceptionHandler methods invoked here
        }

        // ── STEP 7: Render view (if ModelAndView returned) ─────────────────────
        if (mv != null && !mv.wasCleared()) {
            render(mv, request, response);
            //   → ViewResolver resolves view name to View object
            //   → View.render() writes HTML to response
        }

        // ── STEP 8: After-completion interceptors ──────────────────────────────
        mappedHandler.triggerAfterCompletion(request, response, null);
    }

    private HandlerExecutionChain getHandler(HttpServletRequest request)
            throws Exception {
        // Iterate through all registered HandlerMappings
        for (HandlerMapping mapping : this.handlerMappings) {
            HandlerExecutionChain handler = mapping.getHandler(request);
            if (handler != null) {
                return handler;   // first match wins
            }
        }
        return null;   // no handler found → 404
    }
}
```

##### How Spring Boot Auto-Configures DispatcherServlet

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot Auto-Configuration of DispatcherServlet:                            │
│                                                                                  │
│  When spring-boot-starter-web is on the classpath:                               │
│                                                                                  │
│  1. DispatcherServletAutoConfiguration activates                                │
│       @ConditionalOnClass(DispatcherServlet.class) ✓                            │
│       @ConditionalOnWebApplication ✓                                            │
│                                                                                  │
│  2. Creates DispatcherServlet bean                                               │
│       @Bean("dispatcherServlet")                                                │
│       public DispatcherServlet dispatcherServlet() {                            │
│           return new DispatcherServlet();                                        │
│       }                                                                          │
│                                                                                  │
│  3. DispatcherServletRegistrationBean registers it with Tomcat                  │
│       → URL mapping: "/" (handles ALL requests)                                 │
│       → Load-on-startup: -1 (created when first request arrives                │
│         or at app startup if set to ≥ 0)                                        │
│                                                                                  │
│  4. DispatcherServlet.onRefresh(ApplicationContext) called                       │
│       → initStrategies() loads HandlerMappings, HandlerAdapters, etc.           │
│       → from the Spring IoC container (ApplicationContext)                      │
│                                                                                  │
│  You never write this code — Spring Boot does it all automatically.             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### DispatcherServlet Inheritance Hierarchy

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  DispatcherServlet Class Hierarchy:                                               │
│                                                                                  │
│  jakarta.servlet.Servlet               (interface — defines service())           │
│      │                                                                           │
│      └── jakarta.servlet.GenericServlet (abstract — lifecycle: init/destroy)     │
│              │                                                                   │
│              └── jakarta.servlet.http.HttpServlet                                │
│                      │  → service() dispatches to doGet/doPost/doPut/doDelete   │
│                      │                                                           │
│                      └── FrameworkServlet (Spring)                               │
│                              │  → Overrides service() to call processRequest()  │
│                              │  → processRequest() calls doService()            │
│                              │  → Publishes ServletRequestHandledEvent          │
│                              │  → Holds reference to WebApplicationContext      │
│                              │                                                   │
│                              └── DispatcherServlet (Spring MVC)                 │
│                                      → doService() calls doDispatch()           │
│                                      → doDispatch() is the main dispatch loop   │
│                                      → Delegates to HandlerMapping,             │
│                                        HandlerAdapter, ViewResolver, etc.       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### What is HandlerMapping?

A **HandlerMapping** is a Spring MVC strategy interface that maps an incoming HTTP request to a **handler** (typically a `@Controller` method). When `DispatcherServlet` receives a request, it asks each registered `HandlerMapping`: *"Do you have a handler for this request?"* The first one that returns a match wins.

```java
// HandlerMapping interface — the contract
public interface HandlerMapping {

    /**
     * Given an HTTP request, return the handler (controller method)
     * and any interceptors that should be applied.
     * Returns null if this HandlerMapping has no handler for the request.
     */
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}
```

##### Types of HandlerMapping

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  HandlerMapping Implementations (ordered by priority):                            │
│                                                                                  │
│  ┌───────────────────────────────────┬───────────────────────────────────────┐   │
│  │ HandlerMapping                    │ What it maps                          │   │
│  ├───────────────────────────────────┼───────────────────────────────────────┤   │
│  │ RequestMappingHandlerMapping      │ @RequestMapping / @GetMapping /      │   │
│  │ (most commonly used)              │ @PostMapping annotations on          │   │
│  │                                   │ @Controller and @RestController      │   │
│  │                                   │ methods. This is what you use 99%    │   │
│  │                                   │ of the time.                         │   │
│  ├───────────────────────────────────┼───────────────────────────────────────┤   │
│  │ BeanNameUrlHandlerMapping         │ Maps URLs to bean names starting     │   │
│  │                                   │ with "/". Legacy — rarely used.      │   │
│  ├───────────────────────────────────┼───────────────────────────────────────┤   │
│  │ SimpleUrlHandlerMapping           │ Explicit URL-to-handler map defined  │   │
│  │                                   │ in configuration. Used for static    │   │
│  │                                   │ resources.                           │   │
│  ├───────────────────────────────────┼───────────────────────────────────────┤   │
│  │ RouterFunctionMapping             │ Maps functional endpoints defined    │   │
│  │                                   │ with RouterFunction (WebFlux style   │   │
│  │                                   │ available in MVC since Spring 5.2).  │   │
│  └───────────────────────────────────┴───────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### How RequestMappingHandlerMapping Works Internally

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  RequestMappingHandlerMapping — Startup Phase:                                   │
│                                                                                  │
│  During application startup (afterPropertiesSet / InitializingBean):             │
│                                                                                  │
│  1. Scan all beans in the ApplicationContext                                     │
│  2. For each bean annotated with @Controller or @RequestMapping at class level:  │
│     3. Scan all methods for @RequestMapping / @GetMapping / @PostMapping ...     │
│     4. For each annotated method, create a RequestMappingInfo object:            │
│        ┌──────────────────────────────────────────────────────────────┐          │
│        │ RequestMappingInfo:                                          │          │
│        │   patterns:  ["/api/v1/users/{id}"]                         │          │
│        │   methods:   [GET]                                           │          │
│        │   params:    []                                              │          │
│        │   headers:   []                                              │          │
│        │   consumes:  [application/json]                             │          │
│        │   produces:  [application/json]                             │          │
│        └──────────────────────────────────────────────────────────────┘          │
│     5. Register mapping: RequestMappingInfo → HandlerMethod                     │
│        (HandlerMethod = controller bean + Method reference)                     │
│                                                                                  │
│  Result: an in-memory registry (HashMap) of all URL patterns → handler methods  │
│                                                                                  │
│  Registry contents (example):                                                    │
│  ┌──────────────────────────────────────────┬────────────────────────────────┐   │
│  │ RequestMappingInfo                       │ HandlerMethod                  │   │
│  ├──────────────────────────────────────────┼────────────────────────────────┤   │
│  │ GET /api/v1/users                        │ UserController#list()         │   │
│  │ GET /api/v1/users/{id}                   │ UserController#getById(Long)  │   │
│  │ POST /api/v1/users                       │ UserController#create(Req)    │   │
│  │ PUT /api/v1/users/{id}                   │ UserController#update(L, Req) │   │
│  │ DELETE /api/v1/users/{id}                │ UserController#delete(Long)   │   │
│  │ GET /api/v1/orders                       │ OrderController#list()        │   │
│  │ POST /api/v1/orders                      │ OrderController#create(Req)   │   │
│  └──────────────────────────────────────────┴────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  RequestMappingHandlerMapping — Runtime Phase (on each request):                 │
│                                                                                  │
│  Input: HttpServletRequest for "GET /api/v1/users/42"                           │
│                                                                                  │
│  1. Extract request URL: "/api/v1/users/42"                                     │
│  2. Extract HTTP method: GET                                                     │
│  3. Look up in the registry:                                                     │
│       → Match URL pattern "/api/v1/users/{id}" using PathPattern matching       │
│       → Match HTTP method GET                                                   │
│       → Match content type, accept headers if specified                         │
│  4. Extract path variables: {id} = "42"                                         │
│  5. Store in request attributes:                                                │
│       request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerMethod)      │
│       request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, {id=42})           │
│  6. Wrap handler + interceptors into HandlerExecutionChain                      │
│  7. Return to DispatcherServlet                                                 │
│                                                                                  │
│  If NO match found:                                                              │
│    → Return null                                                                │
│    → DispatcherServlet tries next HandlerMapping                                │
│    → If all return null → 404 Not Found                                         │
│                                                                                  │
│  If MULTIPLE matches found:                                                      │
│    → Most specific pattern wins                                                 │
│    → "/api/v1/users/42" beats "/api/v1/users/{id}" beats "/api/v1/**"          │
│    → If ambiguous → IllegalStateException ("Ambiguous handler methods")         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### HandlerExecutionChain — Handler + Interceptors

```java
// What getHandler() returns — not just the handler, but also interceptors
public class HandlerExecutionChain {
    private final Object handler;                    // the controller method
    private final List<HandlerInterceptor> interceptors;  // interceptors to apply

    // DispatcherServlet calls these in order:
    boolean applyPreHandle(request, response);       // all interceptors' preHandle()
    void applyPostHandle(request, response, mv);     // all interceptors' postHandle()
    void triggerAfterCompletion(request, response, ex); // all interceptors' afterCompletion()
}
```

##### HandlerAdapter — The Bridge Between DispatcherServlet and Controller Methods

After `HandlerMapping` finds the handler, the `DispatcherServlet` needs a `HandlerAdapter` to actually invoke it. The adapter knows how to resolve method arguments and handle return values:

```java
public interface HandlerAdapter {

    // Can this adapter handle the given handler?
    boolean supports(Object handler);

    // Invoke the handler and return a ModelAndView
    ModelAndView handle(HttpServletRequest request,
                        HttpServletResponse response,
                        Object handler) throws Exception;
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  RequestMappingHandlerAdapter — What Happens Inside handle():                    │
│                                                                                  │
│  handle(request, response, handlerMethod)                                        │
│      │                                                                           │
│      ▼                                                                           │
│  1. Resolve Method Arguments (ArgumentResolvers):                                │
│     ┌─────────────────────────────────────────────────────────────────────┐      │
│     │ @PathVariable Long id                                               │      │
│     │   → PathVariableMethodArgumentResolver                             │      │
│     │   → reads from URI_TEMPLATE_VARIABLES_ATTRIBUTE → "42" → 42L      │      │
│     │                                                                     │      │
│     │ @RequestBody UserRequest request                                    │      │
│     │   → RequestResponseBodyMethodProcessor                             │      │
│     │   → reads request body bytes                                       │      │
│     │   → selects HttpMessageConverter (MappingJackson2HttpMessageConverter)│     │
│     │   → deserialises JSON → UserRequest object                         │      │
│     │   → runs @Valid validation (if present) via Hibernate Validator    │      │
│     │                                                                     │      │
│     │ @RequestParam String name                                           │      │
│     │   → RequestParamMethodArgumentResolver                             │      │
│     │   → reads from query string: ?name=Alice → "Alice"                │      │
│     │                                                                     │      │
│     │ @RequestHeader("Authorization") String auth                         │      │
│     │   → RequestHeaderMethodArgumentResolver                            │      │
│     │   → reads from HTTP header                                         │      │
│     └─────────────────────────────────────────────────────────────────────┘      │
│      │                                                                           │
│      ▼                                                                           │
│  2. Invoke Controller Method via Reflection:                                     │
│     Method.invoke(controllerBean, resolvedArgs)                                 │
│     → UserController.getById(42L) called                                        │
│      │                                                                           │
│      ▼                                                                           │
│  3. Handle Return Value (ReturnValueHandlers):                                   │
│     ┌─────────────────────────────────────────────────────────────────────┐      │
│     │ ResponseEntity<UserResponse>                                        │      │
│     │   → HttpEntityMethodProcessor                                      │      │
│     │   → extracts status code, headers, body                           │      │
│     │   → selects HttpMessageConverter for body serialisation            │      │
│     │   → MappingJackson2HttpMessageConverter.write(userResponse)        │      │
│     │   → Jackson ObjectMapper serialises UserResponse → JSON bytes     │      │
│     │   → writes to HttpServletResponse output stream                   │      │
│     │   → sets Content-Type: application/json                           │      │
│     └─────────────────────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### What is the IoC Container?

**IoC (Inversion of Control) Container** — also called the **Spring Container** or **ApplicationContext** — is the core of the Spring Framework. It is responsible for **creating**, **configuring**, **wiring**, and **managing the lifecycle** of all objects (called **beans**) in your application.

**Inversion of Control** means that objects do **not** create their own dependencies. Instead, the container **injects** dependencies into objects. This is also called **Dependency Injection (DI)**.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Without IoC (traditional code):                                                 │
│                                                                                  │
│  public class OrderService {                                                     │
│      // The class creates its own dependencies — tight coupling                 │
│      private UserRepository userRepo = new UserRepositoryImpl();                │
│      private EmailService emailService = new EmailServiceImpl("smtp.gmail.com");│
│      private PaymentGateway gateway = new StripePaymentGateway("sk_key");       │
│                                                                                  │
│      // Problems:                                                                │
│      // 1. Cannot swap implementations without code change                      │
│      // 2. Cannot unit test — stuck with real EmailService, real Stripe         │
│      // 3. Every class manages its own dependency graph                         │
│      // 4. Circular dependencies cause StackOverflow                            │
│  }                                                                               │
│                                                                                  │
│  With IoC (Spring):                                                              │
│                                                                                  │
│  @Service                                                                        │
│  public class OrderService {                                                     │
│      // Dependencies INJECTED by the container — loose coupling                 │
│      private final UserRepository userRepo;          // interface               │
│      private final EmailService emailService;        // interface               │
│      private final PaymentGateway gateway;           // interface               │
│                                                                                  │
│      public OrderService(UserRepository userRepo,                               │
│                          EmailService emailService,                              │
│                          PaymentGateway gateway) {                               │
│          this.userRepo = userRepo;                                              │
│          this.emailService = emailService;                                      │
│          this.gateway = gateway;                                                │
│      }                                                                           │
│      // Container provides the right implementations at runtime                 │
│      // In tests: pass mocks → new OrderService(mockRepo, mockEmail, mockGw)   │
│  }                                                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### IoC Container Hierarchy

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring IoC Container — Interface Hierarchy:                                     │
│                                                                                  │
│  BeanFactory (root interface)                                                    │
│      │  → Basic container: creates beans, resolves dependencies                 │
│      │  → Lazy initialization by default                                        │
│      │  → Methods: getBean(name), getBean(class), containsBean(name)           │
│      │                                                                           │
│      └── ApplicationContext (extends BeanFactory)                               │
│              │  → Full-featured container (this is what Spring Boot uses)       │
│              │  → Eager initialization by default (beans created at startup)    │
│              │  → Additional features over BeanFactory:                         │
│              │     • Event publishing (ApplicationEvent)                        │
│              │     • Message source (i18n)                                      │
│              │     • Environment abstraction (profiles, properties)             │
│              │     • Resource loading (classpath:, file:, http:)                │
│              │     • AOP integration                                            │
│              │     • @PostConstruct / @PreDestroy lifecycle hooks               │
│              │                                                                   │
│              ├── AnnotationConfigApplicationContext                              │
│              │     → For standalone (non-web) Spring apps                       │
│              │     → Reads @Configuration + @ComponentScan                      │
│              │                                                                   │
│              ├── AnnotationConfigServletWebServerApplicationContext              │
│              │     → For Spring Boot web apps (Servlet-based)                   │
│              │     → This is what SpringApplication.run() creates               │
│              │     → Starts embedded Tomcat/Jetty/Undertow                      │
│              │                                                                   │
│              └── AnnotationConfigReactiveWebServerApplicationContext            │
│                    → For Spring WebFlux (reactive) apps                          │
│                    → Starts embedded Netty                                       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### How the IoC Container Creates and Manages Beans

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  IoC Container — Bean Lifecycle (complete):                                      │
│                                                                                  │
│  SpringApplication.run(MyApplication.class, args)                               │
│      │                                                                           │
│      ▼                                                                           │
│  1. Create ApplicationContext                                                    │
│      → AnnotationConfigServletWebServerApplicationContext                       │
│      │                                                                           │
│      ▼                                                                           │
│  2. Scan for Bean Definitions                                                    │
│      → @ComponentScan scans base package + sub-packages                         │
│      → Finds classes annotated with:                                            │
│         @Component, @Service, @Repository, @Controller, @RestController,        │
│         @Configuration, @Bean methods                                            │
│      → Creates BeanDefinition objects (metadata, NOT instances yet):            │
│         ┌─────────────────────────────────────────────────────┐                 │
│         │ BeanDefinition for "userService":                    │                 │
│         │   beanClass: UserServiceImpl.class                   │                 │
│         │   scope: singleton (default)                         │                 │
│         │   lazyInit: false                                    │                 │
│         │   dependsOn: [userRepository, userMapper]           │                 │
│         │   initMethod: null                                   │                 │
│         │   destroyMethod: null                                │                 │
│         └─────────────────────────────────────────────────────┘                 │
│      │                                                                           │
│      ▼                                                                           │
│  3. BeanFactoryPostProcessor Phase                                               │
│      → Modify bean definitions BEFORE any beans are created                     │
│      → PropertySourcesPlaceholderConfigurer resolves ${...} placeholders        │
│      → ConfigurationClassPostProcessor processes @Configuration classes         │
│      │                                                                           │
│      ▼                                                                           │
│  4. Instantiate Beans (in dependency order)                                      │
│      → Container resolves the dependency graph (topological sort)               │
│      → Creates beans in order: dependencies first, dependents second            │
│                                                                                  │
│      For each bean:                                                              │
│      ┌──────────────────────────────────────────────────────────────────┐        │
│      │ a) Constructor call                                              │        │
│      │    → new UserServiceImpl(userRepository, userMapper)             │        │
│      │    → Dependencies are resolved and injected via constructor     │        │
│      │                                                                  │        │
│      │ b) Populate properties                                           │        │
│      │    → @Autowired fields set (if field injection used)            │        │
│      │    → @Value("${...}") properties resolved and set              │        │
│      │                                                                  │        │
│      │ c) BeanPostProcessor.postProcessBeforeInitialization()          │        │
│      │    → @PostConstruct method called                               │        │
│      │    → CommonAnnotationBeanPostProcessor handles this             │        │
│      │                                                                  │        │
│      │ d) InitializingBean.afterPropertiesSet()                        │        │
│      │    → If bean implements InitializingBean                        │        │
│      │                                                                  │        │
│      │ e) Custom init-method                                            │        │
│      │    → @Bean(initMethod = "init") if specified                    │        │
│      │                                                                  │        │
│      │ f) BeanPostProcessor.postProcessAfterInitialization()           │        │
│      │    → AOP proxies created here                                   │        │
│      │    → @Transactional → CGLIB proxy wrapping the bean             │        │
│      │    → @Cacheable → proxy with cache logic                       │        │
│      │    → @Async → proxy with async execution logic                 │        │
│      └──────────────────────────────────────────────────────────────────┘        │
│      │                                                                           │
│      ▼                                                                           │
│  5. Beans Are Ready — Application Context Refreshed                             │
│      → ApplicationReadyEvent published                                          │
│      → DispatcherServlet initialized with HandlerMappings from context          │
│      → Embedded Tomcat started                                                  │
│      → Application is live                                                      │
│      │                                                                           │
│      ▼                                                                           │
│  6. Runtime — Beans serve requests                                               │
│      → Singleton beans shared across all requests                               │
│      → Prototype beans: new instance created per getBean() call                 │
│      │                                                                           │
│      ▼                                                                           │
│  7. Shutdown                                                                     │
│      → @PreDestroy methods called                                               │
│      → DisposableBean.destroy() called                                          │
│      → Custom destroy-method called                                             │
│      → ApplicationContext closed                                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Bean Scopes

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Bean Scopes:                                                                    │
│                                                                                  │
│  ┌─────────────┬─────────────────────────────────────────────────────────────┐  │
│  │ Scope       │ Description                                                 │  │
│  ├─────────────┼─────────────────────────────────────────────────────────────┤  │
│  │ singleton   │ ONE instance per ApplicationContext (DEFAULT).              │  │
│  │ (default)   │ All injections of this bean point to the SAME object.      │  │
│  │             │ Created at startup (eager). Thread-safe design required.   │  │
│  ├─────────────┼─────────────────────────────────────────────────────────────┤  │
│  │ prototype   │ NEW instance every time getBean() is called or injected.   │  │
│  │             │ Container does NOT manage lifecycle after creation —        │  │
│  │             │ no @PreDestroy called.                                      │  │
│  ├─────────────┼─────────────────────────────────────────────────────────────┤  │
│  │ request     │ ONE instance per HTTP request (web apps only).             │  │
│  │             │ Destroyed when the request completes.                      │  │
│  ├─────────────┼─────────────────────────────────────────────────────────────┤  │
│  │ session     │ ONE instance per HTTP session (web apps only).             │  │
│  │             │ Destroyed when the session expires.                        │  │
│  ├─────────────┼─────────────────────────────────────────────────────────────┤  │
│  │ application │ ONE instance per ServletContext (web apps only).            │  │
│  │             │ Similar to singleton but scoped to the web layer.          │  │
│  └─────────────┴─────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Dependency Injection Types

```java
// ── 1. Constructor Injection (RECOMMENDED) ──────────────────────────────────────
@Service
public class OrderService {
    private final UserRepository userRepo;
    private final PaymentGateway paymentGateway;

    // Spring sees this constructor and injects matching beans
    // @Autowired is optional when there's only one constructor (Spring 4.3+)
    public OrderService(UserRepository userRepo, PaymentGateway paymentGateway) {
        this.userRepo = userRepo;
        this.paymentGateway = paymentGateway;
    }
}

// ── 2. Setter Injection ─────────────────────────────────────────────────────────
@Service
public class NotificationService {
    private EmailService emailService;

    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
    // Used for optional dependencies — bean can function without it
}

// ── 3. Field Injection (NOT RECOMMENDED — hard to test) ─────────────────────────
@Service
public class ReportService {
    @Autowired
    private DataSource dataSource;    // injected directly into the field
    // Cannot set this in unit tests without reflection or a Spring context
}
```

##### How the IoC Container Resolves Dependencies

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dependency Resolution — What the Container Does:                                │
│                                                                                  │
│  Given:                                                                          │
│    @Service                                                                      │
│    class UserServiceImpl implements UserService {                                │
│        UserServiceImpl(UserRepository repo, UserMapper mapper) { ... }          │
│    }                                                                             │
│                                                                                  │
│  Container resolution:                                                           │
│  1. "I need to create UserServiceImpl"                                          │
│  2. "Its constructor requires UserRepository and UserMapper"                    │
│  3. "Let me find a bean of type UserRepository..."                              │
│       → Found: userRepository (JPA proxy created by Spring Data)               │
│  4. "Let me find a bean of type UserMapper..."                                  │
│       → Found: userMapper (component-scanned @Component)                       │
│  5. "Both dependencies resolved. Calling constructor:"                          │
│       new UserServiceImpl(userRepository, userMapper)                           │
│  6. "Bean created. Running BeanPostProcessors..."                               │
│       → @Transactional detected → wrap in CGLIB proxy                          │
│  7. "Registering in singleton cache as 'userServiceImpl'"                       │
│                                                                                  │
│  If Step 3 or 4 fails:                                                           │
│  → NoSuchBeanDefinitionException:                                               │
│    "No qualifying bean of type 'UserMapper' available"                          │
│                                                                                  │
│  If multiple beans match:                                                        │
│  → NoUniqueBeanDefinitionException:                                             │
│    "Expected single matching bean but found 2: impl1, impl2"                   │
│  → Fix with: @Primary on preferred bean, or @Qualifier("impl1") at injection  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Complete Request-Response Flow — All Components Working Together

This is the end-to-end flow showing the **Servlet Container**, **DispatcherServlet**, **HandlerMapping**, **IoC Container**, and the **exact methods** called on each bean, for a `GET /api/v1/users/42` request:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│   CLIENT                                                                         │
│   GET /api/v1/users/42                                                           │
│   Headers: Accept: application/json                                              │
│            Authorization: Bearer eyJhbGci...                                    │
│      │                                                                           │
│      │  TCP connection on port 8080                                              │
│      ▼                                                                           │
│  ╔══════════════════════════════════════════════════════════════════════════════╗ │
│  ║  SERVLET CONTAINER (Embedded Tomcat)                                       ║ │
│  ║                                                                            ║ │
│  ║  1. Tomcat NIO Connector                                                   ║ │
│  ║     • Acceptor thread accepts the TCP connection (ServerSocket.accept())   ║ │
│  ║     • Reads raw HTTP bytes from the socket                                ║ │
│  ║     • Parses HTTP request line: "GET /api/v1/users/42 HTTP/1.1"          ║ │
│  ║     • Parses headers into key-value map                                   ║ │
│  ║     • Creates:                                                             ║ │
│  ║       - org.apache.catalina.connector.Request (→ HttpServletRequest)      ║ │
│  ║       - org.apache.catalina.connector.Response (→ HttpServletResponse)    ║ │
│  ║                                                                            ║ │
│  ║  2. Tomcat assigns a thread from the NIO thread pool (default: 200)       ║ │
│  ║     → Thread: "http-nio-8080-exec-7"                                      ║ │
│  ║                                                                            ║ │
│  ║  3. Tomcat Pipeline → Valve chain → ApplicationFilterChain                ║ │
│  ║     ┌──────────────────────────────────────────────────────────────────┐   ║ │
│  ║     │  FILTER CHAIN (executed in order):                               │   ║ │
│  ║     │                                                                  │   ║ │
│  ║     │  CharacterEncodingFilter.doFilter(req, res, chain)              │   ║ │
│  ║     │    → req.setCharacterEncoding("UTF-8")                          │   ║ │
│  ║     │    → chain.doFilter(req, res)                                   │   ║ │
│  ║     │         │                                                        │   ║ │
│  ║     │         ▼                                                        │   ║ │
│  ║     │  CorsFilter.doFilter(req, res, chain)                           │   ║ │
│  ║     │    → checks Origin header, adds Access-Control-* headers        │   ║ │
│  ║     │    → chain.doFilter(req, res)                                   │   ║ │
│  ║     │         │                                                        │   ║ │
│  ║     │         ▼                                                        │   ║ │
│  ║     │  SecurityFilterChain (Spring Security — 15+ filters):           │   ║ │
│  ║     │    SecurityContextHolderFilter.doFilter()                        │   ║ │
│  ║     │      → creates empty SecurityContext                             │   ║ │
│  ║     │    JwtAuthenticationFilter.doFilter() (your custom filter)       │   ║ │
│  ║     │      → extracts "Bearer eyJhbGci..." from Authorization header │   ║ │
│  ║     │      → validates JWT signature and expiry                       │   ║ │
│  ║     │      → creates UsernamePasswordAuthenticationToken              │   ║ │
│  ║     │      → SecurityContextHolder.getContext()                        │   ║ │
│  ║     │            .setAuthentication(authToken)                         │   ║ │
│  ║     │    AuthorizationFilter.doFilter()                                │   ║ │
│  ║     │      → checks if authenticated user has required role           │   ║ │
│  ║     │    → chain.doFilter(req, res)                                   │   ║ │
│  ║     │         │                                                        │   ║ │
│  ║     └─────────┼────────────────────────────────────────────────────────┘   ║ │
│  ║               │                                                            ║ │
│  ║               ▼                                                            ║ │
│  ║  4. Request reaches the Servlet registered at "/"                          ║ │
│  ║     → DispatcherServlet (the ONLY Servlet in a Spring Boot app)           ║ │
│  ║     → Tomcat calls: dispatcherServlet.service(request, response)          ║ │
│  ║                                                                            ║ │
│  ╚═══════════════╪═══════════════════════════════════════════════════════════╝ │
│                  │                                                               │
│                  ▼                                                               │
│  ╔══════════════════════════════════════════════════════════════════════════════╗ │
│  ║  DISPATCHER SERVLET                                                        ║ │
│  ║                                                                            ║ │
│  ║  FrameworkServlet.service(req, res)                                        ║ │
│  ║    → FrameworkServlet.processRequest(req, res)                            ║ │
│  ║       → DispatcherServlet.doService(req, res)                             ║ │
│  ║          → DispatcherServlet.doDispatch(req, res)   ← main entry point   ║ │
│  ║                                                                            ║ │
│  ║  ┌─── doDispatch() Step-by-Step: ──────────────────────────────────────┐  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  STEP 1: Find Handler via HandlerMapping                            │  ║ │
│  ║  │  ────────────────────────────────────────                            │  ║ │
│  ║  │  HandlerExecutionChain mappedHandler = getHandler(request);          │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  → Iterates through registered HandlerMappings:                     │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  ┌──────────────────────────────────────────────────────────────┐   │  ║ │
│  ║  │  │  RequestMappingHandlerMapping.getHandler(request)            │   │  ║ │
│  ║  │  │                                                              │   │  ║ │
│  ║  │  │  • Extract URL: "/api/v1/users/42"                          │   │  ║ │
│  ║  │  │  • Extract method: GET                                       │   │  ║ │
│  ║  │  │  • Search mapping registry:                                  │   │  ║ │
│  ║  │  │    "GET /api/v1/users/{id}" matches!                        │   │  ║ │
│  ║  │  │  • Resolve: handler = UserController#getById(Long)          │   │  ║ │
│  ║  │  │  • Extract path variables: {id} → "42"                     │   │  ║ │
│  ║  │  │  • Collect interceptors: [LoggingInterceptor, ...]          │   │  ║ │
│  ║  │  │  • Return HandlerExecutionChain:                            │   │  ║ │
│  ║  │  │      handler: HandlerMethod[UserController#getById]         │   │  ║ │
│  ║  │  │      interceptors: [LoggingInterceptor]                     │   │  ║ │
│  ║  │  └──────────────────────────────────────────────────────────────┘   │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  STEP 2: Find HandlerAdapter                                        │  ║ │
│  ║  │  ────────────────────────────────                                    │  ║ │
│  ║  │  HandlerAdapter ha = getHandlerAdapter(handler);                     │  ║ │
│  ║  │  → RequestMappingHandlerAdapter (handles @RequestMapping methods)   │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  STEP 3: Pre-Handle Interceptors                                    │  ║ │
│  ║  │  ────────────────────────────────                                    │  ║ │
│  ║  │  mappedHandler.applyPreHandle(request, response)                     │  ║ │
│  ║  │  → LoggingInterceptor.preHandle(req, res, handler)                  │  ║ │
│  ║  │    → MDC.put("method", "GET")                                       │  ║ │
│  ║  │    → MDC.put("path", "/api/v1/users/42")                           │  ║ │
│  ║  │    → log.info("START GET /api/v1/users/42")                        │  ║ │
│  ║  │    → return true (continue processing)                              │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  STEP 4: Invoke Handler (Controller Method)                         │  ║ │
│  ║  │  ──────────────────────────────────────────                          │  ║ │
│  ║  │  mv = ha.handle(request, response, handler)                          │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  RequestMappingHandlerAdapter internally:                            │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  4a. Resolve arguments:                                              │  ║ │
│  ║  │      @PathVariable Long id                                           │  ║ │
│  ║  │        → PathVariableMethodArgumentResolver.resolveArgument()       │  ║ │
│  ║  │        → reads {id}="42" from request attributes                   │  ║ │
│  ║  │        → converts String "42" → Long 42L (ConversionService)       │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  │  4b. Invoke controller method via reflection:                        │  ║ │
│  ║  │      Method.invoke(userControllerBean, 42L)                          │  ║ │
│  ║  │                                                                      │  ║ │
│  ║  └──┼───────────────────────────────────────────────────────────────────┘  ║ │
│  ╚═════╪════════════════════════════════════════════════════════════════════════╝ │
│        │                                                                         │
│        ▼                                                                         │
│  ╔══════════════════════════════════════════════════════════════════════════════╗ │
│  ║  IOC CONTAINER (ApplicationContext) — Beans Serving the Request            ║ │
│  ║                                                                            ║ │
│  ║  ┌──────────────────────────────────────────────────────────────────────┐  ║ │
│  ║  │  UserController.getById(42L)                     [@RestController]  │  ║ │
│  ║  │  ────────────────────────────────────────────────────────────────── │  ║ │
│  ║  │                                                                     │  ║ │
│  ║  │  public ResponseEntity<UserResponse> getById(@PathVariable Long id)│  ║ │
│  ║  │  {                                                                  │  ║ │
│  ║  │      return ResponseEntity.ok(userService.findById(id));           │  ║ │
│  ║  │  }                                                                  │  ║ │
│  ║  │      │                                                              │  ║ │
│  ║  │      │ calls userService (injected via constructor by IoC container)│  ║ │
│  ║  │      ▼                                                              │  ║ │
│  ║  └──────┼──────────────────────────────────────────────────────────────┘  ║ │
│  ║         │                                                                  ║ │
│  ║  ┌──────▼──────────────────────────────────────────────────────────────┐  ║ │
│  ║  │  CGLIB Proxy for UserServiceImpl              (AOP / @Transactional)│  ║ │
│  ║  │  ─────────────────────────────────────────────────────────────────  │  ║ │
│  ║  │                                                                     │  ║ │
│  ║  │  The IoC container wrapped UserServiceImpl in a CGLIB proxy        │  ║ │
│  ║  │  because @Transactional(readOnly=true) is on the class.            │  ║ │
│  ║  │                                                                     │  ║ │
│  ║  │  proxy.findById(42L) →                                             │  ║ │
│  ║  │    TransactionInterceptor.invoke()                                 │  ║ │
│  ║  │      → PlatformTransactionManager.getTransaction()                │  ║ │
│  ║  │      → DataSource.getConnection()                                 │  ║ │
│  ║  │        → HikariPool.getConnection() (from connection pool)        │  ║ │
│  ║  │      → connection.setAutoCommit(false)                             │  ║ │
│  ║  │      → connection.setReadOnly(true)  (readOnly=true optimisation) │  ║ │
│  ║  │      → delegate to ACTUAL UserServiceImpl.findById(42L)           │  ║ │
│  ║  │      │                                                              │  ║ │
│  ║  │      ▼                                                              │  ║ │
│  ║  └──────┼──────────────────────────────────────────────────────────────┘  ║ │
│  ║         │                                                                  ║ │
│  ║  ┌──────▼──────────────────────────────────────────────────────────────┐  ║ │
│  ║  │  UserServiceImpl.findById(42L)                         [@Service]  │  ║ │
│  ║  │  ─────────────────────────────────────────────────────────────────  │  ║ │
│  ║  │                                                                     │  ║ │
│  ║  │  public UserResponse findById(Long id) {                           │  ║ │
│  ║  │      log.debug("Fetching user id={}", id);                        │  ║ │
│  ║  │      User user = userRepository.findById(id)                      │  ║ │
│  ║  │          .orElseThrow(() ->                                        │  ║ │
│  ║  │              new UserNotFoundException("User not found: " + id)); │  ║ │
│  ║  │      return userMapper.toResponse(user);                          │  ║ │
│  ║  │  }                                                                  │  ║ │
│  ║  │      │                                                              │  ║ │
│  ║  │      │ calls userRepository (injected via constructor by IoC)      │  ║ │
│  ║  │      ▼                                                              │  ║ │
│  ║  └──────┼──────────────────────────────────────────────────────────────┘  ║ │
│  ║         │                                                                  ║ │
│  ║  ┌──────▼──────────────────────────────────────────────────────────────┐  ║ │
│  ║  │  Spring Data JPA Proxy for UserRepository        [@Repository]     │  ║ │
│  ║  │  ─────────────────────────────────────────────────────────────────  │  ║ │
│  ║  │                                                                     │  ║ │
│  ║  │  UserRepository is an INTERFACE:                                   │  ║ │
│  ║  │    public interface UserRepository extends JpaRepository<User, Long>│  ║ │
│  ║  │                                                                     │  ║ │
│  ║  │  IoC container creates a proxy (SimpleJpaRepository) at startup:   │  ║ │
│  ║  │    → JpaRepositoryFactory.getRepository(UserRepository.class)     │  ║ │
│  ║  │    → Creates a JDK dynamic proxy implementing UserRepository      │  ║ │
│  ║  │    → Backed by SimpleJpaRepository (default implementation)       │  ║ │
│  ║  │                                                                     │  ║ │
│  ║  │  proxy.findById(42L) →                                             │  ║ │
│  ║  │    SimpleJpaRepository.findById(42L)                               │  ║ │
│  ║  │      → EntityManager.find(User.class, 42L)                        │  ║ │
│  ║  │        → Hibernate Session.find(User.class, 42L)                  │  ║ │
│  ║  │          → First Level Cache (Persistence Context) check          │  ║ │
│  ║  │            → MISS (first access in this transaction)              │  ║ │
│  ║  │          → Generate SQL:                                           │  ║ │
│  ║  │            SELECT u.id, u.name, u.email, u.created_at             │  ║ │
│  ║  │            FROM users u WHERE u.id = ?                             │  ║ │
│  ║  │          → PreparedStatement.setLong(1, 42L)                      │  ║ │
│  ║  │          → PreparedStatement.executeQuery()                        │  ║ │
│  ║  │            → JDBC driver sends SQL to MySQL over TCP              │  ║ │
│  ║  │            → MySQL executes query, returns ResultSet              │  ║ │
│  ║  │          → Hibernate hydrates ResultSet → User entity object      │  ║ │
│  ║  │            → user.setId(42L)                                       │  ║ │
│  ║  │            → user.setName("Alice")                                 │  ║ │
│  ║  │            → user.setEmail("alice@example.com")                    │  ║ │
│  ║  │            → user.setCreatedAt(LocalDateTime.of(...))             │  ║ │
│  ║  │          → Store User in First Level Cache                        │  ║ │
│  ║  │      → return Optional.of(user)                                    │  ║ │
│  ║  │      │                                                              │  ║ │
│  ║  └──────┼──────────────────────────────────────────────────────────────┘  ║ │
│  ║         │                                                                  ║ │
│  ║         │  Back in UserServiceImpl.findById():                            ║ │
│  ║         │  Optional<User> is present → unwrap User entity                 ║ │
│  ║         │                                                                  ║ │
│  ║  ┌──────▼──────────────────────────────────────────────────────────────┐  ║ │
│  ║  │  UserMapper.toResponse(user)                       [@Component]    │  ║ │
│  ║  │  ─────────────────────────────────────────────────────────────────  │  ║ │
│  ║  │                                                                     │  ║ │
│  ║  │  public UserResponse toResponse(User user) {                       │  ║ │
│  ║  │      return new UserResponse(                                      │  ║ │
│  ║  │          user.getId(),        // 42L                               │  ║ │
│  ║  │          user.getName(),      // "Alice"                           │  ║ │
│  ║  │          user.getEmail(),     // "alice@example.com"               │  ║ │
│  ║  │          user.getCreatedAt()  // 2026-05-11T10:30:00              │  ║ │
│  ║  │      );                                                             │  ║ │
│  ║  │  }                                                                  │  ║ │
│  ║  │  → Returns UserResponse DTO                                        │  ║ │
│  ║  └──────┼──────────────────────────────────────────────────────────────┘  ║ │
│  ║         │                                                                  ║ │
│  ║         │  Back in CGLIB Proxy (TransactionInterceptor):                  ║ │
│  ║         │  → No exception thrown → commit transaction                     ║ │
│  ║         │    → connection.commit()                                        ║ │
│  ║         │    → connection.setReadOnly(false)                              ║ │
│  ║         │    → HikariPool.releaseConnection(connection)                   ║ │
│  ║         │  → Return UserResponse to controller                            ║ │
│  ║         │                                                                  ║ │
│  ║  ┌──────▼──────────────────────────────────────────────────────────────┐  ║ │
│  ║  │  Back in UserController.getById():                                  │  ║ │
│  ║  │  → ResponseEntity.ok(userResponse)                                 │  ║ │
│  ║  │  → Returns ResponseEntity<UserResponse> with status 200            │  ║ │
│  ║  └──────┼──────────────────────────────────────────────────────────────┘  ║ │
│  ║         │                                                                  ║ │
│  ╚═════════╪════════════════════════════════════════════════════════════════════╝ │
│            │                                                                     │
│            ▼                                                                     │
│  ╔══════════════════════════════════════════════════════════════════════════════╗ │
│  ║  BACK IN DISPATCHER SERVLET — doDispatch() continues:                      ║ │
│  ║                                                                            ║ │
│  ║  STEP 5: Post-Handle Interceptors                                          ║ │
│  ║  ─────────────────────────────────                                         ║ │
│  ║  mappedHandler.applyPostHandle(request, response, mv)                      ║ │
│  ║  → LoggingInterceptor.postHandle(req, res, handler, mv)                   ║ │
│  ║    → log.info("END GET /api/v1/users/42 — 200 — 15ms")                   ║ │
│  ║                                                                            ║ │
│  ║  STEP 6: Resolve Return Value                                              ║ │
│  ║  ────────────────────────────                                              ║ │
│  ║  @RestController → @ResponseBody implicit on all methods                   ║ │
│  ║  → HttpEntityMethodProcessor.handleReturnValue()                          ║ │
│  ║    → Extract status: 200 OK                                               ║ │
│  ║    → Extract body: UserResponse object                                    ║ │
│  ║    → Content negotiation:                                                 ║ │
│  ║      Client Accept: application/json                                      ║ │
│  ║      → Select MappingJackson2HttpMessageConverter                         ║ │
│  ║    → Jackson ObjectMapper.writeValue(outputStream, userResponse)          ║ │
│  ║      → Introspects UserResponse via reflection (or getter methods)        ║ │
│  ║      → Serialises to JSON:                                                ║ │
│  ║        {"id":42,"name":"Alice","email":"alice@example.com",               ║ │
│  ║         "createdAt":"2026-05-11T10:30:00"}                                ║ │
│  ║    → Set response headers:                                                ║ │
│  ║      Content-Type: application/json                                       ║ │
│  ║      Content-Length: 89                                                    ║ │
│  ║    → Write JSON bytes to HttpServletResponse.getOutputStream()            ║ │
│  ║                                                                            ║ │
│  ║  STEP 7: After-Completion Interceptors                                     ║ │
│  ║  ──────────────────────────────────                                        ║ │
│  ║  mappedHandler.triggerAfterCompletion(request, response, null)             ║ │
│  ║  → LoggingInterceptor.afterCompletion(req, res, handler, ex)              ║ │
│  ║    → MDC.clear()   // clean up thread-local logging context               ║ │
│  ║                                                                            ║ │
│  ╚══════════════════════════════════════════════════════════════════════════════╝ │
│            │                                                                     │
│            ▼                                                                     │
│  ╔══════════════════════════════════════════════════════════════════════════════╗ │
│  ║  SERVLET CONTAINER (Tomcat) — Response Phase:                              ║ │
│  ║                                                                            ║ │
│  ║  → Tomcat flushes HttpServletResponse to the socket                       ║ │
│  ║  → NIO Connector writes HTTP response bytes:                              ║ │
│  ║                                                                            ║ │
│  ║    HTTP/1.1 200 OK                                                         ║ │
│  ║    Content-Type: application/json                                          ║ │
│  ║    Content-Length: 89                                                       ║ │
│  ║    Date: Mon, 12 May 2026 10:30:00 GMT                                    ║ │
│  ║                                                                            ║ │
│  ║    {"id":42,"name":"Alice","email":"alice@example.com",                    ║ │
│  ║     "createdAt":"2026-05-11T10:30:00"}                                    ║ │
│  ║                                                                            ║ │
│  ║  → Thread "http-nio-8080-exec-7" returned to the pool                     ║ │
│  ║  → Connection kept alive (or closed based on Keep-Alive header)           ║ │
│  ║                                                                            ║ │
│  ╚══════════════════════════════════════════════════════════════════════════════╝ │
│            │                                                                     │
│            ▼                                                                     │
│   CLIENT receives:                                                               │
│   HTTP 200 OK                                                                    │
│   {"id":42,"name":"Alice","email":"alice@example.com",                          │
│    "createdAt":"2026-05-11T10:30:00"}                                           │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Summary — Every Method Called in Order

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Complete Method Call Trace — GET /api/v1/users/42:                               │
│                                                                                  │
│   #  Component                       Method Called                               │
│  ─── ──────────────────────────────  ────────────────────────────────────────── │
│   1  Tomcat NIO Connector            ServerSocket.accept()                       │
│   2  Tomcat NIO Connector            parse HTTP → HttpServletRequest             │
│   3  CharacterEncodingFilter         doFilter(req, res, chain)                   │
│   4  CorsFilter                      doFilter(req, res, chain)                   │
│   5  JwtAuthenticationFilter         doFilter(req, res, chain)                   │
│   6  SecurityContextHolder           getContext().setAuthentication(token)        │
│   7  AuthorizationFilter             doFilter(req, res, chain)                   │
│   8  DispatcherServlet               service(req, res)                           │
│   9  FrameworkServlet                 processRequest(req, res)                    │
│  10  DispatcherServlet               doService(req, res)                         │
│  11  DispatcherServlet               doDispatch(req, res)                        │
│  12  RequestMappingHandlerMapping    getHandler(req)                             │
│  13  RequestMappingHandlerMapping    lookupHandlerMethod(lookupPath, req)        │
│  14  DispatcherServlet               getHandlerAdapter(handler)                  │
│  15  LoggingInterceptor              preHandle(req, res, handler)                │
│  16  RequestMappingHandlerAdapter    handle(req, res, handler)                   │
│  17  PathVariableMethodArgResolver   resolveArgument() → 42L                    │
│  18  UserController                  getById(42L)                                │
│  19  TransactionInterceptor         invoke() → begin TX                         │
│  20  HikariDataSource               getConnection()                              │
│  21  UserServiceImpl                 findById(42L)                                │
│  22  SimpleJpaRepository (proxy)     findById(42L)                               │
│  23  EntityManager                   find(User.class, 42L)                       │
│  24  Hibernate Session               find(User.class, 42L)                       │
│  25  PreparedStatement               setLong(1, 42L)                             │
│  26  PreparedStatement               executeQuery()                              │
│  27  Hibernate                       hydrate ResultSet → User entity             │
│  28  UserMapper                      toResponse(user) → UserResponse             │
│  29  TransactionInterceptor         invoke() → commit TX                        │
│  30  HikariPool                     releaseConnection(conn)                      │
│  31  LoggingInterceptor              postHandle(req, res, handler, mv)           │
│  32  MappingJackson2HttpMsgConverter write(userResponse, outputStream)           │
│  33  Jackson ObjectMapper            writeValue(stream, userResponse)            │
│  34  LoggingInterceptor              afterCompletion(req, res, handler, null)    │
│  35  Tomcat NIO Connector            flush response bytes → TCP socket           │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### How IoC Container Wires Everything Before the First Request

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Startup Phase — IoC Container Wiring (happens ONCE before any request):         │
│                                                                                  │
│  SpringApplication.run(MyApplication.class)                                      │
│      │                                                                           │
│      ▼                                                                           │
│  ApplicationContext created                                                      │
│      │                                                                           │
│      ▼                                                                           │
│  @ComponentScan scans com.example.** and finds:                                  │
│    → UserController        (@RestController → @Component)                       │
│    → UserServiceImpl       (@Service → @Component)                              │
│    → UserRepository        (JpaRepository → Spring Data creates proxy)          │
│    → UserMapper            (@Component)                                          │
│    → SecurityConfig        (@Configuration)                                      │
│    → GlobalExceptionHandler(@RestControllerAdvice → @Component)                 │
│      │                                                                           │
│      ▼                                                                           │
│  IoC Container resolves dependency graph and instantiates in order:              │
│                                                                                  │
│    1. DataSource bean (HikariCP — auto-configured from application.properties)  │
│    2. EntityManagerFactory (Hibernate — auto-configured)                         │
│    3. PlatformTransactionManager (JpaTransactionManager — auto-configured)      │
│    4. UserRepository proxy (SimpleJpaRepository — Spring Data creates it)       │
│    5. UserMapper (new UserMapper() — no dependencies)                           │
│    6. UserServiceImpl (new UserServiceImpl(userRepository, userMapper))          │
│       → Wrapped in CGLIB proxy for @Transactional                               │
│    7. UserController (new UserController(userServiceProxy))                      │
│    8. DispatcherServlet (auto-configured)                                        │
│       → initStrategies() loads:                                                 │
│         → RequestMappingHandlerMapping (scans @Controller beans for @GetMapping) │
│         → RequestMappingHandlerAdapter                                           │
│         → MappingJackson2HttpMessageConverter                                    │
│    9. Embedded Tomcat started                                                    │
│       → DispatcherServlet registered at "/"                                     │
│       → Listening on port 8080                                                  │
│      │                                                                           │
│      ▼                                                                           │
│  Application is ready to serve requests                                          │
│  ──────────────────────────────────────                                          │
│                                                                                  │
│  Bean Dependency Graph (what IoC wired):                                         │
│                                                                                  │
│  DataSource ──→ EntityManagerFactory ──→ TransactionManager                     │
│                       │                        │                                 │
│                       ▼                        │                                 │
│               UserRepository (proxy)           │                                 │
│                       │                        │                                 │
│                       ▼                        ▼                                 │
│  UserMapper ──→ UserServiceImpl ──→ CGLIB Proxy (@Transactional)                │
│                       │                                                          │
│                       ▼                                                          │
│               UserController                                                     │
│                       │                                                          │
│                       ▼                                                          │
│  RequestMappingHandlerMapping (registers URL → controller method mappings)      │
│                       │                                                          │
│                       ▼                                                          │
│               DispatcherServlet (uses HandlerMapping to route requests)          │
│                       │                                                          │
│                       ▼                                                          │
│               Embedded Tomcat (DispatcherServlet registered at "/")              │
└──────────────────────────────────────────────────────────────────────────────────┘
```


Add all the theories for the following question in detail and wherever possible add diagram and add code

1. How controller maps the method with api, http method, and the model and registers with handlermapping
2. How rest controller maps the method with api , method, response body and registers with handlermapping