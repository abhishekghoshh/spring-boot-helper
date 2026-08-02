# Service Discovery

## Detailed Guide

### Spring cloud Discovery server or Eureka Naming Server

Eureka is Netflix's service discovery server, integrated into Spring Cloud as `spring-cloud-starter-netflix-eureka-server`. In a microservices architecture, service instances come and go dynamically due to scaling, deployments, and failures, so hard-coding hostnames and ports in configuration is brittle. Eureka solves this by acting as a phone book: every service instance registers itself with the Eureka server on startup, advertising its host, port, and metadata, and other services query Eureka to discover where to send requests instead of relying on static configuration.

The Eureka server itself is a plain Spring Boot application annotated with `@EnableEurekaServer`. It exposes a REST API and dashboard (by default on port 8761) that clients use to register, renew their lease via heartbeats, and fetch the registry. Internally, Eureka keeps an in-memory registry structured as a map of application name to a list of instances, and this registry is periodically synchronized to registered clients so that lookups are fast and don't require a round trip to the server for every call.

Eureka favors **availability over strict consistency** (an AP system in CAP terms). It would rather serve a slightly stale registry than reject requests, which is intentional for service discovery since a stale-but-mostly-correct list of instances is far more useful than no list at all during a network partition.

**Real-life scenario:** In an e-commerce platform with `order-service`, `inventory-service`, and `payment-service` each running multiple replicas behind auto-scaling, a dedicated Eureka server cluster lets every new pod register itself automatically the moment it starts, so `order-service` never needs a redeploy just because `payment-service` scaled from 3 to 5 instances.

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: true
```

```mermaid
flowchart LR
    subgraph Clients
        A[order-service instance] 
        B[inventory-service instance]
        C[payment-service instance]
    end
    subgraph Eureka["Eureka Naming Server"]
        R[(Service Registry)]
    end
    A -- register / heartbeat --> R
    B -- register / heartbeat --> R
    C -- register / heartbeat --> R
    A -- fetch registry --> R
    B -- fetch registry --> R
```

| Discovery Option | Consistency Model | Health Check | Typical Use Case |
|---|---|---|---|
| Eureka | AP (available, eventually consistent) | Client heartbeat / self-preservation | Cloud-native microservices, Netflix OSS stack |
| Consul | CP by default (Raft) | Active health checks (HTTP/TCP/script) | Multi-datacenter, service mesh integrations |
| ZooKeeper | CP (Zab consensus) | Ephemeral znodes | Coordination-heavy systems (Kafka, Hadoop) |

**Interview Q&A:**

**Q: Why is Eureka's registry described as a "phone book" rather than a DNS replacement?**
Unlike static DNS, Eureka's registry updates dynamically as instances register, renew, and deregister in near real-time, and it carries richer per-instance metadata (status, zone, custom attributes) that plain DNS records don't support.

**Q: What port does the Eureka dashboard/REST API run on by default, and why does the server itself set `register-with-eureka: false`?**
Port 8761 by default. A standalone Eureka server disables `register-with-eureka` and `fetch-registry` because it doesn't need to register with itself as a client — those flags are only meaningful for a peer-aware cluster or actual client applications.

### Problems with Eureka

While Eureka simplifies discovery, it introduces its own operational challenges. First, a single Eureka instance is a single point of failure — if it goes down, existing clients can still call each other using cached registries, but no new instances can register and stale entries won't be cleaned up. Second, Eureka's default lease expiration and eviction settings can lag reality: an instance that crashes ungracefully may still appear "UP" for up to 90 seconds (3 missed 30-second heartbeats) before being evicted, so a naive client could route traffic to a dead instance.

Another common problem is network partition handling. During a partition, Eureka nodes may see a sharp drop in the number of renewing clients and trigger self-preservation mode, which stops evicting instances even if they haven't sent heartbeats — this protects against a false mass-eviction, but if left misunderstood, teams sometimes see "phantom" instances lingering in the dashboard and get confused. Finally, in polyglot environments, teams need to make sure clients from other languages implement the Eureka REST protocol correctly, since not all client libraries handle registry rate-limiting or delta fetches identically.

**Real-life scenario:** A team notices that after a rolling deployment, old pod IPs remain visible in the Eureka dashboard for over a minute, causing a handful of 5xx errors from clients that picked a stale instance — understanding lease duration and eviction timing helps them tune `lease-expiration-duration-in-seconds` appropriately instead of panicking.

| Problem | Cause | Mitigation |
|---|---|---|
| Stale instance served | Slow eviction after crash | Tune lease renewal/expiration intervals, use client-side circuit breakers |
| Single point of failure | One Eureka node | Run a peer-aware cluster (2+ nodes) |
| Self-preservation confusion | Renewal threshold drop during partition/deploy storm | Understand it's a safety net, tune thresholds, monitor via dashboard |
| Registry propagation delay | Client registry cache refresh interval | Reduce `registry-fetch-interval-seconds` for faster convergence (trade-off: more load) |

**Interview Q&A:**

**Q: Why can a dead instance still appear "UP" in Eureka for up to 90 seconds?**
Because eviction only happens after the lease expiration duration (90 seconds by default, three missed 30-second heartbeats) elapses without a renewal — Eureka doesn't detect crashes instantly, it detects the absence of heartbeats.

**Q: How should client applications compensate for Eureka's slow eviction of dead instances?**
By layering client-side resilience such as connection timeouts, retries, and circuit breakers (e.g., Resilience4j) so a single stale instance returning errors or timing out doesn't cascade into a broader failure.

### Resilient Eureka Server with multiple instances and Data Replication Theory

To eliminate the single point of failure, Eureka servers are deployed as a peer-aware cluster where each node registers itself as a *client* of every other node. When any client registers, renews a lease, or cancels registration against one Eureka node, that node replicates the change to all its peers asynchronously. This means each Eureka server holds a full, independent copy of the registry — there's no leader/follower split-brain the way there is in Raft/Paxos-based systems; every node is symmetric.

This replication model is intentionally **eventually consistent**. Because replication happens over plain HTTP calls between peers, a registration on node A might take a moment to appear on node B. Eureka embraces this trade-off because the alternative — a strongly consistent registry requiring quorum writes — would reduce availability exactly when the network is unreliable, which is the worst time to lose service discovery. Clients also cache the registry locally and refresh periodically, adding another layer of eventual consistency but also resilience to a temporary Eureka outage.

**Real-life scenario:** Running a 3-node Eureka cluster across three availability zones means that if an entire AZ goes down, clients in the other two AZs continue to discover and call each other because the surviving Eureka nodes still hold a replicated copy of the registry.

```mermaid
sequenceDiagram
    participant C as Client Instance
    participant E1 as Eureka Node 1
    participant E2 as Eureka Node 2
    participant E3 as Eureka Node 3
    C->>E1: Register (host, port, metadata)
    E1-->>E2: Replicate registration
    E1-->>E3: Replicate registration
    C->>E1: Heartbeat (renew lease)
    E1-->>E2: Replicate renewal
    E1-->>E3: Replicate renewal
```

**Interview Q&A:**

**Q: Why doesn't a Eureka cluster use a consensus protocol like Raft or Paxos for replication?**
Because strong consistency via quorum writes would reduce availability precisely when the network is unreliable. Eureka trades strict consistency for availability, accepting brief replication lag in exchange for every node staying independently operational.

**Q: What happens to registry data on a Eureka node that restarts within a cluster?**
On startup it tries to fetch the current registry from its peers before accepting traffic, so it doesn't briefly serve an empty registry; if peers are unreachable it falls back to waiting for clients to re-register.

### Self-Preservation Mode in Eureka

Self-preservation is a protective mechanism where the Eureka server stops expiring instances from the registry when the percentage of renewals received drops below a configured threshold (85% by default) within a rolling window. The rationale is that a sudden drop in heartbeats is more likely caused by a network issue between clients and the server than by all those instances actually dying simultaneously, so evicting them all would be a worse outcome than temporarily trusting possibly-stale data.

When self-preservation kicks in, the Eureka dashboard displays a prominent warning banner ("EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP..."), and the server pauses eviction until renewals recover above the threshold. This is extremely common in local development and small clusters (fewer than ~3 instances), because the renewal threshold math doesn't have enough samples to behave predictably, which is why many teams disable it in dev/test with `eureka.server.enable-self-preservation=false` while keeping it enabled in production.

```mermaid
stateDiagram-v2
    [*] --> Normal
    Normal --> SelfPreservation: renewal rate < 85% threshold
    SelfPreservation --> Normal: renewal rate recovers
    Normal --> Normal: evict expired leases normally
    SelfPreservation --> SelfPreservation: no eviction, keep all leases
```

**Real-life scenario:** During a chaotic deployment where 40% of instances restart within a short window, self-preservation prevents Eureka from wrongly wiping out the entire registry, avoiding a cascading outage where healthy services suddenly can't find anyone to call.

| Mode | Behavior | Risk if Misconfigured |
|---|---|---|
| Self-preservation enabled (default, prod) | Stops evicting instances when renewals drop below threshold | May briefly route to a truly dead instance; mitigated by client-side timeouts/circuit breakers |
| Self-preservation disabled (common in dev) | Evicts instances strictly based on lease expiration | In small/unstable networks, can cause mass false-eviction and registry flapping |

```yaml
eureka:
  server:
    enable-self-preservation: false   # typical for single-node local dev
    renewal-percent-threshold: 0.85
    eviction-interval-timer-in-ms: 60000
```

**Interview Q&A:**

**Q: What renewal threshold triggers self-preservation mode by default?**
When the number of renewals received in the last minute drops below 85% of the expected number (based on registered instance count), the server enters self-preservation and stops evicting instances.

**Q: Should self-preservation be disabled in production?**
Generally no — it should stay enabled in production since it's a genuine safety net against mass false-eviction during transient network issues; it's typically only disabled in small local/dev setups where the renewal math is unreliable due to low instance counts.

### Registering Eureka Clients And Sending Request

A Eureka client is any Spring Boot application that includes `spring-cloud-starter-netflix-eureka-client` and is annotated with `@EnableDiscoveryClient` (or simply has the dependency on the classpath, since Spring Boot auto-configures it). On startup, the client reads its `spring.application.name`, host, and port, and sends a `POST` registration request to the Eureka server's `/eureka/apps/{APP-NAME}` endpoint. From then on, it sends periodic heartbeats (`PUT` renewals) every 30 seconds by default to keep its lease alive, and on graceful shutdown it sends a `DELETE` request to deregister immediately.

Once registered, a client also acts as a discovery consumer: it downloads the full registry on startup and refreshes it on an interval (30 seconds by default), caching it locally. When code needs to call another service, it uses the logical application name (e.g., `payment-service`) rather than a physical host and port, and the client-side infrastructure (`DiscoveryClient`, Feign, or `RestTemplate` with `@LoadBalanced`) resolves that name to one of the currently registered, healthy instances.

**Real-life scenario:** A `checkout-service` calls `inventory-service` purely by name; when `inventory-service` is redeployed with new pod IPs, `checkout-service` needs zero configuration changes because it always resolves the name against the live Eureka registry.

```yaml
spring:
  application:
    name: checkout-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

```java
@SpringBootApplication
@EnableDiscoveryClient
public class CheckoutServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckoutServiceApplication.class, args);
    }
}
```

**Interview Q&A:**

**Q: What HTTP verbs does a Eureka client use over its registration lifecycle?**
`POST` to register on startup, `PUT` for periodic heartbeat renewals, and `DELETE` to deregister on graceful shutdown.

**Q: Why set `eureka.instance.prefer-ip-address: true` in containerized environments?**
Containers often have unstable or non-resolvable hostnames, so advertising the IP address instead of the hostname ensures other services can actually reach the instance rather than failing DNS resolution.

### Eureka Client Library

The `spring-cloud-starter-netflix-eureka-client` starter bundles the Netflix `eureka-client` library along with Spring Cloud's auto-configuration glue. It provides the `EurekaClient` / `com.netflix.discovery.EurekaClient` API for programmatic registry access, plus Spring abstractions like `DiscoveryClient` (Spring Cloud Commons) that abstract over Eureka, Consul, or Zookeeper so application code isn't tied to a specific discovery implementation.

Under the hood, the library manages a background scheduler for heartbeats and registry fetches, an in-memory cache of the registry (refreshed via delta updates rather than full downloads after the initial fetch, to save bandwidth), and hooks into the Spring lifecycle so that registration happens after the web server has actually started listening (avoiding a race where Eureka advertises an instance before it can accept traffic).

```java
@RestController
public class InstanceInfoController {

    private final DiscoveryClient discoveryClient;

    public InstanceInfoController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @GetMapping("/instances/{serviceId}")
    public List<ServiceInstance> instances(@PathVariable String serviceId) {
        return discoveryClient.getInstances(serviceId);
    }
}
```

**Real-life scenario:** A platform team builds an internal admin tool that uses the generic `DiscoveryClient` API to list all registered services and their instance counts, and the same code works unchanged whether the backing registry is Eureka in production or Consul in a newer cluster.

**Interview Q&A:**

**Q: What is the difference between Netflix's `EurekaClient` API and Spring Cloud Commons' `DiscoveryClient`?**
`EurekaClient` is Eureka-specific and exposes Netflix's native API surface, while `DiscoveryClient` is a Spring Cloud abstraction that works uniformly across Eureka, Consul, or Zookeeper, letting application code stay decoupled from a specific discovery implementation.

**Q: Why does the client fetch registry deltas instead of the full registry after the initial download?**
Delta fetches reduce bandwidth and processing overhead on both the client and server, since only changes since the last fetch need to be transmitted rather than the entire registry on every refresh cycle.

### Eureka Client Health Check Configuration

By default, Eureka's notion of "instance health" is just whether the client is sending heartbeats — it doesn't know if the application itself is actually healthy (e.g., its database connection pool exhausted). Spring Boot Actuator can be wired in so Eureka reflects real application health: setting `eureka.client.healthcheck.enabled=true` makes the Eureka client delegate to Actuator's `/actuator/health` endpoint, and if that reports `DOWN`, the client actively tells Eureka the instance status is `DOWN` instead of waiting for lease expiration.

This is important because without it, a service whose health indicator fails (say, a broken downstream dependency) would still show as `UP` in Eureka and keep receiving traffic until an operator intervenes, defeating the purpose of health checks. Combining Actuator health indicators with Eureka health propagation lets the registry reflect true application readiness, not just process liveness.

```yaml
eureka:
  client:
    healthcheck:
      enabled: true
  instance:
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30

management:
  endpoint:
    health:
      show-details: always
  health:
    db:
      enabled: true
```

**Real-life scenario:** When `inventory-service`'s database connection pool is exhausted, its Actuator health indicator flips to `DOWN`, Eureka immediately marks the instance `DOWN`, and Feign clients stop routing new requests to it within seconds instead of waiting up to 90 seconds for a lease to expire.

**Interview Q&A:**

**Q: Without `eureka.client.healthcheck.enabled=true`, what does Eureka's "UP" status actually reflect?**
Only that the client is currently sending heartbeats — it says nothing about whether the application's internal dependencies (database, downstream services) are actually functioning correctly.

**Q: What is a risk of enabling Actuator health propagation to Eureka without care?**
If a non-critical health indicator (e.g., a rarely-used external dependency) is included in the aggregate health status, it could mark the whole instance `DOWN` and remove it from routing even though its core functionality is fine — health indicators should be scoped deliberately.

### Load Balancing with Eureka, Feign & Spring Cloud LoadBalancer

Service discovery is only half the story — once multiple instances of a service are known, something needs to pick which one to call. Spring Cloud LoadBalancer is the client-side load balancer that plugs into `DiscoveryClient`: it fetches the list of instances for a logical service name from Eureka and applies a strategy (round-robin by default) to select one for each outgoing call. This replaced the older Netflix Ribbon library, which is now in maintenance mode.

Feign, Spring Cloud's declarative REST client, integrates directly with Spring Cloud LoadBalancer: instead of hardcoding a URL, a Feign client interface is declared with just the logical service name, and Feign resolves it to a real instance transparently on every call, load-balancing across all healthy registered instances. `RestTemplate` can also be made load-balancing-aware simply by adding `@LoadBalanced` to its `@Bean` definition, which causes it to resolve service names the same way.

```java
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory/{sku}")
    InventoryResponse getStock(@PathVariable("sku") String sku);
}
```

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

```mermaid
flowchart TD
    A[checkout-service] -->|"1. resolve 'inventory-service'"| B[Spring Cloud LoadBalancer]
    B -->|"2. query instances"| C[(Eureka Registry)]
    C -->|"3. instance list"| B
    B -->|"4. pick instance round-robin"| D[inventory-service #1]
    B -.->|alternative pick| E[inventory-service #2]
    A -->|"5. HTTP call via Feign"| D
```

| Mechanism | Style | Load Balancing | Status |
|---|---|---|---|
| Feign | Declarative interface-based client | Built-in via Spring Cloud LoadBalancer | Recommended |
| `RestTemplate` + `@LoadBalanced` | Imperative, manual calls | Via `LoadBalancerInterceptor` | Supported, more boilerplate |
| Netflix Ribbon | Imperative | Client-side rules | Deprecated / maintenance mode |

**Real-life scenario:** `checkout-service` calls `inventory-service` through a Feign client; when traffic spikes and the platform scales `inventory-service` from 2 to 6 pods, no code or config changes are needed — new instances are picked up automatically and load spreads across all 6.

**Interview Q&A:**

**Q: Why was Netflix Ribbon replaced by Spring Cloud LoadBalancer?**
Ribbon entered maintenance mode and lacked first-class reactive support; Spring Cloud LoadBalancer is actively maintained, integrates cleanly with both blocking (`RestTemplate`) and reactive (`WebClient`) clients, and has simpler configuration.

**Q: What load-balancing strategy does Spring Cloud LoadBalancer use by default?**
A round-robin strategy across the healthy instances returned by the `DiscoveryClient` for a given service name, though custom `ServiceInstanceListSupplier` implementations can plug in weighted, zone-aware, or other strategies.

### Reactive Way using Feign Reactive

Traditional Feign clients are blocking — each call occupies a thread until the HTTP response returns, which doesn't fit well into a reactive, non-blocking stack built on Spring WebFlux and Project Reactor. Reactive Feign (a community project, `feign-reactor`) and, in newer Spring Cloud versions, native support layered over `WebClient`, allow Feign-style declarative clients to return `Mono<T>` or `Flux<T>` instead of plain objects, so calls integrate into a fully non-blocking pipeline without dedicating a thread per in-flight request.

This matters most for high-concurrency services that need to minimize thread usage, such as an API gateway aggregating calls to several downstream services. Combining reactive Feign with Eureka-backed load balancing means the reactive `WebClient` still needs a `ReactorLoadBalancerExchangeFilterFunction` (Spring Cloud LoadBalancer's reactive support) to resolve the logical service name against the registry before issuing the non-blocking call.

```java
@FeignClient(name = "inventory-service")
public interface ReactiveInventoryClient {

    @GetMapping("/api/inventory/{sku}")
    Mono<InventoryResponse> getStock(@PathVariable("sku") String sku);
}
```

```java
@Bean
@LoadBalanced
public WebClient.Builder loadBalancedWebClientBuilder() {
    return WebClient.builder();
}
```

**Real-life scenario:** An API gateway aggregating responses from `pricing-service`, `inventory-service`, and `review-service` in parallel uses reactive Feign clients so a burst of 10,000 concurrent requests doesn't require 10,000 blocked threads, keeping memory and context-switching overhead low.

**Interview Q&A:**

**Q: Why don't blocking Feign clients scale well inside a WebFlux application?**
Each blocking call ties up a thread until the response returns, which defeats WebFlux's small, fixed-size event-loop thread model and can exhaust threads under high concurrency, negating the benefits of going reactive in the first place.

**Q: What component resolves a logical service name for a reactive `WebClient` before the non-blocking call is issued?**
`ReactorLoadBalancerExchangeFilterFunction`, Spring Cloud LoadBalancer's reactive integration, intercepts the request and resolves the service name against the registry before the actual non-blocking HTTP call is dispatched.

### Configure Spring Security to Eureka Server

By default, a Eureka server exposes its dashboard and REST API with no authentication, which is unacceptable outside of an isolated local/dev network. Adding `spring-boot-starter-security` to the Eureka server project brings Spring Security onto the classpath, which by default locks down *all* endpoints, including Eureka's own registration API, with basic authentication and a randomly generated password unless explicitly configured.

The goal is to require credentials for both the human-facing dashboard and the machine-facing registration/heartbeat API, while keeping the actual security configuration simple since Eureka's server itself isn't meant to do fine-grained authorization — it's typically protected with a single shared username/password pair for all Eureka clients, sitting behind network-level controls for anything more sensitive.

```yaml
spring:
  security:
    user:
      name: eureka-admin
      password: "${EUREKA_SERVER_PASSWORD}"
```

**Real-life scenario:** Without security, any pod on the network segment could register itself as a fake `payment-service` instance and intercept traffic; requiring a shared credential for registration closes off this trivial spoofing vector.

**Interview Q&A:**

**Q: What happens if `spring-boot-starter-security` is added to a Eureka server without any explicit user configuration?**
Spring Boot auto-generates a random password logged at startup for a default `user` account, which is fine for a quick smoke test but unusable operationally since the password changes every restart — explicit credentials must be configured.

**Q: Is per-client fine-grained authorization typical for a Eureka server?**
No — Eureka servers are usually protected with a single shared credential for all clients rather than per-service authorization, with finer access control left to network segmentation or a service mesh layer.

### Enable Web Security in Eureka

Simply adding the security starter isn't enough — Spring Security's default behavior can interfere with Eureka's own client-to-server communication, which uses HTTP Basic auth but doesn't handle CSRF tokens the way browser-based forms do. A dedicated `SecurityFilterChain` bean is needed to require authentication for all requests while disabling CSRF protection specifically for the `/eureka/**` paths, since Eureka clients are non-browser HTTP clients that can't obtain or submit CSRF tokens.

```java
@Configuration
@EnableWebSecurity
public class EurekaServerSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

**Real-life scenario:** Without disabling CSRF on `/eureka/**`, Eureka clients would receive `403 Forbidden` responses when trying to register or renew leases, because they can't supply the CSRF token that Spring Security expects for state-changing POST/PUT requests by default.

**Interview Q&A:**

**Q: Why is CSRF protection disabled specifically for `/eureka/**` rather than globally?**
Disabling CSRF globally would remove protection for any browser-facing endpoints the application might expose; scoping the exemption to `/eureka/**` keeps CSRF defenses intact everywhere except the machine-to-machine registration API that can't supply CSRF tokens.

**Q: What authentication scheme does the example `SecurityFilterChain` enforce for Eureka server requests?**
HTTP Basic authentication (`httpBasic(Customizer.withDefaults())`) combined with requiring authentication on every request via `anyRequest().authenticated()`.

### Configure Eureka Clients to use Username and Password

Once the Eureka server requires authentication, every client must embed credentials in its `defaultZone` service URL, since Eureka's client library authenticates using HTTP Basic auth encoded directly into the registration endpoint URL. The credentials should never be hardcoded in plaintext in version control — they're typically injected via environment variables or pulled from Config Server/Vault.

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_USERNAME}:${EUREKA_PASSWORD}@localhost:8761/eureka/
```

**Real-life scenario:** A new microservice fails to register with "401 Unauthorized" in its startup logs until the ops team realizes the `EUREKA_USERNAME`/`EUREKA_PASSWORD` environment variables weren't propagated to its container, illustrating why centralizing these credentials (rather than copy-pasting per service) reduces this class of misconfiguration.

**Interview Q&A:**

**Q: How does a Eureka client pass credentials to a secured server?**
By embedding them directly in the `defaultZone` URL using standard HTTP Basic Auth URL syntax (`http://user:password@host:port/eureka/`), which the Eureka client library parses and uses for every registration/renewal request.

**Q: Why is hardcoding credentials in `defaultZone` in version control a bad practice?**
Anyone with read access to the repository would see the plaintext password; credentials should instead come from environment variables, a secrets manager, or an encrypted value resolved via Config Server.

### Configure Eureka Service URL in Config Server

Rather than duplicating the Eureka `defaultZone` URL (and credentials) in every microservice's local `application.yml`, teams centralize it in Spring Cloud Config Server. Each client's `bootstrap.yml`/`application.yml` only needs to point at the Config Server, and the actual `eureka.client.service-url.defaultZone` value is resolved from a shared configuration file (e.g., `application.yml` in the Config Server's Git-backed repository) that all services inherit.

This means changing the Eureka cluster address (say, moving from a 1-node dev cluster to a 3-node cluster) requires updating configuration in one place instead of redeploying every microservice, and combined with Spring Cloud Bus, the change can even be pushed to running instances without a restart.

```yaml
# config-repo/application.yml (shared by all services via Config Server)
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka2:8762/eureka/,http://eureka3:8763/eureka/
```

**Real-life scenario:** When migrating Eureka from a single node to a highly available 3-node cluster, updating one shared file in the config repository automatically propagates the new cluster addresses to dozens of microservices without touching their individual repositories.

**Interview Q&A:**

**Q: What problem does centralizing `eureka.client.service-url.defaultZone` in Config Server solve?**
It eliminates the need to duplicate (and keep in sync) the same Eureka cluster address across every microservice's local configuration, so infrastructure changes require editing one shared file instead of many repositories.

**Q: How can a centralized Eureka URL change be propagated to already-running services without a restart?**
By combining Config Server with Spring Cloud Bus, which broadcasts a refresh event over a message broker to all subscribed instances, triggering them to re-fetch configuration without a redeploy.

### Move Username and Password to Config Server

Extending centralization further, the Eureka Basic Auth credentials themselves are stored in the Config Server's backing repository rather than as plain environment variables on each service. This keeps sensitive values in one governed location (often a private Git repo or Vault-backed backend) instead of scattered across dozens of deployment manifests, making rotation and auditing far simpler.

```yaml
# config-repo/application.yml
eureka:
  instance:
    hostname: localhost
  client:
    service-url:
      defaultZone: http://${eureka.username}:${eureka.password}@localhost:8761/eureka/

eureka:
  username: eureka-admin
  password: "{cipher}AQBx...encrypted-value..."
```

**Real-life scenario:** During a security audit, rotating the Eureka credentials becomes a single commit to the config repository followed by a Spring Cloud Bus refresh event, instead of a coordinated redeploy of every consuming microservice.

**Interview Q&A:**

**Q: Why move Eureka credentials into the Config Server's backing repository instead of leaving them as per-service environment variables?**
It consolidates sensitive values into one governed location, making rotation, auditing, and access control far simpler than tracking credentials scattered across dozens of deployment manifests.

**Q: What risk remains if credentials are stored in Config Server's repository as plain values?**
Anyone with read access to that repository (even a private one) can see the plaintext password, which is why the next step is typically encrypting the value rather than storing it as-is.

### Encrypting Username and Password

Storing credentials in Config Server as plaintext, even in a private repository, is still a risk — anyone with repo read access sees the password. Spring Cloud Config supports symmetric encryption: a shared secret key (`encrypt.key`) is configured on the Config Server, and sensitive values are encrypted using the `/encrypt` endpoint, then stored with a `{cipher}` prefix. The Config Server automatically decrypts these values before serving them to clients, so client applications never need to know about encryption at all.

```bash
# Encrypt a password via the Config Server's /encrypt endpoint
curl -X POST --data-urlencode "eureka-secret-password" \
  http://localhost:8888/encrypt

# Response is the ciphertext to store with a {cipher} prefix
# eureka.password: '{cipher}3af9c1...'
```

```yaml
encrypt:
  key: "${CONFIG_SERVER_ENCRYPT_KEY}"
```

| Approach | Security | Operational Complexity | Key Rotation |
|---|---|---|---|
| Plaintext in config repo | Low — anyone with repo access sees secrets | Lowest | Manual find/replace |
| Symmetric encryption (`{cipher}`) | Moderate — protects repo-at-rest, single shared key | Low — Config Server decrypts transparently | Requires re-encrypting all values on key rotation |
| Asymmetric (JCE keystore) | High — public/private key pair, supports key rotation policies | Higher — keystore management | Cleaner rotation via key aliases |
| External vault (HashiCorp Vault) | Highest — dynamic secrets, audit trail, leasing | Highest — additional infrastructure | Native support for rotation and revocation |

**Real-life scenario:** A compliance requirement mandates that no plaintext secrets exist in any Git repository; encrypting the Eureka password with Config Server's symmetric encryption satisfies the audit while requiring no changes to how client applications consume the configuration.

**Interview Q&A:**

**Q: How does the Config Server know to decrypt a `{cipher}`-prefixed value before serving it?**
The `{cipher}` prefix signals to the Config Server that the value is encrypted; it uses the configured `encrypt.key` to decrypt it transparently server-side before returning the resolved configuration to the requesting client, so clients never handle ciphertext.

**Q: What's the trade-off between symmetric encryption and an external vault like HashiCorp Vault for Eureka credentials?**
Symmetric encryption is simple to operate but relies on a single shared key and requires re-encrypting values on key rotation; a vault provides dynamic secrets, leasing, and audit trails at the cost of additional infrastructure to run and maintain.

### Eureka Cluster and Peer Awareness Configuration

Peer awareness is what turns a set of independent Eureka servers into a resilient cluster. Each Eureka node is configured with its *own* hostname and, critically, with `eureka.client.service-url.defaultZone` pointing at the *other* peer nodes (not itself), which makes each server register with its peers as if it were a client. This is what enables the replication behavior described earlier — every node knows about every other node and pushes/pulls registry changes to/from them.

A common mistake is having every node point at `localhost` for its peer list, which effectively creates isolated single-node "clusters" that never actually replicate anything. Each node's `defaultZone` must list the *actual* hostnames of its peers so replication traffic can reach them over the network.

```yaml
# eureka-peer1 (application-peer1.yml)
eureka:
  instance:
    hostname: peer1
  client:
    service-url:
      defaultZone: http://peer2:8762/eureka/,http://peer3:8763/eureka/

# eureka-peer2 (application-peer2.yml)
eureka:
  instance:
    hostname: peer2
  client:
    service-url:
      defaultZone: http://peer1:8761/eureka/,http://peer3:8763/eureka/
```

**Real-life scenario:** A team that copy-pasted the same `defaultZone: http://localhost:8761/eureka/` across all three "cluster" nodes discovers during a failover drill that killing one node causes total registry loss for its clients, because the nodes were never actually peer-aware in the first place.

**Interview Q&A:**

**Q: What is the single most common mistake when configuring a Eureka peer cluster?**
Pointing every node's `defaultZone` at `localhost` instead of the actual peer hostnames, which silently creates isolated single-node instances that never replicate with each other despite appearing to run correctly.

**Q: Does a Eureka node register itself in its own `defaultZone`?**
No — each node's `defaultZone` should list only its *peer* nodes, not itself; a node registering with itself doesn't achieve peer awareness or replication.

### Eureka Cluster: Update Hosts File

Since each Eureka node's peer configuration references peer hostnames (like `peer1`, `peer2`, `peer3`) rather than IP addresses, local development and on-prem VM setups often need those hostnames resolvable. Editing `/etc/hosts` (or `C:\Windows\System32\drivers\etc\hosts` on Windows) to map each peer hostname to `127.0.0.1` (when running multiple instances on one machine) or to the correct internal IP lets the peers reach each other without relying on a full DNS setup.

```bash
# /etc/hosts — running a 3-node Eureka cluster locally
127.0.0.1   peer1
127.0.0.1   peer2
127.0.0.1   peer3
```

```bash
# Apply changes immediately (macOS/Linux) — usually no extra step needed,
# but flush DNS cache if resolution seems stale
sudo dscacheutil -flushcache
```

**Real-life scenario:** Running a 3-node Eureka cluster on a single developer laptop for testing peer replication requires distinct hostnames mapped in `/etc/hosts` so each Eureka instance (bound to a different port) can address its peers by name exactly as it would in a real multi-host deployment.

**Interview Q&A:**

**Q: Why use hostnames instead of `localhost` directly for each peer in a single-machine test cluster?**
Using distinct hostnames (all mapped to `127.0.0.1` in `/etc/hosts`) mirrors how peer configuration would look in a real multi-host deployment, making the local test setup representative of production behavior.

**Q: In a Kubernetes deployment, what replaces manual `/etc/hosts` editing for peer resolution?**
A headless Service providing stable per-pod DNS names, so peers can address each other by predictable DNS entries without any manual host-file management.

### Staring up Eureka Discovery Server Cluster

Starting a Eureka cluster means launching each peer node with its own Spring profile (or distinct config file) so it binds to the correct port and points at the correct peers. In practice this is done either by running the same JAR multiple times with different `--spring.profiles.active` flags, or via separate containers/pods in Docker/Kubernetes, each with environment-specific configuration.

```bash
# Start peer1 on port 8761
java -jar eureka-server.jar --spring.profiles.active=peer1

# Start peer2 on port 8762 (separate terminal/host)
java -jar eureka-server.jar --spring.profiles.active=peer2

# Start peer3 on port 8763 (separate terminal/host)
java -jar eureka-server.jar --spring.profiles.active=peer3
```

```mermaid
sequenceDiagram
    participant P1 as Eureka Peer1 (8761)
    participant P2 as Eureka Peer2 (8762)
    participant P3 as Eureka Peer3 (8763)
    P1->>P2: Register as peer client
    P1->>P3: Register as peer client
    P2->>P1: Register as peer client
    P2->>P3: Register as peer client
    P3->>P1: Register as peer client
    P3->>P2: Register as peer client
    Note over P1,P3: Cluster fully peer-aware, ready to replicate registry changes
```

**Real-life scenario:** In a Kubernetes deployment, three Eureka pods behind a headless service each start with an environment variable pointing at the other two pods' stable DNS names, forming a resilient cluster that survives the loss of any single pod.

**Interview Q&A:**

**Q: What is the purpose of `--spring.profiles.active` when starting each Eureka peer?**
It selects a profile-specific configuration file (e.g., `application-peer1.yml`) so each node binds to its own port/hostname and points at the correct set of peers, without needing separate JARs or codebases.

**Q: Why does the startup sequence diagram show each peer registering with every other peer?**
Because true peer awareness is symmetric — every node must know about and replicate to every other node, not just one designated node, so there's no single leader whose failure would break the cluster.

### Eureka Server Dashboard: Checking Registered Peers

Each Eureka node's dashboard (`http://<host>:<port>/`) shows a "DS Replicas" panel listing the other nodes it considers its peers, alongside the "Instances currently registered with Eureka" table showing every registered application and instance. Checking this panel after startup is the simplest way to confirm peer awareness actually worked — if a node's DS Replicas list is empty or incorrect, replication silently isn't happening even though the server appears to run fine.

The dashboard also surfaces general server health: uptime, renewal threshold, whether self-preservation is active, and the current renewals-per-minute versus the expected threshold, which is the first place to check when diagnosing why an instance appears in an unexpected state.

**Real-life scenario:** After deploying a new Eureka node into an existing cluster, an engineer checks the dashboard's "DS Replicas" section on all three nodes to confirm the new node is visible everywhere before routing any production traffic through it.

**Interview Q&A:**

**Q: What does an empty or incomplete "DS Replicas" panel indicate?**
That peer awareness/replication configuration is broken for that node — even though the Eureka server process runs fine and may still serve its own registry, it isn't actually syncing with its intended peers.

**Q: Besides DS Replicas, what other information does the Eureka dashboard surface for diagnosing issues?**
Server uptime, the renewal threshold, whether self-preservation mode is currently active, and the current renewals-per-minute compared to the expected threshold.

### Eureka Discovery Client with Default Configuration

When a Eureka client is added to a Spring Boot project with no explicit `eureka.client.service-url.defaultZone` configured, it falls back to the default value of `http://localhost:8761/eureka/`. This "just works" convention is convenient for local development against a single Eureka instance on the default port, but it also means a misconfigured or forgotten override in other environments silently tries to reach `localhost`, which will simply fail to connect rather than raise a clear configuration error.

Other client defaults worth knowing: `lease-renewal-interval-in-seconds` defaults to 30, `lease-expiration-duration-in-seconds` defaults to 90, and `registry-fetch-interval-seconds` defaults to 30 — these defaults balance registry freshness against network chatter and are usually fine for production but are often tightened in demos to make registration/eviction visibly faster.

```yaml
# No eureka.client.service-url.defaultZone specified —
# client defaults to http://localhost:8761/eureka/
spring:
  application:
    name: demo-service
```

**Real-life scenario:** A developer's integration test suite mysteriously hangs trying to reach `localhost:8761` in a CI pipeline where no Eureka server runs — the fix is either providing `eureka.client.enabled=false` for tests or spinning up a lightweight Eureka test server, since the default `service-url` silently assumed a local instance exists.

**Interview Q&A:**

**Q: What is the default `eureka.client.service-url.defaultZone` if none is configured?**
`http://localhost:8761/eureka/` — convenient for local development but a silent trap in other environments where no Eureka server runs at that address.

**Q: How can tests avoid unnecessary Eureka registration attempts?**
By setting `eureka.client.enabled=false` in the test configuration, which disables the Eureka client entirely instead of letting it try (and fail or hang) to reach a non-existent local server.

### Register Eureka Client with Eureka Cluster

When a Eureka server is a peer-aware cluster, clients should be configured with the **full list** of peer URLs in `defaultZone`, comma-separated, rather than just one node. This lets the Eureka client library fail over automatically: if the first URL in the list is unreachable, the client tries the next one, so a single Eureka node outage doesn't prevent new registrations or heartbeats.

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://peer1:8761/eureka/,http://peer2:8762/eureka/,http://peer3:8763/eureka/
    register-with-eureka: true
    fetch-registry: true
```

```mermaid
flowchart TD
    S[order-service] -->|try peer1 first| P1[Eureka Peer1]
    S -.->|peer1 down, fail over| P2[Eureka Peer2]
    S -.->|peer1 & peer2 down| P3[Eureka Peer3]
    P1 <--> P2
    P2 <--> P3
    P1 <--> P3
```

**Real-life scenario:** During a rolling restart of the Eureka cluster itself (e.g., for a version upgrade), client services configured with all three peer URLs keep registering and renewing leases uninterrupted because they transparently fail over to whichever peer is currently available.

**Interview Q&A:**

**Q: What happens if a client is configured with only one Eureka node's URL and that node goes down for maintenance?**
The client can't register or renew its lease against that node, and since no fallback URL exists, discovery for that client is disrupted until the node returns or its configuration is updated to include other peers.

**Q: Does the order of URLs in a comma-separated `defaultZone` list matter?**
The client tries them in order, using the first reachable one, but since all peers hold an equivalent replicated registry, the choice of which one ends up serving the client doesn't affect correctness — only initial connection latency.

## Interview Questions & Answers

### Eureka Fundamentals

**Q: What problem does Eureka (service discovery) solve in a microservices architecture?**

It removes the need to hard-code hostnames and ports of dependent services. As instances scale up/down or get rescheduled with new IPs, each service registers itself with Eureka on startup, and callers resolve a logical service name to a currently healthy instance at call time instead of relying on static configuration.

**Q: Is Eureka a CP or AP system in terms of the CAP theorem, and why does that matter?**

Eureka is AP — it favors availability over strict consistency. During a network partition, it would rather serve a possibly stale registry than reject discovery requests entirely, because for service discovery, a mostly-correct answer is far more useful than no answer.

**Q: What is the role of `@EnableEurekaServer` versus `@EnableDiscoveryClient`?**

`@EnableEurekaServer` turns a Spring Boot application into the Eureka registry itself. `@EnableDiscoveryClient` (largely automatic since Spring Boot auto-configures it when the client starter is present) marks an application as a client that registers itself with, and discovers other services from, a Eureka server.

**Q: What is the default heartbeat/lease renewal interval, and what happens if a client stops sending heartbeats?**

By default, clients renew their lease every 30 seconds, and the lease expires after 90 seconds without a renewal (`lease-expiration-duration-in-seconds`). If heartbeats stop, the server eventually evicts the instance from the registry once the lease expires, unless self-preservation mode is active.

### Problems and Resilience

**Q: What are the main operational risks of running Eureka?**

A single Eureka node is a single point of failure; eviction timing means a crashed instance can appear "UP" for up to 90 seconds; and self-preservation, if misunderstood, can make stale instances linger in the registry during deploy storms or network partitions.

**Q: How does Eureka achieve high availability?**

By running a peer-aware cluster of two or more Eureka nodes, each configured to know about the others. Every registration, renewal, or cancellation received by one node is asynchronously replicated to its peers, so any node can serve a (eventually consistent) copy of the full registry.

**Q: Is Eureka's peer replication synchronous or asynchronous, and what trade-off does that represent?**

It's asynchronous, which means replication is eventually consistent rather than strongly consistent. The trade-off favors availability and low latency for registration/renewal over guaranteeing every peer has the absolute latest state at every instant.

**Q: What is self-preservation mode and when does it activate?**

It's a safety mechanism where Eureka stops evicting instances when the percentage of renewals received falls below a threshold (85% by default) within a rolling window, on the assumption that a sharp heartbeat drop is more likely a network problem than mass instance failure.

**Q: Why is self-preservation often disabled in local development?**

With very few instances and frequent restarts, the renewal-percentage math doesn't have enough samples to be meaningful, so self-preservation triggers easily and clutters the dashboard with warnings; disabling it (`eureka.server.enable-self-preservation=false`) keeps local eviction behavior predictable.

### Registration, Health, and Load Balancing

**Q: What API calls does a Eureka client make against the server over its lifecycle?**

Registration (`POST`) on startup, periodic heartbeat/renewal (`PUT`) while running, and deregistration (`DELETE`) on graceful shutdown. It also periodically fetches the registry (initially a full fetch, then incremental deltas).

**Q: How can Eureka reflect actual application health rather than just process liveness?**

By enabling `eureka.client.healthcheck.enabled=true`, which wires the Eureka client to Spring Boot Actuator's `/actuator/health` endpoint, so if a health indicator (e.g., database connectivity) reports `DOWN`, the client actively updates its status in Eureka instead of waiting for lease expiration.

**Q: How does Spring Cloud LoadBalancer differ from the older Netflix Ribbon?**

Spring Cloud LoadBalancer is the modern, actively maintained client-side load balancer that integrates with `DiscoveryClient` and reactive `WebClient`. Ribbon is in maintenance mode and was Netflix's original client-side load balancer, superseded because Spring Cloud LoadBalancer has better reactive support and simpler configuration.

**Q: How does a Feign client resolve a logical service name to a real network address?**

Feign delegates name resolution to Spring Cloud LoadBalancer, which queries the registered `DiscoveryClient` (backed by Eureka) for the current instance list of that service name and applies a selection strategy (round-robin by default) to pick one instance per call.

**Q: Why would you use reactive Feign or `WebClient` instead of a blocking Feign client?**

In a non-blocking, WebFlux-based application, blocking calls would tie up threads waiting for I/O, negating the benefits of reactive programming. Reactive Feign or `WebClient` returns `Mono`/`Flux` so calls compose into a non-blocking pipeline, which matters most under high concurrency.

### Security

**Q: Why does adding `spring-boot-starter-security` to a Eureka server require additional configuration for clients to keep working?**

Spring Security secures all endpoints by default and enforces CSRF protection, which breaks Eureka's non-browser client registration calls. A custom `SecurityFilterChain` is needed to require Basic Auth for all requests while explicitly disabling CSRF for `/eureka/**`.

**Q: How do Eureka clients authenticate against a secured Eureka server?**

By embedding credentials directly in the `defaultZone` service URL using HTTP Basic Auth syntax, e.g., `http://user:password@host:port/eureka/`, since the Eureka client library extracts and uses these credentials for every registration/renewal call.

**Q: Why centralize the Eureka service URL and credentials in Config Server rather than each service's local configuration?**

It avoids duplicating (and potentially drifting) the same URL and secret across every microservice. A single change in the shared config repository — e.g., adding a new cluster node or rotating a password — propagates to all consuming services without redeploying each one individually.

**Q: How does Spring Cloud Config's symmetric encryption protect stored credentials?**

A shared secret key (`encrypt.key`) configured on the Config Server is used to encrypt sensitive values via its `/encrypt` endpoint; the encrypted value is stored with a `{cipher}` prefix in the repository, and the Config Server transparently decrypts it before serving configuration to clients, so plaintext secrets never live in version control.

### Cluster Configuration

**Q: What is "peer awareness" in a Eureka cluster, and what is the most common misconfiguration?**

Peer awareness means each Eureka node is configured with the URLs of its *sibling* nodes (not itself) in `defaultZone`, so it registers as a client of its peers and replicates registry changes to them. A common mistake is pointing every node at `localhost`, which creates isolated single-node instances that never actually replicate.

**Q: Why might a Eureka cluster running locally need `/etc/hosts` entries?**

Because peer configuration typically references peer hostnames rather than raw IPs (to mirror real deployments), and those hostnames need to resolve to something reachable — usually `127.0.0.1` when running multiple instances on one machine for testing.

**Q: How can you verify that a Eureka cluster's peer replication is actually working?**

Check each node's dashboard "DS Replicas" panel — it should list the other peer nodes. If it's empty or incomplete, replication configuration is broken even though the server process appears to run normally.

**Q: Why should clients be configured with the full list of Eureka peer URLs rather than just one?**

So the Eureka client library can fail over automatically: if the first URL is unreachable, it tries the next one in the comma-separated list, allowing registration and heartbeats to continue even if one Eureka node is down or being restarted.

**Q: What happens to a client if it doesn't configure `eureka.client.service-url.defaultZone` at all?**

It falls back to the default `http://localhost:8761/eureka/`. In non-local environments this silently fails to connect rather than raising an explicit configuration error, which can be confusing when debugging why a service never registers.


## Resources

### Youtube

- [Highly Available Eureka Service Registry | Spring Cloud | Microservice Architecture](https://www.youtube.com/watch?v=Y3JxNzBSOp0)
- [Spring Cloud Netflix Eureka - Service Discovery with Spring Boot Microservices](https://www.youtube.com/watch?v=e09P-CkCvvs)
- [Spring Boot Microservices - Service Discovery & Registration using Netflix Eureka | JavaTechie](https://www.youtube.com/watch?v=sTKQSPDAnlA)

### Medium

- [Service Discovery in Spring Boot Microservices using Netflix Eureka](https://medium.com/@ankithahjprakash/service-discovery-in-spring-boot-microservices-using-netflix-eureka-5b41827ba0c6)
- [Spring Cloud Netflix Eureka: Service Discovery in Microservices](https://www.baeldung.com/spring-cloud-netflix-eureka)
