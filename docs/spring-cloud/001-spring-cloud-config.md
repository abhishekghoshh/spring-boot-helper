# Spring Cloud Config

## Topics

- Centralized Configuration
- Get configuration from application props
- Setting up Spring Cloud Config Server
- Git and Creating Local Git Repository
- Connect Spring Cloud Config Server to Local Git Repository
- Create Private GitHub Repository and Configure Config Server to Access Private GitHub Repository
- Managing Profiles With Config Server
- Naming Property Files Served by Config Server
- Explain spring.cloud.config.uri, spring.cloud.config.profile, search-paths
- Explain spring.profile.include spring.profile.active spring.cloud.config.profile
- Connect Service to Spring Cloud Config Server
- @RefreshScope Annotation and Refreshing Beans at Runtime
- Configure API Gateway to be a Client of Config Server
- Introduction to Spring Cloud Config(File System as a backend)
- Setting up File System Backend
- Composite Configuration Backends (Git + Vault + JDBC)
- Previewing Values Returned by Spring Cloud Config Server
- Trying how Microservices work
- Introduction to Spring Cloud Config configuration for multiple Microservices
- Shared and Microservice-specific configuration properties
- Config Server High Availability and Multiple Instances
- Enable Basic Authentication for Spring Cloud Config Server
- Configure CSRF exceptions - /actuator/busrefresh
- Configure Client Microservice to use Basic Auth credentials
- Restricting /actuator/busrefresh to ADMIN Role
- Restricting Configuration Properties to CLIENT Role
- Configure Client Microservice to use new access credentials
- Config Server Health Indicator and /actuator/health Details
- Other API endpoints like: /encrypt and /decrypt
- Basic Auth Is Not Encryption
- Introduction to Encryption and Decryption of Configuration Properties
- note about Java Cryptography Extension(JCE)
- Add Java Cryptography Extension
- Configure access to /encrypt and /decrypt API endpoints
- Spring Cloud Config - Symmetric Encryption of configuration properties
- Creating a Keystore for Asymmetric Encryption
- Spring Cloud Config - Asymmetric Encryption of configuration properties

## Detailed Guide

### Centralized Configuration

In a monolithic application, configuration typically lives in a single `application.properties` or `application.yml` file bundled with the deployable artifact. Once an application is decomposed into dozens (or hundreds) of microservices, that approach breaks down: the same database credentials, feature flags, or third-party API keys might need to change across many services simultaneously, and there is no single place to audit or version those values. Centralized Configuration solves this by extracting all environment-specific and shared configuration out of individual services and into one externally managed source of truth.

Spring Cloud Config implements this pattern with a **Config Server** that exposes configuration over HTTP, and **Config Clients** (every microservice) that fetch their properties from that server at startup (and optionally at runtime). The actual configuration values are usually stored in a Git repository, giving you version history, pull-request review, rollback, and branch-per-environment strategies for free. Other backends (file system, HashiCorp Vault, JDBC, Consul) are also supported.

The benefit isn't just "one place to edit values" — it's operational consistency. Because every service resolves configuration the same way (same URI, same profile/label resolution rules), teams stop reinventing config-loading logic per service, and secrets/feature-flags can be rotated without rebuilding or redeploying artifacts (when combined with `@RefreshScope` or Spring Cloud Bus).

```mermaid
flowchart TD
    GIT[(Git Repository<br/>config-repo)] --> CS[Spring Cloud Config Server]
    CS -->|HTTP GET /app/profile| S1[Order Service]
    CS -->|HTTP GET /app/profile| S2[Inventory Service]
    CS -->|HTTP GET /app/profile| S3[Payment Service]
    CS -->|HTTP GET /app/profile| S4[API Gateway]
```

**Real-life scenario:** An e-commerce platform runs 40 microservices across `dev`, `staging`, and `prod`. Marketing needs to flip a "flash sale" feature flag off in production at 2 AM without redeploying anything. Because all 40 services read `feature.flash-sale.enabled` from the same Config Server-backed Git repo, an on-call engineer merges a one-line change and triggers `/actuator/busrefresh`, and every affected service picks up the new value within seconds.

### Get configuration from application props

Before introducing the Config Server, it helps to remember the baseline Spring Boot behavior: any Spring Boot application already reads configuration from `application.properties` or `application.yml` located on its classpath (or external locations like `./config/`), using Spring's `Environment` abstraction. Values are injected via `@Value("${my.property}")`, `@ConfigurationProperties`, or `Environment.getProperty(...)`.

This local, per-artifact configuration model is simple and works well for a single application, but every property change requires rebuilding/redeploying the JAR, and there's no way to share values across services without copy-pasting them into each project. Understanding this default mechanism is important because Spring Cloud Config doesn't replace it — it *extends* it. The Config Client adds a remote `PropertySource` that is layered into the exact same `Environment`, so code that already does `@Value("${my.property}")` continues to work unchanged, whether the value came from a local file or a remote Git-backed Config Server.

```properties
# src/main/resources/application.properties
server.port=8081
greeting.message=Hello from local application.properties
```

```java
@RestController
public class GreetingController {

    @Value("${greeting.message}")
    private String message;

    @GetMapping("/greeting")
    public String greeting() {
        return message;
    }
}
```

**Real-life scenario:** A developer prototyping a new microservice starts with plain `application.yml` values for local development, then later swaps to Spring Cloud Config for staging/production — the `@Value` and `@ConfigurationProperties` code doesn't need to change at all, only the property source changes.

### Setting up Spring Cloud Config Server

The Config Server is itself a Spring Boot application. You enable it by adding the `spring-cloud-config-server` dependency and annotating the main application class with `@EnableConfigServer`. Internally, this annotation registers an auto-configuration that wires up an `EnvironmentRepository` (Git, file system, JDBC, Vault, or a composite of these), a set of REST controllers (`EnvironmentController`, `ResourceController`, etc.) that expose endpoints like `/{application}/{profile}[/{label}]`, and (optionally) encryption/decryption support if a key is configured.

At startup, the Config Server does **not** eagerly load every application's configuration — it lazily resolves and (depending on backend) clones/pulls the backing Git repository the first time a client requests a given `{application}/{profile}/{label}` combination, then caches the result until the next refresh or repository change. This makes the server lightweight to start even if the backing repository contains configuration for hundreds of services.

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

```yaml
# config-server/src/main/resources/application.yml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: file:///Users/dev/config-repo
          default-label: main
```

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant CS as Config Server
    participant Git as Git Repo

    Dev->>CS: Start ConfigServerApplication (@EnableConfigServer)
    CS->>CS: Register EnvironmentRepository, REST endpoints
    Note over CS: Server is up, but repo not yet cloned
    Client->>CS: GET /order-service/prod
    CS->>Git: git clone / pull (first request)
    Git-->>CS: property files
    CS-->>Client: JSON PropertySource response
```

**Real-life scenario:** A platform team stands up a single Config Server instance in a shared "platform" Kubernetes namespace. Every new microservice onboarded to the platform only needs to know one URL (`http://config-server:8888`) and its own `spring.application.name` to immediately inherit organization-wide defaults (logging patterns, actuator exposure, common timeouts).

### Git and Creating Local Git Repository

Git is the default and most commonly used `EnvironmentRepository` backend for Spring Cloud Config because it gives configuration the same guarantees as source code: full history, diffing, branching (used as "labels" in Config Server terminology), and pull-request-based review. Before wiring the Config Server to a remote GitHub repository, it's common to first practice with a **local** Git repository so you can iterate quickly without network calls or authentication concerns.

Creating a local repo is just standard Git: initialize a directory, add profile-specific YAML/properties files named after the target application (`{application}-{profile}.yml`), and commit them. The Config Server then points at this directory using a `file://` URI, which Git treats like any other remote — internally the server still performs `git clone`/`git pull` operations against it, so behavior mirrors a real remote repository (useful for realistic local testing of refresh/labels).

```bash
mkdir config-repo && cd config-repo
git init
cat > order-service.yml <<'EOF'
greeting:
  message: Hello from Git-backed config (default profile)
EOF

cat > order-service-dev.yml <<'EOF'
greeting:
  message: Hello from Git-backed config (dev profile)
EOF

git add .
git commit -m "Initial config for order-service"
```

**Real-life scenario:** A team wants to demo Spring Cloud Config in a workshop without depending on internet access or GitHub credentials — using a local Git repository as the backend lets attendees clone, edit, and commit configuration entirely offline while the Config Server behaves exactly as it would against a real remote.

### Connect Spring Cloud Config Server to Local Git Repository

Once a local Git repository exists, the Config Server is pointed at it via `spring.cloud.config.server.git.uri`, using the `file:` scheme (with three slashes for an absolute path, e.g. `file:///Users/dev/config-repo`). No authentication properties are needed because there's no remote host involved.

A subtlety worth understanding: even for a local `file:` URI, the Config Server clones the repository into a temporary working directory (by default under `/tmp` or the configured `spring.cloud.config.server.git.basedir`) rather than reading files directly from the source path. This means changes committed to the *original* local repo are only visible to clients after the Config Server performs its next `git pull`, which happens on each incoming request by default (configurable via `force-pull` and refresh settings).

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: file:///Users/dev/config-repo
          basedir: /tmp/config-repo-clone
          default-label: main
```

```bash
curl http://localhost:8888/order-service/dev
```

**Real-life scenario:** During local development, an engineer edits a property in `config-repo`, commits it, and immediately curls the Config Server endpoint to confirm the new value is served — validating the full pull → serve pipeline before ever pushing to a shared remote.

### Create Private GitHub Repository and Configure Config Server to Access Private GitHub Repository

Production configuration almost always lives in a **private** remote Git repository (GitHub, GitLab, Bitbucket, or an internal Git server) rather than a local folder, both for durability and for team-wide collaboration through pull requests. Connecting the Config Server to a private repository requires supplying credentials, since anonymous Git operations won't be authorized.

Spring Cloud Config supports several authentication mechanisms: username/password (or, more realistically, a GitHub Personal Access Token used as the password), SSH with a private key, and pre-configured Git credential helpers. Secrets like the PAT should never be hardcoded in `application.yml` in plain text for real deployments — use environment variables, a secrets manager, or (ironically) Spring Cloud Config's own property encryption for the Config Server's *own* bootstrap secrets stored elsewhere.

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/my-org/config-repo.git
          username: ${GITHUB_USERNAME}
          password: ${GITHUB_PERSONAL_ACCESS_TOKEN}
          default-label: main
          clone-on-start: true
```

```yaml
# SSH-based alternative
spring:
  cloud:
    config:
      server:
        git:
          uri: git@github.com:my-org/config-repo.git
          private-key: |
            -----BEGIN OPENSSH PRIVATE KEY-----
            ...
            -----END OPENSSH PRIVATE KEY-----
          ignore-local-ssh-settings: true
```

**Real-life scenario:** A security review mandates that no plaintext credentials appear in any deployed configuration file; the platform team switches the Config Server's GitHub PAT to be injected via a Kubernetes `Secret` mounted as an environment variable (`GITHUB_PERSONAL_ACCESS_TOKEN`), keeping the checked-in `application.yml` free of secrets while still authenticating against the private repo.

### Managing Profiles With Config Server

Spring Profiles (`dev`, `test`, `staging`, `prod`, etc.) let the same codebase behave differently per environment, and the Config Server fully supports profile-specific configuration files in the backing Git repository. When a client requests `/{application}/{profile}`, the server resolves and merges (in increasing precedence) `application.yml`, `application-{profile}.yml`, `{application}.yml`, and `{application}-{profile}.yml` — mirroring the same override semantics Spring Boot uses locally, just sourced remotely.

A client can request multiple, comma-separated profiles (e.g. `/order-service/dev,cloud`), and the server merges them left-to-right with later profiles taking precedence over earlier ones for conflicting keys. This lets you compose configuration layers — for example a `cloud` profile shared by every service deployed to Kubernetes, combined with a service-specific `dev` or `prod` profile.

```bash
curl http://localhost:8888/order-service/dev
curl http://localhost:8888/order-service/dev,cloud
```

**Real-life scenario:** A service needs both environment-specific settings (`dev` database URL) and deployment-platform-specific settings (`cloud` profile enabling Kubernetes service discovery). Requesting `order-service/dev,cloud` merges both cleanly instead of duplicating cloud-specific properties into every environment file.

### Naming Property Files Served by Config Server

The Config Server resolves files using the naming convention `{application}-{profile}.{yml|properties}`, where `{application}` must match the requesting client's `spring.application.name`. Files without a profile suffix (just `{application}.yml`) act as defaults applied regardless of active profile, similar to plain `application.yml` in a standalone Spring Boot app. A file named exactly `application.yml` (or `application-{profile}.yml`) in the repository root is treated as **global** configuration shared by *all* applications, since `application` is the reserved default name Spring Boot uses when no specific `spring.application.name` is set.

Get this naming wrong (e.g. naming a file `orderservice-dev.yml` instead of `order-service-dev.yml`) and the Config Server will silently not find profile-specific overrides — it won't error, it will just fall back to defaults, which is a common source of confusion. The `{label}` segment (typically a Git branch or tag, defaulting to `main`/`master`) can also be included in the request path: `/{application}/{profile}/{label}`.

```text
config-repo/
├── application.yml                # shared by ALL services
├── application-prod.yml           # shared prod overrides
├── order-service.yml              # defaults for order-service
├── order-service-dev.yml          # order-service, dev profile
└── order-service-prod.yml         # order-service, prod profile
```

**Real-life scenario:** A new team member accidentally names a config file `orderService-dev.yml` (camelCase) instead of `order-service-dev.yml` (matching `spring.application.name: order-service`); the client silently ignores it and boots with defaults, causing confusing "why isn't my override applying?" debugging until the naming mismatch is spotted.

### Explain spring.cloud.config.uri, spring.cloud.config.profile, search-paths

On the **client** side, three properties control how a service locates and queries the Config Server. `spring.cloud.config.uri` is the base URL of the Config Server (default `http://localhost:8888`), and can be a comma-separated list of URLs for basic client-side failover. `spring.cloud.config.profile` specifies which profile(s) the client should request configuration for — note this is distinct from the more commonly used `spring.profiles.active`, though by default the client profile mirrors the active Spring profile if not explicitly overridden. `spring.cloud.config.server.git.search-paths` (a **server**-side property) tells the Config Server to look for configuration files in subdirectories of the Git repository rather than only the root — useful for monorepos where each service's config lives in its own folder.

```yaml
# Client (bootstrap.yml / application.yml)
spring:
  application:
    name: order-service
  cloud:
    config:
      uri: http://config-server:8888
      profile: dev
```

```yaml
# Server - search subfolders per service
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/my-org/config-repo.git
          search-paths:
            - '{application}'
            - shared
```

**Real-life scenario:** A monorepo stores config as `config-repo/order-service/order-service-dev.yml` and `config-repo/shared/application.yml`. Setting `search-paths: ['{application}', shared]` lets the Config Server find each service's folder dynamically based on the requesting `{application}` name, without one search path per service.

### Explain spring.profile.include spring.profile.active vs spring.cloud.config.profile

These three properties are frequently confused because they sound similar but operate at different layers. `spring.profiles.active` is a **core Spring Boot** property that determines which profile(s) are active for the running JVM — it affects `@Profile`-annotated beans, which `application-{profile}.yml` files are loaded locally, and (by default) which profile is requested from the Config Server. `spring.profiles.include` **adds** additional profiles alongside whatever is active, without replacing them — commonly used to always layer in a `common` or `logging` profile regardless of environment. `spring.cloud.config.profile` is Config-Client-specific and only affects which profile is requested *from the remote Config Server*; if unset, it defaults to the value of `spring.profiles.active`.

| Property | Scope | Purpose |
|---|---|---|
| `spring.profiles.active` | Local JVM / Spring `Environment` | Determines active profile(s) for beans, local files, and (by default) the config-server request |
| `spring.profiles.include` | Local JVM / Spring `Environment` | Adds extra profile(s) on top of active ones, never replaces them |
| `spring.cloud.config.profile` | Config Client only | Overrides which profile is requested from the Config Server, independent of `spring.profiles.active` |

**Real-life scenario:** A service must run locally with `spring.profiles.active=dev` (for local bean wiring and logging) but still needs to pull `staging` configuration from the Config Server to test against staging-like values — setting `spring.cloud.config.profile=staging` decouples the two without changing local bean behavior.

### Connect Service to Spring Cloud Config Server

To turn any Spring Boot microservice into a Config Client, add the `spring-cloud-starter-config` dependency and declare `spring.application.name` plus `spring.cloud.config.uri` — historically in a special `bootstrap.yml` (loaded before `application.yml`, since remote config must be fetched *before* the rest of the context initializes), though newer Spring Cloud versions support fetching config during regular context initialization via `spring.config.import=configserver:`.

Internally, the Config Client registers a `PropertySourceLocator` that runs early in the Spring Boot startup lifecycle. It calls the Config Server's REST endpoint (`/{application}/{profile}/{label}`), receives a JSON payload describing one or more `PropertySource`s, and inserts them into the `Environment` with high precedence — so remote values can override local `application.yml` defaults. If the Config Server is unreachable and `spring.cloud.config.fail-fast=true`, the client aborts startup instead of running with incomplete configuration.

```yaml
# application.yml (Spring Cloud 2020+ style)
spring:
  application:
    name: order-service
  config:
    import: "configserver:http://localhost:8888"
  cloud:
    config:
      fail-fast: true
```

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

```mermaid
sequenceDiagram
    participant Client as order-service (Config Client)
    participant CS as Config Server
    participant Git as Git Repo

    Client->>Client: Bootstrap phase starts
    Client->>CS: GET /order-service/dev
    CS->>Git: pull latest config
    Git-->>CS: order-service-dev.yml
    CS-->>Client: JSON PropertySource(s)
    Client->>Client: Merge into Environment (high precedence)
    Client->>Client: Continue normal ApplicationContext startup
```

**Real-life scenario:** During a Config Server outage, `order-service` instances configured with `fail-fast: true` refuse to start, immediately surfacing the dependency failure in deployment logs/alerts rather than silently running with stale or missing configuration and causing subtle downstream bugs.

### @RefreshScope Annotation and Refreshing Beans at Runtime

By default, Spring beans are singletons created once at startup, so even if the Config Server's underlying values change, a running application won't see the new values without a restart. `@RefreshScope` solves this by wrapping the annotated bean in a special scope whose backing instance is destroyed and lazily recreated whenever a refresh event occurs — meaning fields injected via `@Value` or `@ConfigurationProperties` on that bean pick up fresh values without restarting the JVM.

A refresh is triggered by calling the actuator endpoint `POST /actuator/refresh` on an individual instance, or (more commonly at scale) by broadcasting a `RefreshRemoteApplicationEvent` to *all* instances of *all* services at once via Spring Cloud Bus (`POST /actuator/busrefresh`), which relays the event over a shared message broker (RabbitMQ/Kafka). Internally, `@RefreshScope` beans are proxies; refreshing doesn't mutate the existing object in place — it discards the cached instance so the next method call re-resolves it via the `BeanFactory`, re-reading the (now updated) `Environment`.

```java
@RefreshScope
@RestController
public class GreetingController {

    @Value("${greeting.message}")
    private String message;

    @GetMapping("/greeting")
    public String greeting() {
        return message;
    }
}
```

```bash
curl -X POST http://localhost:8081/actuator/refresh
curl -X POST http://localhost:8888/actuator/busrefresh
```

```mermaid
sequenceDiagram
    participant Ops as Operator
    participant Svc as Service A (@RefreshScope)
    participant Bus as Spring Cloud Bus (broker)
    participant SvcN as Service B/C/... (@RefreshScope)

    Ops->>Svc: POST /actuator/busrefresh
    Svc->>Bus: publish RefreshRemoteApplicationEvent
    Bus-->>Svc: event delivered to self
    Bus-->>SvcN: event delivered to all instances
    Svc->>Svc: destroy + lazily recreate @RefreshScope beans
    SvcN->>SvcN: destroy + lazily recreate @RefreshScope beans
```

**Real-life scenario:** A production incident requires immediately lowering a rate-limit threshold across 12 instances of a payment service. Instead of a rolling restart (risking downtime), the on-call engineer edits the Git-backed config, commits, and hits `/actuator/busrefresh` once — Spring Cloud Bus propagates the refresh to all 12 instances within seconds.

### Configure API Gateway to be a Client of Config Server

An API Gateway (e.g. Spring Cloud Gateway) is itself just another Spring Boot application, so it becomes a Config Client the same way any microservice does: adding `spring-cloud-starter-config`, setting `spring.application.name`, and pointing `spring.cloud.config.uri` (or `spring.config.import=configserver:...`) at the Config Server. This is particularly valuable for the gateway because routing rules, rate-limit thresholds, CORS policies, and downstream service URIs are exactly the kind of environment-specific values that benefit from centralization — and being able to `@RefreshScope` route definitions means routing changes can be pushed without redeploying the gateway.

One gateway-specific nuance: route definitions declared under `spring.cloud.gateway.routes` are bound via `@ConfigurationProperties`, and dynamic refreshing of gateway routes typically requires triggering `/actuator/gateway/refresh` in addition to (or instead of) the generic `/actuator/refresh`, since route definitions are cached separately by the `RouteDefinitionLocator`.

```yaml
# api-gateway/src/main/resources/application.yml
spring:
  application:
    name: api-gateway
  config:
    import: "configserver:http://localhost:8888"
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: ${ORDER_SERVICE_URI}
          predicates:
            - Path=/orders/**
```

**Real-life scenario:** When a downstream service is migrated to a new internal DNS name, updating `ORDER_SERVICE_URI` in the Git-backed config repo and refreshing the gateway avoids a redeploy of the gateway itself, minimizing risk to the single most critical piece of routing infrastructure.

### Introduction to Spring Cloud Config(File System as a backend)

While Git is the most common backend, Spring Cloud Config also supports a plain **native/file system** backend (`spring.profiles.active=native` on the server), which serves configuration files directly from the local classpath or a specified filesystem directory instead of a Git repository. This is useful for simple setups, air-gapped environments without Git infrastructure, or scenarios where configuration is generated/mounted onto disk by another process (e.g. a Kubernetes ConfigMap volume mount).

The trade-off is losing Git's built-in versioning, audit trail, and branch-based labeling — with the file system backend, "labels" aren't meaningful in the same way, and rollback/history must be handled by whatever mechanism manages the files on disk (or not at all). It's best suited to simpler deployments or as a local-testing stand-in before wiring up a real Git remote.

**Real-life scenario:** A tightly regulated on-premises deployment has no outbound network access and no internal Git server provisioned for the platform team yet; configuration files are instead placed directly onto the Config Server's disk (or mounted via a ConfigMap) using the native/file-system backend as an interim solution.

### Setting up File System Backend

Enabling the file system backend means activating the `native` Spring profile on the Config Server and pointing `spring.cloud.config.server.native.search-locations` at one or more classpath or file-system directories containing the same `{application}-{profile}.yml` naming convention used by the Git backend. Multiple locations can be listed, and (like `search-paths` for Git) `{application}` placeholders can be used to organize per-service subdirectories.

```yaml
# Config Server application.yml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations:
            - file:///opt/config-repo
            - classpath:/shared-config/
```

```bash
# Start server with the native profile explicitly
java -jar config-server.jar --spring.profiles.active=native
```

**Real-life scenario:** A CI pipeline generates environment-specific YAML files as a build artifact and drops them into `/opt/config-repo` on the Config Server host before the server starts, letting the native backend serve them without any Git dependency in the deployment pipeline.

### Composite Configuration Backends (Git + Vault + JDBC)

Real-world systems often need more than one kind of configuration source at once — for example, non-sensitive application settings in Git, secrets in HashiCorp Vault, and legacy configuration already stored in a relational database via JDBC. Spring Cloud Config supports this through a **composite** `EnvironmentRepository`, configured by setting `spring.cloud.config.server.composite` to a list of repository definitions, each with its own `type` (`git`, `vault`, `jdbc`, `native`, etc.).

When a client requests configuration, the Config Server queries **each** repository in the composite list and merges the resulting property sources, with repositories listed **earlier** taking precedence over later ones for conflicting keys (order matters). This lets teams keep sensitive values out of Git entirely (in Vault) while still benefiting from Git's review workflow for everything else, all served through the same single Config Server endpoint that clients already know how to call.

```yaml
spring:
  cloud:
    config:
      server:
        composite:
          - type: git
            uri: https://github.com/my-org/config-repo.git
          - type: vault
            host: vault.internal
            port: 8200
            scheme: https
          - type: jdbc
            sql: SELECT prop_key, prop_value FROM properties WHERE application=? and profile=? and label=?
```

```mermaid
flowchart TD
    Client[Config Client] -->|GET /order-service/prod| CS[Config Server<br/>Composite Repository]
    CS --> Git[(Git Repo<br/>app settings)]
    CS --> Vault[(HashiCorp Vault<br/>secrets)]
    CS --> JDBC[(Relational DB<br/>legacy properties)]
    Git --> Merge[Merged PropertySource]
    Vault --> Merge
    JDBC --> Merge
    Merge --> Client
```

| Backend | Best for | Versioning | Secret-safe |
|---|---|---|---|
| Git | Most application config, reviewed via PRs | Yes (full history) | No (avoid raw secrets) |
| Vault | Passwords, API keys, certificates | Vault's own versioned KV | Yes (built for secrets) |
| JDBC | Legacy systems already storing config in a DB | Depends on schema/audit table | No |
| File System / native | Simple/offline/testing setups | No | No |

**Real-life scenario:** A financial services company keeps feature flags and timeouts in Git (reviewable, low-risk) but stores database passwords and third-party API keys in Vault (rotated automatically, audited access), while a legacy billing system still reads some values from an old JDBC-backed properties table — a composite backend unifies all three under one Config Server API for every client.

### Previewing Values Returned by Spring Cloud Config Server

Before wiring a client, it's good practice to directly query the Config Server's REST API to confirm exactly what values it will return — the Config Server exposes plain HTTP GET endpoints following the pattern `/{application}/{profile}[/{label}]`, as well as `/{application}-{profile}.yml` (or `.properties`, `.json`) for a flattened, ready-to-use file format instead of the default nested JSON `PropertySource` structure.

The default JSON response includes metadata (which files/property-sources contributed which values and in what precedence order), which is invaluable for debugging "why is my property not what I expect" issues, since it shows the exact merge order across default/profile/shared files.

```bash
# Full JSON PropertySource breakdown (most useful for debugging precedence)
curl http://localhost:8888/order-service/dev

# Flattened YAML exactly as the client would receive it
curl http://localhost:8888/order-service-dev.yml

# Flattened properties format
curl http://localhost:8888/order-service-dev.properties
```

**Real-life scenario:** A developer is confused why a property isn't taking the expected value; curling `/order-service/dev` reveals two property sources both define the key, and the *first* one listed in the JSON response (higher precedence) is the "shared" `application-dev.yml`, not the service-specific file, exposing an unintended override.

### Trying how Microservices work

Before deep-diving into config-per-microservice patterns, it's worth running a small multi-service sandbox: a Config Server plus two or three trivial Spring Boot services, each registering under a distinct `spring.application.name`, each requesting its own profile-specific configuration, to see end-to-end how the pieces (Git repo, Config Server, multiple clients, refresh) interact in practice. This hands-on step typically precedes adding service discovery (Eureka/Consul) and an API Gateway, since Config Server is usually the *first* piece of Spring Cloud infrastructure introduced into a growing system.

```mermaid
flowchart LR
    subgraph Infra
        CS[Config Server]
        Git[(config-repo)]
    end
    subgraph Services
        A[order-service]
        B[inventory-service]
        C[api-gateway]
    end
    Git --> CS
    CS --> A
    CS --> B
    CS --> C
```

**Real-life scenario:** A team learning Spring Cloud for the first time spins up this minimal sandbox locally with `docker-compose` (Config Server + two demo services) to build confidence in the config-fetch flow before introducing production concerns like private repos, encryption, and high availability.

### Introduction to Spring Cloud Config configuration for multiple Microservices

As the number of services grows, a config repository needs an organizing convention so it doesn't become an unmanageable flat pile of YAML files. The typical pattern is one `{application}.yml` + `{application}-{profile}.yml` pair per microservice (matching each service's `spring.application.name`), plus a shared `application.yml` (and `application-{profile}.yml`) for cross-cutting concerns common to *every* service — logging levels, actuator exposure, common resilience timeouts, and organization-wide defaults.

This mirrors how Spring Boot itself layers configuration locally (`application.yml` as defaults, profile files as overrides) but extended to a fleet of independently deployable services, all resolved dynamically by the single Config Server based on the requesting client's declared `spring.application.name` and active profile.

```text
config-repo/
├── application.yml                # org-wide defaults (all services)
├── application-prod.yml           # org-wide prod overrides
├── order-service.yml
├── order-service-prod.yml
├── inventory-service.yml
├── inventory-service-prod.yml
├── api-gateway.yml
└── api-gateway-prod.yml
```

**Real-life scenario:** Onboarding a brand-new "shipping-service" microservice only requires adding `shipping-service.yml`/`shipping-service-prod.yml` to the existing repo — it automatically inherits all organization-wide defaults from `application.yml` without any Config Server redeployment or reconfiguration.

### Shared and Microservice-specific configuration properties

Building on the previous convention, it's important to be deliberate about *which* properties belong in the shared `application.yml` versus a service-specific file. Good candidates for **shared** config: logging patterns, common actuator/management settings, shared Resilience4j/circuit-breaker defaults, common Kafka/RabbitMQ broker addresses, and organization-wide timeouts. Good candidates for **service-specific** config: a service's own database URL/schema, feature flags scoped to that service, downstream service URLs it calls, and thread-pool sizing tuned to that service's workload.

Because service-specific files take precedence over the shared file for the same key (per the merge order described earlier), a service can always override an organization-wide default locally when it has a legitimate reason to diverge, without needing platform team approval to change the shared file.

```yaml
# application.yml (shared, all services)
logging:
  level:
    root: INFO
management:
  endpoints:
    web:
      exposure:
        include: health, info, refresh, busrefresh

# order-service.yml (specific overrides)
logging:
  level:
    com.example.order: DEBUG
spring:
  datasource:
    url: jdbc:postgresql://order-db:5432/orders
```

**Real-life scenario:** All services share the same `management.endpoints.web.exposure.include` list defined once centrally, but the `order-service` team temporarily bumps its own package's log level to `DEBUG` in `order-service.yml` while chasing a bug, without affecting logging verbosity for any other service.

### Config Server High Availability and Multiple Instances

A single Config Server instance is a single point of failure: if it's down, no service can start (or refresh) until it's back — a serious risk, especially since many services depend on it during bootstrap. Making the Config Server highly available simply means running **multiple stateless instances** behind a load balancer (or registered with a service registry like Eureka), since the Config Server itself holds no persistent local state — its only state is the cloned Git working directory, which is disposable and rebuildable from the same remote repository.

Because instances are stateless and interchangeable, HA doesn't require sticky sessions or shared storage — any instance can serve any request, as long as they're all pointed at the same backing repository/branch. Clients can be given a list of Config Server URLs (`spring.cloud.config.uri` accepts a comma-separated list) for basic client-side failover, or more commonly, discovery-first lookup is used so clients resolve the Config Server via the service registry instead of a hardcoded URL.

```yaml
# Client with discovery-based config server lookup
spring:
  cloud:
    config:
      discovery:
        enabled: true
        service-id: config-server
      fail-fast: true
      retry:
        max-attempts: 6
        initial-interval: 1000
        multiplier: 1.5
```

```mermaid
flowchart TD
    LB[Load Balancer / Service Registry] --> CS1[Config Server Instance 1]
    LB --> CS2[Config Server Instance 2]
    LB --> CS3[Config Server Instance 3]
    CS1 --> Git[(Shared Git Repo)]
    CS2 --> Git
    CS3 --> Git
    Client1[Service A] --> LB
    Client2[Service B] --> LB
```

**Real-life scenario:** During a rolling Kubernetes deployment that briefly takes one Config Server pod offline, the remaining replicas behind the `ClusterIP` Service continue serving requests uninterrupted, and newly starting service pods with `retry` configured simply retry against the load balancer until a healthy instance responds.

### Enable Basic Authentication for Spring Cloud Config Server

Because the Config Server can expose sensitive configuration (database credentials, API keys — even if some are encrypted at rest, plenty of legitimately-plaintext operational values remain), its endpoints should never be left unauthenticated in any non-trivial deployment. The simplest protection is HTTP Basic Authentication, enabled by adding `spring-boot-starter-security` to the Config Server and defining a username/password (or a full `SecurityFilterChain` for more control).

Once Spring Security is on the classpath, **all** endpoints are secured by default (including actuator endpoints), so client applications must now supply matching credentials on every config-fetch request, and any tooling (curl, browsers) used to preview values must also authenticate.

```yaml
# Config Server application.yml
spring:
  security:
    user:
      name: configuser
      password: ${CONFIG_SERVER_PASSWORD}
```

```bash
curl -u configuser:secret http://localhost:8888/order-service/dev
```

**Real-life scenario:** A penetration test flags an internal Config Server as reachable without authentication from within the cluster network; enabling Basic Auth (as a first, minimal step before a more complete OAuth2/mTLS setup) immediately closes off casual/unauthorized access to production secrets.

### Configure CSRF exceptions - /actuator/busrefresh

Spring Security enables CSRF (Cross-Site Request Forgery) protection by default for state-changing HTTP methods (POST, PUT, DELETE) on any endpoint, which includes `/actuator/busrefresh` once Basic Auth (and therefore Spring Security) is added to the Config Server. Because `/actuator/busrefresh` is typically invoked by automated tooling (CI/CD pipelines, curl scripts, webhooks) rather than a browser session with a CSRF token, the default CSRF protection blocks these legitimate calls with a `403 Forbidden`.

The fix is to explicitly disable (or exempt) CSRF protection for actuator endpoints, since actuator calls made with valid Basic Auth credentials over HTTPS don't have the same cross-site forgery risk profile as browser form submissions relying on cookies.

```java
@Configuration
@EnableWebSecurity
public class ConfigServerSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/actuator/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

**Real-life scenario:** A CI/CD pipeline step that calls `curl -X POST -u user:pass https://config-server/actuator/busrefresh` after every config-repo merge starts failing with `403` the moment Spring Security is introduced — the fix is adding the CSRF exemption for `/actuator/**`, not disabling security altogether.

### Configure Client Microservice to use Basic Auth credentials

Once the Config Server requires Basic Authentication, every Config Client must supply matching credentials when fetching configuration, or its own startup will fail with a `401 Unauthorized`. This is configured on the client via `spring.cloud.config.username` and `spring.cloud.config.password` (or embedded directly in the `spring.cloud.config.uri` as `http://user:pass@host:port`, though the separate properties are preferable since they can be injected from environment variables/secrets rather than hardcoded in the URI).

```yaml
# Config Client application.yml (or bootstrap.yml)
spring:
  application:
    name: order-service
  cloud:
    config:
      uri: http://config-server:8888
      username: configuser
      password: ${CONFIG_SERVER_PASSWORD}
```

**Real-life scenario:** After the platform team enables Basic Auth on the shared Config Server, every one of the 40 onboarded microservices needs a coordinated deployment adding the new `spring.cloud.config.username`/`password` properties (sourced from a shared Kubernetes secret) — otherwise they fail to start on the next redeploy.

### Restricting /actuator/busrefresh to ADMIN Role

A single set of Basic Auth credentials shared by all clients is a coarse security model — it authenticates *that* a caller is a legitimate client, but doesn't distinguish between "a microservice fetching its own config" and "an operator triggering a fleet-wide refresh." Since `/actuator/busrefresh` can force every service in the system to reload configuration simultaneously (a meaningful operational action, potentially disruptive if misused), it's good practice to restrict it to a separate, more privileged role (e.g. `ADMIN`) rather than the general-purpose client credentials.

This is implemented with Spring Security's role-based authorization rules, defining multiple users (or integrating with an external identity provider) where regular config-fetch endpoints require only `USER`/`CLIENT` authority, while `/actuator/busrefresh` (and similarly sensitive actuator endpoints) require `ADMIN`.

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/actuator/**"))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/busrefresh").hasRole("ADMIN")
            .requestMatchers("/actuator/**").hasAnyRole("ADMIN", "CLIENT")
            .anyRequest().hasAnyRole("ADMIN", "CLIENT")
        )
        .httpBasic(Customizer.withDefaults());
    return http.build();
}

@Bean
public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    UserDetails client = User.withUsername("configuser")
        .password(encoder.encode("clientpass")).roles("CLIENT").build();
    UserDetails admin = User.withUsername("configadmin")
        .password(encoder.encode("adminpass")).roles("ADMIN").build();
    return new InMemoryUserDetailsManager(client, admin);
}
```

**Real-life scenario:** A junior developer's automated script accidentally has the config-fetch credentials hardcoded in a shared library; because those credentials only carry `CLIENT` authority (not `ADMIN`), an accidental or malicious call to `/actuator/busrefresh` using them is rejected with `403`, limiting the blast radius of leaked non-admin credentials.

### Restricting Configuration Properties to CLIENT Role

Symmetrically, the reverse concern also applies: an `ADMIN` user capable of triggering refreshes shouldn't necessarily be the *only* identity allowed to fetch configuration, and conversely, regular config-fetch endpoints should be reachable by ordinary client-role credentials without needing admin privileges. This is really the same role-based authorization mechanism as above, just emphasizing that the general `/{application}/{profile}` config-fetch endpoints should be authorized for the `CLIENT` (or equivalent) role, and each microservice's credentials should be scoped to exactly that — least-privilege access that can read configuration but cannot perform administrative actions like triggering bus-wide refreshes or viewing full actuator diagnostics.

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/busrefresh").hasRole("ADMIN")
    .requestMatchers("/{application}/{profile}/**").hasAnyRole("CLIENT", "ADMIN")
    .requestMatchers("/encrypt/**", "/decrypt/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
```

**Real-life scenario:** Every microservice is provisioned with `CLIENT`-role credentials sufficient only to fetch its own configuration; only the platform team's CI/CD pipeline holds `ADMIN` credentials capable of calling `/actuator/busrefresh` or `/encrypt`, enforcing least-privilege across the whole system.

### Configure Client Microservice to use new access credentials

Whenever the Config Server's authentication scheme changes — new users, new roles, rotated passwords — every client's `spring.cloud.config.username`/`password` (or discovery-based credential source) must be updated in lockstep, otherwise clients will fail to fetch configuration on their next restart or refresh attempt. In practice, this is usually managed by pulling credentials from a shared secret store (Kubernetes `Secret`, HashiCorp Vault, AWS Secrets Manager) referenced by environment variable, rather than hardcoding values per-service, so a credential rotation is a single change propagated to all consumers rather than dozens of individual file edits.

```yaml
spring:
  cloud:
    config:
      username: ${CONFIG_CLIENT_USERNAME}
      password: ${CONFIG_CLIENT_PASSWORD}
```

**Real-life scenario:** After a scheduled credential rotation, the platform team updates a single Kubernetes `Secret` referenced by every microservice's deployment manifest as `CONFIG_CLIENT_PASSWORD`; a coordinated rolling restart picks up the new value everywhere without touching individual service YAML files.

### Config Server Health Indicator and /actuator/health Details

The Config Server ships with a custom `HealthIndicator` (`ConfigServerHealthIndicator`) that, by default, actively verifies connectivity to its configured backend(s) — for the Git backend, this means attempting to resolve a default application's configuration (typically named `app`) to confirm the repository is reachable and credentials are valid, not just that the Spring Boot process is running. This makes `/actuator/health` a genuinely useful liveness/readiness signal for orchestrators like Kubernetes: a Config Server that's up but can't reach its Git remote will correctly report `DOWN` (or a degraded status) instead of falsely reporting healthy.

This behavior can be tuned or disabled (`spring.cloud.config.server.health.enabled=false`) if the extra backend round-trip per health check is undesirable, or configured to check specific named applications/profiles.

```yaml
management:
  endpoint:
    health:
      show-details: always
spring:
  cloud:
    config:
      server:
        health:
          enabled: true
          repositories:
            order-service:
              label: main
              name: order-service
              profiles: prod
```

```bash
curl http://localhost:8888/actuator/health
```

**Real-life scenario:** A Kubernetes readiness probe hitting `/actuator/health` correctly keeps a Config Server pod out of the load balancer rotation during a transient GitHub outage, since the health indicator's Git connectivity check fails, preventing clients from being routed to an instance that can't actually serve fresh configuration.

### Other API endpoints like: /encrypt and /decrypt

Beyond the core `/{application}/{profile}/{label}` configuration-fetch endpoints, the Config Server exposes utility endpoints `/encrypt` and `/decrypt` (enabled once a symmetric key or asymmetric keystore is configured) that let you encrypt a plaintext value into a `{cipher}`-prefixed ciphertext suitable for storing directly in a Git-backed properties file, and decrypt it back for verification. These endpoints are typically used by developers/operators preparing configuration values before committing them, not called by client applications at runtime — clients receive already-decrypted values transparently, since the Config Server decrypts `{cipher}`-prefixed properties server-side before returning them.

```bash
# Encrypt a plaintext secret
curl -X POST --data-urlencode "my-super-secret-db-password" http://localhost:8888/encrypt

# Response, e.g.: AQAj7f8...base64ciphertext...

# Decrypt to verify
curl -X POST --data-urlencode "AQAj7f8...base64ciphertext..." http://localhost:8888/decrypt
```

```yaml
# order-service-prod.yml, storing the ciphertext directly
spring:
  datasource:
    password: '{cipher}AQAj7f8...base64ciphertext...'
```

**Real-life scenario:** Before committing a production database password to the Git-backed config repo, an operator calls `/encrypt` to obtain a `{cipher}`-prefixed ciphertext, commits *that* instead of the plaintext, and the Config Server transparently decrypts it only in-memory when serving the value to the authorized client.

### Basic Auth Is Not Encryption

A common misunderstanding: enabling HTTP Basic Authentication on the Config Server protects **who can call the API**, but does absolutely nothing to protect **the confidentiality of values stored in the Git repository itself**, nor values in transit if TLS isn't also enabled. Anyone with read access to the Git repository (which may include more people than have Config Server API credentials — e.g. an intern with repo read access, or a compromised CI runner) can see plaintext secrets committed there, entirely bypassing Basic Auth. Similarly, Basic Auth credentials sent over plain HTTP (not HTTPS) are trivially interceptable in transit.

Real protection for sensitive values requires **encryption at rest** (via the `/encrypt` endpoint and symmetric/asymmetric keys, storing only ciphertext in Git) combined with **TLS in transit**, in addition to (not instead of) authentication/authorization controls. Basic Auth answers "who may ask the Config Server for values"; encryption answers "can the values be read by anyone who merely has access to the storage/network, without the decryption key."

| Concern | Basic Auth | TLS | Property Encryption (`{cipher}`) |
|---|---|---|---|
| Protects API access | Yes | No | No |
| Protects data in transit | No | Yes | Partially (already ciphertext) |
| Protects data at rest (in Git) | No | No | Yes |
| Protects against repo-read-access leaks | No | No | Yes |

**Real-life scenario:** A security audit discovers that plaintext AWS credentials committed to the "private" config-repo are visible to every engineer with read access to the repository (far more people than have Config Server login credentials); the remediation is re-committing those values through `/encrypt` as `{cipher}`-prefixed ciphertext, not merely tightening Basic Auth further.

### Introduction to Encryption and Decryption of Configuration Properties

Spring Cloud Config supports encrypting individual property values before they're committed to the backing repository, storing them with a `{cipher}` prefix so the Config Server recognizes and automatically decrypts them server-side before returning values to authenticated clients. This means sensitive values (database passwords, API keys, signing secrets) never need to exist in plaintext anywhere in the Git history, while remaining fully usable by client applications, which receive already-decrypted plain values transparently through the normal config-fetch flow.

Two encryption modes are supported: **symmetric** encryption (a single shared secret key used for both encrypting and decrypting) and **asymmetric** encryption (an RSA key pair — a public key for encrypting, a private key held only by the Config Server for decrypting, typically stored in a Java keystore). Symmetric is simpler to set up; asymmetric offers stronger operational security since the encryption key (public) can be distributed to anyone preparing config values without exposing the decryption capability itself.

```mermaid
flowchart LR
    Dev[Developer / CI] -->|POST /encrypt plaintext| CS[Config Server]
    CS -->|"{cipher}ciphertext"| Dev
    Dev -->|commit ciphertext| Git[(Git Repo)]
    Client[Config Client] -->|GET /app/profile| CS
    Git --> CS
    CS -->|decrypt server-side| CS
    CS -->|plaintext value| Client
```

**Real-life scenario:** A team storing all configuration in a single shared Git repository (read by many engineers for legitimate reasons) uses property encryption so that even though the repository itself isn't secret, individual sensitive values within it are unreadable without the Config Server's decryption key.

### note about Java Cryptography Extension(JCE)

Older JDK distributions shipped with restricted cryptographic policies (the "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy") that capped the maximum key length usable for certain algorithms (e.g. limiting AES to 128-bit keys unless the unlimited-strength policy files were manually installed). Attempting to use full-strength keys (like 256-bit AES, or certain RSA key sizes for asymmetric encryption) without the unlimited policy installed historically caused a runtime `InvalidKeyException: Illegal key size` error.

Since JDK 8u151+ (and by default in JDK 9+), this restriction was relaxed/removed, and the unlimited-strength policy is enabled by default in modern JDKs — but it's still a common gotcha when working with older JDK 8 base images (especially minimal/slim container images that sometimes strip policy files), so it's worth knowing this history when debugging cryptography-related startup failures in a Config Server using strong keys.

**Real-life scenario:** A team encounters a mysterious `InvalidKeyException: Illegal key size` when enabling asymmetric encryption on a Config Server running on an old, unpatched JDK 8 base Docker image; upgrading the base image (or enabling the unlimited-strength policy) resolves it immediately.

### Add Java Cryptography Extension

On modern JDKs this step is usually unnecessary (unlimited strength is default), but on legacy JDK 8 distributions, enabling full-strength cryptography meant downloading Oracle's "JCE Unlimited Strength Jurisdiction Policy Files" ZIP and replacing `local_policy.jar`/`US_export_policy.jar` in the JRE's `lib/security` directory, or (from 8u151 onward) simply setting a security property to opt in without replacing any files.

```bash
# JDK 8u151+ opt-in without replacing policy files
# In $JAVA_HOME/jre/lib/security/java.security, set:
crypto.policy=unlimited
```

```dockerfile
# Ensuring a modern JDK base image (which defaults to unlimited strength)
FROM eclipse-temurin:17-jre-jammy
```

**Real-life scenario:** Rather than manually patching JCE policy files on an aging JDK 8 image, a team simply migrates the Config Server's Dockerfile to a current JDK 17 base image, which includes unlimited-strength cryptography by default and sidesteps the issue entirely.

### Configure access to /encrypt and /decrypt API endpoints

Because `/encrypt` and `/decrypt` can reveal or manipulate sensitive cryptographic material, access to them should be at least as restricted as any other sensitive administrative endpoint — typically gated behind the same `ADMIN` role used for `/actuator/busrefresh`, and definitely never left open to the same broad `CLIENT` role used for ordinary config-fetch requests. In some deployments, these endpoints are disabled entirely on the "runtime" Config Server instances and only enabled on a separate, network-isolated instance used purely for offline encryption/decryption tooling by the platform team.

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/encrypt/**", "/decrypt/**").hasRole("ADMIN")
    .requestMatchers("/actuator/busrefresh").hasRole("ADMIN")
    .requestMatchers("/{application}/{profile}/**").hasAnyRole("CLIENT", "ADMIN")
    .anyRequest().authenticated()
)
```

**Real-life scenario:** A platform team restricts `/encrypt` and `/decrypt` to `ADMIN`-role credentials held only by the small group responsible for onboarding new secrets into the config repo, ensuring rank-and-file service accounts can fetch configuration but can't probe the encryption endpoints.

### Spring Cloud Config - Symmetric Encryption of configuration properties

Symmetric encryption uses a single shared secret key, configured via `encrypt.key` (or, more securely, the `ENCRYPT_KEY` environment variable rather than a plaintext property file) on the Config Server. Once set, the `/encrypt` and `/decrypt` endpoints become active, and any property value prefixed with `{cipher}` in the backing repository is automatically decrypted server-side using this key before being returned to clients.

The main operational weakness of symmetric encryption is key distribution and rotation: the same key that encrypts must also decrypt, so it must be kept confidential yet available to the Config Server process, and rotating it requires re-encrypting every previously-encrypted value with the new key — there's no way to "encrypt-only" a value with a restricted-privilege key the way asymmetric encryption allows.

```yaml
# Config Server application.yml (prefer env var over plaintext!)
encrypt:
  key: ${ENCRYPT_KEY}
```

```bash
export ENCRYPT_KEY="a-long-random-shared-secret-value"

curl -X POST --data-urlencode "supersecretpassword" http://localhost:8888/encrypt
# => {cipher}686e2...

curl -X POST --data-urlencode "{cipher}686e2..." http://localhost:8888/decrypt
# => supersecretpassword
```

```mermaid
flowchart TD
    Key[Shared Secret Key encrypt.key] --> Enc[POST /encrypt]
    Key --> Dec[Server-side decrypt on config fetch]
    Enc -->|"{cipher}xyz"| Git[(Git Repo)]
    Git --> Dec
    Dec --> Client[Config Client receives plaintext]
```

**Real-life scenario:** A small startup with a single Config Server and a handful of services uses symmetric encryption for simplicity — a single `ENCRYPT_KEY` environment variable set via their secrets manager is enough to protect all committed secrets, without the added complexity of managing an RSA key pair.

### Creating a Keystore for Asymmetric Encryption

Asymmetric encryption requires an RSA key pair stored in a Java KeyStore (JKS or PKCS12) file, generated with the JDK's `keytool` utility. The keystore contains a private key (used only by the Config Server to decrypt) and its corresponding public key/certificate (which can be freely distributed to anyone who needs to encrypt values, e.g. via the `/encrypt` endpoint or an extracted public key used offline, without granting them decryption capability).

```bash
keytool -genkeypair \
  -alias config-server-key \
  -keyalg RSA \
  -keysize 2048 \
  -keystore server.jks \
  -storetype JKS \
  -validity 3650 \
  -dname "CN=Config Server,OU=Platform,O=MyOrg,C=US" \
  -storepass ${KEYSTORE_PASSWORD} \
  -keypass ${KEYSTORE_PASSWORD}
```

```bash
# Verify the keystore contents
keytool -list -v -keystore server.jks -storepass ${KEYSTORE_PASSWORD}
```

**Real-life scenario:** The platform security team generates the Config Server's RSA keystore once, stores `server.jks` and its password in a secrets manager (never in Git), and mounts it into the Config Server's container at deploy time, keeping the private key off every developer's laptop.

### Spring Cloud Config - Asymmetric Encryption of configuration properties

With a keystore in place, the Config Server is configured to use it via `encrypt.key-store.*` properties instead of a plain `encrypt.key`. Once active, `/encrypt` uses the public key to produce ciphertext, and only the Config Server (holding the private key inside the keystore) can decrypt `{cipher}`-prefixed values when serving configuration to clients — meaning even someone who can call `/encrypt` (to prepare new secrets) cannot use that same access to decrypt existing ones, a meaningfully stronger separation of duties than symmetric encryption provides.

```yaml
encrypt:
  key-store:
    location: classpath:/server.jks
    password: ${KEYSTORE_PASSWORD}
    alias: config-server-key
    secret: ${KEY_PASSWORD}
```

```bash
curl -X POST --data-urlencode "supersecretpassword" http://localhost:8888/encrypt
# => {cipher}AQBx3f...   (encrypted with the public key)
```

```mermaid
flowchart TD
    subgraph Keystore [server.jks]
        Pub[Public Key]
        Priv[Private Key]
    end
    Dev[Developer] -->|POST /encrypt| CS[Config Server]
    CS -->|uses| Pub
    CS -->|"{cipher}ciphertext"| Git[(Git Repo)]
    Client[Config Client] -->|GET /app/profile| CS
    Git --> CS
    CS -->|uses| Priv
    CS -->|decrypted plaintext| Client
```

| Aspect | Symmetric Encryption | Asymmetric Encryption |
|---|---|---|
| Key material | Single shared secret (`encrypt.key`) | RSA key pair in a keystore |
| Setup complexity | Low | Higher (keytool, keystore management) |
| Encrypt/decrypt separation | None — same key does both | Yes — public key encrypts, private key decrypts |
| Key rotation | Must re-encrypt all values with new key | Can rotate/manage keys via keystore aliases |
| Best for | Small teams, simpler deployments | Larger orgs needing separation of duties |

**Real-life scenario:** A large enterprise wants developers across many teams to be able to encrypt new secrets for their own services' config files without ever being able to decrypt *existing* secrets belonging to other teams; asymmetric encryption (distributing only the public key/encrypt capability broadly, while the private key stays exclusively on the Config Server) achieves exactly that separation.

## Interview Questions & Answers

### Config Server Basics

**Q: What problem does Spring Cloud Config solve?**

It centralizes externalized configuration for distributed systems so that dozens or hundreds of microservices can share, version, and update configuration from a single source of truth (typically a Git repository) instead of bundling configuration inside each service's deployable artifact. This enables consistent configuration management, auditability via Git history, and the ability to change configuration without rebuilding or redeploying services.

**Q: What is the difference between the Config Server and a Config Client?**

The Config Server is a standalone Spring Boot application (annotated with `@EnableConfigServer`) that exposes configuration over HTTP from a backend (Git, file system, Vault, JDBC, or composite). A Config Client is any Spring Boot microservice that depends on `spring-cloud-starter-config` and fetches its configuration from the Config Server at startup (and optionally at runtime via refresh), merging the remote values into its local `Environment`.

**Q: What backends does Spring Cloud Config Server support?**

Git (the default and most common), a native/file-system backend, JDBC (relational database), HashiCorp Vault, and Consul, plus a composite backend that combines multiple of these simultaneously with defined precedence order.

**Q: How does the Config Server resolve which files to serve for a request like `/order-service/prod`?**

It looks for files matching `{application}-{profile}.yml/.properties` for the requested application and profile, plus `application.yml`/`application-{profile}.yml` as shared defaults for all applications, merging them with profile-specific and application-specific files taking precedence over generic ones. The optional `{label}` segment (a Git branch/tag, default `main`) can further scope which version of the repository is queried.

**Q: Why might the Config Server not eagerly clone the Git repository at startup?**

Because cloning is done lazily on the first client request for a given `{application}/{profile}/{label}` combination (then cached), which keeps server startup fast even if the backing repository holds configuration for a very large number of applications.

### Profiles & Property Resolution

**Q: What's the difference between `spring.profiles.active`, `spring.profiles.include`, and `spring.cloud.config.profile`?**

`spring.profiles.active` determines the active Spring profile(s) for the local JVM (affecting `@Profile` beans and local file loading), and by default is also what's sent to the Config Server. `spring.profiles.include` layers additional profiles on top of the active one(s) without replacing them. `spring.cloud.config.profile` is Config-Client-specific and overrides which profile(s) are requested from the remote Config Server independently of the local active profile.

**Q: If a client requests profiles `dev,cloud`, which one wins on a conflicting key?**

The later profile in the list takes precedence, so `cloud` would override `dev` for any conflicting property — profiles are merged left-to-right with increasing precedence.

**Q: What does `spring.cloud.config.server.git.search-paths` do, and when would you use it?**

It tells the Config Server to look inside specific subdirectories of the Git repository (rather than only the root) when resolving configuration files, using placeholders like `{application}`. It's useful for monorepos where each service's config lives in its own subfolder.

**Q: Why might a profile-specific config file silently not be picked up by a client?**

Most commonly a filename mismatch — the file must be named exactly `{spring.application.name}-{profile}.yml`; a mismatch (e.g. wrong casing, missing hyphen) means the Config Server won't find it and the client silently falls back to defaults without any error.

### Connecting Clients & Refresh

**Q: How does a Spring Boot service become a Config Client?**

By adding the `spring-cloud-starter-config` dependency, declaring `spring.application.name`, and pointing at the server via `spring.cloud.config.uri` (or the newer `spring.config.import=configserver:...` syntax), typically alongside `spring.cloud.config.fail-fast=true` so startup aborts loudly if the Config Server is unreachable rather than silently running with incomplete config.

**Q: What does `@RefreshScope` actually do under the hood?**

It wraps the annotated bean in a special Spring scope backed by a proxy; when a refresh event occurs, the cached bean instance is discarded (not mutated), so the next invocation lazily re-creates it, re-reading `@Value`/`@ConfigurationProperties` fields from the now-updated `Environment` — enabling runtime configuration updates without restarting the JVM.

**Q: What's the difference between `/actuator/refresh` and `/actuator/busrefresh`?**

`/actuator/refresh` refreshes `@RefreshScope` beans on the single instance it's called against. `/actuator/busrefresh` broadcasts a `RefreshRemoteApplicationEvent` via Spring Cloud Bus (over RabbitMQ/Kafka) to every instance of every subscribed service at once, avoiding the need to call `/actuator/refresh` on each instance individually.

**Q: Why would a Config Client fail to start if the Config Server is temporarily down, and is that a problem?**

If `spring.cloud.config.fail-fast=true`, the client deliberately aborts startup rather than proceeding with missing/incomplete configuration — this is usually desirable because starting with wrong config (e.g. missing DB credentials) can cause worse, harder-to-diagnose failures downstream than a fast, visible startup failure.

### Security & Encryption

**Q: Why is Basic Auth alone not sufficient to protect sensitive configuration values?**

Basic Auth only controls who can call the Config Server's API — it does nothing to protect values already stored in plaintext in the Git repository (readable by anyone with repo access) nor data in transit without TLS. Genuine protection requires property-level encryption (`{cipher}`-prefixed values via `/encrypt`) plus TLS, in addition to authentication.

**Q: What is the difference between symmetric and asymmetric encryption in Spring Cloud Config?**

Symmetric encryption uses a single shared secret key (`encrypt.key`) for both encrypting and decrypting values, which is simple but means anyone with the key can do both operations. Asymmetric encryption uses an RSA key pair stored in a keystore — a public key encrypts values (safe to distribute more broadly) while only the private key (kept solely on the Config Server) can decrypt them, giving a stronger separation of duties.

**Q: How do you encrypt a value for storage in a Git-backed config file?**

Call `POST /encrypt` on the Config Server with the plaintext value, which returns a ciphertext; prefix that ciphertext with `{cipher}` and commit it as the property value. The Config Server automatically decrypts `{cipher}`-prefixed values server-side before returning them to authenticated clients.

**Q: Why does `/actuator/busrefresh` often need a CSRF exception configured?**

Spring Security enables CSRF protection by default for state-changing HTTP methods like POST, and `/actuator/busrefresh` is invoked via POST by automated tooling (CI/CD, scripts) rather than a browser session with a CSRF token, so without an explicit exemption for actuator endpoints, legitimate refresh calls are rejected with `403`.

**Q: Why restrict `/actuator/busrefresh`, `/encrypt`, and `/decrypt` to an ADMIN role instead of the general client credentials?**

Because these endpoints can trigger fleet-wide configuration reloads or reveal/manipulate cryptographic material, granting them to the same broad credentials used by every microservice for routine config-fetch would violate least-privilege — a compromised or misconfigured client could otherwise disrupt the whole system or probe encryption endpoints.

**Q: What historically caused `InvalidKeyException: Illegal key size` errors when configuring encryption?**

Older JDK 8 distributions shipped with restricted Java Cryptography Extension (JCE) policies limiting key strength (e.g. capping AES at 128-bit) unless the unlimited-strength policy files were installed. Modern JDKs (8u151+, and JDK 9+) enable unlimited strength by default, so this is now mostly a legacy concern tied to old base images.

### High Availability & Backends

**Q: How do you make the Config Server highly available?**

Run multiple stateless instances behind a load balancer or service registry, since the Config Server holds no meaningful persistent local state (its Git working copy is disposable and rebuildable). Clients can be pointed at a list of URLs for client-side failover, or use discovery-based lookup (`spring.cloud.config.discovery.enabled=true`) combined with retry settings.

**Q: Why is the Config Server well-suited to horizontal scaling without shared storage?**

Because every instance independently clones/pulls the same backing Git repository and serves requests statelessly — there's no session affinity or shared cache required, so any instance can answer any request as long as they're pointed at the same repository and branch.

**Q: What is a composite configuration backend, and when would you use one?**

A composite backend lets the Config Server query multiple `EnvironmentRepository` types simultaneously (e.g. Git + Vault + JDBC) and merge the results, with earlier-listed repositories taking precedence on key conflicts. It's used when different kinds of configuration naturally belong in different systems — for example non-sensitive settings in Git (reviewable) and secrets in Vault (rotatable, access-audited).

**Q: What's the trade-off of using the native/file-system backend instead of Git?**

The file-system backend is simpler and doesn't require Git infrastructure, but loses version history, pull-request review, and meaningful branch/label semantics — rollback and audit must be handled by whatever manages the files on disk instead.

**Q: How can you preview exactly what configuration a client will receive without starting the client application?**

By directly calling the Config Server's REST endpoints with `curl`, e.g. `GET /{application}/{profile}` for the full JSON `PropertySource` breakdown (useful for debugging merge precedence), or `GET /{application}-{profile}.yml` for the flattened format the client would actually receive.

**Q: Why might the Config Server's `/actuator/health` report DOWN even though the process is running?**

Because its built-in `HealthIndicator` actively verifies backend connectivity (e.g. resolving a default application against the Git remote), so if the Git repository is unreachable or credentials are invalid, health correctly reports as degraded/down even though the JVM itself is alive — a useful signal for orchestrators like Kubernetes to avoid routing traffic to a broken instance.



## Resources
