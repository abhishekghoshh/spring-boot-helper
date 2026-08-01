
## Spring Cloud Config

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


## Spring cloud bus

- Spring Cloud Bus
- The Global Refresh Problem
- Add Spring Cloud Bus & Actuator Dependencies
- Enable the /busrefresh URL Endpoint
- Enable Config Refresh
- Use spring cloud Bus in a service
- Implement Spring Cloud Bus and RabbitMQ and Kafka
- Rabbit MQ Default Connection Details
- Change default Rabbit MQ Password
- Trying how Spring Cloud Bus Works


## Service discovery

- Spring cloud Discovery server or Eureka Naming Server
- Problems with Eureka
- Resilient Eureka Server with multiple instances and Data Replication Theory
- Self-Preservation Mode in Eureka
- Registering Eureka Clients And Sending Request
- Eureka Client Library
- Eureka Client Health Check Configuration
- Load Balancing with Eureka, Feign & Spring Cloud LoadBalancer
- Reactive Way using Feign Reactive
- Configure Spring Security to Eureka Server
- Enable Web Security in Eureka
- Configure Eureka Clients to use Username and Password
- Configure Eureka Service URL in Config Server
- Move Username and Password to Config Server
- Encrypting Username and Password
- Eureka Cluster and Peer Awareness Configuration
- Eureka Cluster: Update Hosts File
- Staring up Eureka Discovery Server Cluster
- Eureka Server Dashboard: Checking Registered Peers
- Eureka Discovery Client with Default Configuration
- Register Eureka Client with Eureka Cluster


## Spring cloud Load Balancing

- What is Spring Cloud LoadBalancer
- Why should we use Spring Cloud LoadBalancer instead of Netflix Ribbon
- Custom Load Balancer Strategies (Round Robin, Random, Weighted Response Time)
- Zone Affinity and Zone Avoidance
- Caching Load Balancer Instances with Spring Cloud LoadBalancer
- how Spring Cloud API Gateway Load Balancing works


## Rest Client

- Using Rest Template for Service Invocation
- Rest Template with Load Balanced
- Rest Template with service discovery
- Using Feign REST Client for Service Invocation
- Feign REST Client with Load Balanced
- Feign REST Client with service discovery
- Using WebClient for connecting different service 
- WebClient with Load Balanced
- WebClient with service discovery
- Using Rest Client for connecting different service 
- Rest Client with Load Balanced
- Rest Client with service discovery
- Declarative HTTP Interface Clients using @HttpExchange
- RestClient vs RestTemplate vs WebClient - When to Use What


## Spring Cloud API Gateway

- Explore Spring Cloud API Gateway and all its connfiguration
- Spring Spring Cloud API Gateway Reactive 
- Exploring Routes with Spring Cloud Gateway
- Automatic Mapping of API Gateway Routes
- Manually Configuring API Gateway Routes
- Automatic & Manual Routing in Spring Cloud API Gateway
- Trying how Spring Cloud API Gateway works
- Rewriting URL Path in Spring Cloud API Gateway
- Build-In Predicate Factories in Spring Cloud API Gateway
- Gateway Filters in Spring Cloud API Gateway
- Implementing Spring Cloud Gateway Logging Filter
- Introduction to Global Filters in Spring Cloud API Gateway
- Creating Global Pre Filter in Spring Cloud API Gateway
- Accessing Request Path and HTTP Headers
- Trying how Pre Filter Works
- Creating Global Post Filter in Spring Cloud API Gateway
- Trying how the Post Filter works
- Defining Filters in a Single Class
- Ordering Global Filters in Spring Cloud API Gateway
- how ordered filters work
- Reviewing Gateway Filter class
- Reading Roles and Authorities from JWT
- Sending Arguments(ROLE) to a Filter class
- Sending Arguments(Authority) to a Filter class
- Passing multiple roles and authorities as a single in-line argument
- Multiple arguments: Trying how it works
- Include error message in Response Body of Spring Cloud Gateway
- Problems with Spring Cloud Gateway
- Enabling Discovery Locator with Eureka for Spring Cloud Gateway
- Rate Limiting in Spring Cloud Gateway using Redis
- Configuring the RequestRateLimiter Gateway Filter
- Integrating Resilience4j Circuit Breaker Filter in Spring Cloud Gateway
- CORS Configuration in Spring Cloud Gateway


## Spring Cloud Security

- Securing Microservices with OAuth2 and JWT
- Setting up an Authorization Server for Microservices
- Configuring Resource Servers to Validate JWT Tokens
- Propagating JWT Tokens Between Microservices
- TokenRelay Filter in Spring Cloud Gateway
- Role-Based Access Control Across Microservices
- Securing Actuator Endpoints Across Microservices


## Circuit Breaker

- Getting started with Circuit Breaker - Resilience4j
- Adding Resilience4j to Spring Boot Microservice
- Circuit Breaker Features of Resilience4j
- Resilience4j - Retry and Fallback Methods
- Rate Limiting and BulkHead Features of Resilience4j
- Bulkhead Pattern - Thread Pool vs Semaphore Isolation
- Time Limiter in Resilience4j
- Circuit Breaker configuration properties
- Actuator /health Endpoint
- Configure Access to Actuator endpoints
- Monitoring Circuit Breaker events in Actuator
- @Retry annotation in Resilience4j
- Aspect Order in Resilience4j
- Resilience4j Retry configuration properties
- Trying how it works Resilience4j Retry works


## Messaging

- Introduction to Spring Cloud Stream
- Spring Cloud Stream Binders (Kafka and RabbitMQ)
- Functional Programming Model with Supplier, Function and Consumer
- Configuring Bindings and Destinations
- Event-Driven Microservices with Spring Cloud Stream and Kafka
- Event-Driven Microservices with Spring Cloud Stream and RabbitMQ
- Consumer Groups and Partitioning
- Error Handling and Dead Letter Queues in Spring Cloud Stream


## Distributed Tracing

- Need For Distributed Tracing
- Distributed Tracing with Spring Sleuth
- Introduction To Zipkin
- Setting Up Zipkin
- Exploring Zipkin Traces
- What is Micrometer Tracing
- Introduction to Distributed Tracing with Micrometer and Zipkin
- Add Micrometer and Zipkin dependencies
- Set up Micrometer Tracing
- Micrometer Tracing Sampling Probability
- Logging TraceId and SpanId
- Configure Micrometer to work with Feign
- View traces in Zipkin Dashboard



## Log Aggregation With ELK

- Configuring ELK Stack
- Setting up And Launching Kibana And Elastic Search
- Exploring Log stash
- Exploring Logstash Filters
- Configuring Logstash And Reading Logs
- Exploring Logstash Grok
- Reading Events Through TCP Socket
- Configure Microservices to Log into a File
- Configure Logstash to Read Log Files
- Visualizing Log Aggregation With Kibana
- Download and Run Elasticsearch with Security Enabled
- Configure Elasticsearch Security in Logstash
- Run Search Query in Elasticsearch


## Testing

- Introduction to Spring Cloud Contract
- Consumer-Driven Contract Testing
- Writing Contract Definitions (Groovy/YAML)
- Stub Runner for Consumer-Side Tests
- Testing Microservices with WireMock and MockMvc
- Integration Testing Microservices with Testcontainers


