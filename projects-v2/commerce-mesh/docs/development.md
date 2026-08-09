# Development Guide

## Prerequisites

| Tool | Version | Verification |
|------|---------|-------------|
| JDK | 21+ | `java -version` |
| Maven | 3.9+ | `mvn --version` |
| Docker | 27+ | `docker --version` |
| Docker Compose | 2+ | `docker compose version` |
| Node.js | 22+ | `node --version` |
| npm | 10+ | `npm --version` |
| Git | 2.40+ | `git --version` |

---

## Quick Start

```bash
# 1. Clone repository
git clone <repo-url>
cd commerce-mesh

# 2. Start infrastructure (databases, message broker)
docker compose up -d postgres mongodb redis rabbitmq

# 3. Start Spring Cloud infrastructure
cd backend/discovery-server && ./mvnw spring-boot:run &     # Terminal 1
cd backend/config-server && ./mvnw spring-boot:run &         # Terminal 2
cd backend/api-gateway && ./mvnw spring-boot:run &           # Terminal 3

# 4. Start a microservice (e.g., product-catalog-service)
cd backend/product-catalog-service && ./mvnw spring-boot:run

# 5. Start frontend
cd ui/admin-console && npm run dev
```

Or use the convenience script:

```bash
./scripts/start.sh   # Start everything
./scripts/stop.sh    # Stop everything
```

---

## Development Workflow

### Running a Single Service

```bash
cd backend/<service-name>
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The service will:
1. Register with Eureka at `http://localhost:8761`
2. Fetch configuration from Config Server at `http://localhost:8888`
3. Connect to its database (PostgreSQL/MongoDB) at `localhost`
4. Expose REST APIs and Actuator endpoints

### Environment Variables

Create a `.env` file in the project root (already provided with defaults):

```bash
POSTGRES_HOST=localhost
POSTGRES_USER=postgres
POSTGRES_PASSWORD=rootroot
MONGO_HOST=localhost
REDIS_HOST=localhost
EUREKA_HOST=localhost
```

Services reference these via `${VAR_NAME}` placeholders in `application.yml`.

### Spring Profiles

| Profile | Purpose |
|---------|---------|
| `default` | Local development with Docker Compose databases |
| `dev` | Developer-specific overrides |
| `docker` | Running inside Docker Compose (uses service names for hosts) |
| `staging` | Pre-production |
| `prod` | Production (no auto-DDL, stricter security) |

Activate profiles in IntelliJ run configurations or via command line:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,docker
```

---

## Project Structure (Per Service)

```
backend/<service-name>/
├── pom.xml
├── Dockerfile
├── helm/
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── configmap.yaml
│       ├── secret.yaml
│       ├── ingress.yaml
│       ├── hpa.yaml
│       ├── serviceaccount.yaml
│       └── networkpolicy.yaml
└── src/
    ├── main/
    │   ├── java/com/commercemesh/<package>/
    │   │   ├── <Service>Application.java
    │   │   ├── config/       # SecurityConfig, CORSConfig, SwaggerConfig
    │   │   ├── controller/   # REST controllers
    │   │   ├── service/      # Business logic services
    │   │   ├── repository/   # Spring Data repositories (JPA/Mongo)
    │   │   ├── model/        # JPA entities / MongoDB documents
    │   │   ├── dto/          # Request/Response DTOs
    │   │   ├── mapper/       # MapStruct mappers
    │   │   └── exception/    # Custom exceptions, GlobalExceptionHandler
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/  # Flyway migrations (PostgreSQL services)
    └── test/
        └── java/com/commercemesh/<package>/
            ├── controller/   # @WebMvcTest
            ├── service/      # Unit tests with Mockito
            ├── repository/   # @DataJpaTest / @DataMongoTest
            └── integration/  # Testcontainers-based integration tests
```

---

## Testing

### Unit Tests

```bash
cd backend/<service-name>
./mvnw test
```

**Controller tests** (`@WebMvcTest`):
```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @MockBean private ProductService productService;
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldReturnProductById() throws Exception {
        when(productService.getById("123")).thenReturn(productDto);
        mockMvc.perform(get("/api/v1/products/123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Headphones"));
    }
}
```

**Service tests** (plain JUnit + Mockito):
```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository repository;
    @Mock private ProductMapper mapper;
    @InjectMocks private ProductService service;

    @Test
    void shouldCreateProduct() { ... }
}
```

**Repository tests** (`@DataJpaTest` / `@DataMongoTest`):
```java
@DataJpaTest
class UserRepositoryTest {
    @Autowired private TestEntityManager em;
    @Autowired private UserRepository repository;

    @Test
    void shouldFindByEmail() { ... }
}
```

### Integration Tests (Testcontainers)

```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Test
    void shouldPlaceOrderAndPublishEvent() { ... }
}
```

### Code Coverage Target

- Unit test coverage: **90%+** on service and controller layers
- Run with JaCoCo: `./mvnw clean verify`
- Report generated at `target/site/jacoco/index.html`

### Architecture Tests (ArchUnit)

```java
@AnalyzeClasses(packages = "com.commercemesh.catalog")
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule services_should_be_annotated_with_Service =
        classes().that().resideInAPackage("..service..")
            .should().beAnnotatedWith(Service.class);
}
```

---

## Frontend Development

CommerceMesh has two frontend applications that share the same technology stack but serve different audiences.

### Admin Portal (`ui/admin-console/`)

Directory structure:

```
ui/admin-console/
├── src/
│   ├── api/             # Axios clients per service
│   │   └── apiClient.ts
│   ├── components/      # Reusable UI components
│   │   ├── DataTable.tsx
│   │   ├── StatusChip.tsx
│   │   └── ConfirmDialog.tsx
│   ├── context/         # React contexts
│   │   └── AuthContext.tsx
│   ├── hooks/           # Custom hooks
│   │   ├── useAuth.ts
│   │   ├── useProducts.ts
│   │   └── useOrders.ts
│   ├── layouts/         # Page layouts
│   │   └── MainLayout.tsx
│   ├── pages/           # Route pages
│   │   ├── LoginPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── ProductsPage.tsx
│   │   └── ...
│   ├── types/           # TypeScript interfaces
│   │   └── index.ts
│   └── utils/           # Utility functions
│       └── formatters.ts
├── package.json
├── vite.config.ts
└── tsconfig.json
```

**Running Admin Portal:**

```bash
cd ui/admin-console
npm install
npm run dev           # dev server on http://localhost:5173
npm run build         # production build → dist/
npm run preview       # preview production build
```

### Customer Storefront (`ui/customer-console/`)

Directory structure:

```
ui/customer-console/
├── src/
│   ├── api/
│   │   └── apiClient.ts
│   ├── context/
│   │   ├── AuthContext.tsx     # Customer login/register
│   │   └── CartContext.tsx     # Cart state management
│   ├── layouts/
│   │   └── CustomerLayout.tsx  # Store header, search, cart badge, footer
│   └── pages/
│       ├── HomePage.tsx        # Hero banner, featured products, categories
│       ├── ProductListPage.tsx # Product grid with filters
│       ├── ProductDetailPage.tsx # Images, description, add to cart
│       ├── CartPage.tsx        # Cart with quantity controls
│       ├── CheckoutPage.tsx    # Multi-step checkout flow
│       ├── OrderHistoryPage.tsx # Past orders with status
│       ├── ProfilePage.tsx     # User profile editing
│       ├── WishlistPage.tsx    # Saved products
│       ├── LoginPage.tsx       # Customer sign-in
│       └── RegisterPage.tsx    # Customer registration
├── package.json
├── vite.config.ts
└── tsconfig.json
```

**Running Customer Storefront:**

```bash
cd ui/customer-console
npm install
npm run dev           # dev server on http://localhost:5173
npm run build         # production build → dist/
npm run preview       # preview production build
```

### Access in Docker Compose

When running via Docker Compose:

| App | URL | Port |
|-----|-----|------|
| Admin Portal | http://localhost:3000 | 3000 |
| Customer Storefront | http://localhost:3001 | 3001 |

---

## Debugging

### Remote Debugging (Java)

Add to your run configuration or command line:

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

Then attach your IDE debugger to `localhost:5005`.

### Docker Compose Service Debugging

```bash
# View logs for a specific service
docker compose logs -f authentication-service

# Shell into a running container
docker compose exec authentication-service sh

# Check database connectivity from container
docker compose exec authentication-service wget -qO- http://postgres:5432

# Check Eureka registration
curl http://localhost:8761/eureka/apps
```

### Common Issues

| Issue | Solution |
|-------|----------|
| **Eureka not registering** | Check `eureka.client.service-url.defaultZone` in `application.yml`. Ensure Eureka server is running. |
| **Config Server connection refused** | Wait for Config Server to fully start. Check `spring.cloud.config.uri`. |
| **Database connection refused** | Ensure Docker Compose databases are running: `docker compose ps` |
| **RabbitMQ connection issues** | Check `spring.rabbitmq.host` and credentials. Verify RabbitMQ is healthy. |
| **Feign client timeouts** | Check Resilience4j timeouts. Increase if downstream service is slow. |
| **JWT validation fails** | Token may be expired (15 min). Refresh or re-login. Check `app.jwt.secret` consistency. |
| **CORS errors in browser** | Ensure API Gateway CORS config allows `http://localhost:5173` (Vite dev server). |

---

## Code Quality Standards

### Java

- **Constructor injection** only — no `@Autowired` on fields
- **Immutability** — dependency fields are `private final`
- **DTOs** — use Java records for request/response DTOs
- **MapStruct** — all entity ↔ DTO mapping through MapStruct interfaces
- **Lombok** — use `@RequiredArgsConstructor` for constructor injection, `@Slf4j` for logging
- **No raw types** — always specify generics
- **Exception handling** — throw custom exceptions defined within each service's own `exception` package, never `RuntimeException` directly

### TypeScript/React

- Use functional components with hooks
- TypeScript strict mode enabled
- API calls through TanStack Query (never raw fetch/axios in components)
- Form handling through React Hook Form + Zod validation
- Components in PascalCase, hooks in camelCase with `use` prefix

### General

- Feature branches named `feat/`, `fix/`, `docs/`
- Conventional commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`
- PRs require code review before merge
- No commented-out code in committed files
- Meaningful test names: `shouldReturn404WhenProductNotFound`

---

## IntelliJ IDEA Setup

### Recommended Plugins

- Spring Boot Assistant
- Lombok (enable annotation processing)
- MapStruct Support
- EnvFile (load `.env` into run configurations)
- Docker
- SonarLint

### Run Configuration

Creating a shared run configuration (`Edit Configurations → Templates → Spring Boot`):
- **Main class:** `<Service>Application`
- **Active profiles:** `dev`
- **Environment variables:** Load from `commerce-mesh/.env`
- **VM options:** `-Duser.timezone=UTC`

---

## Git Workflow

```bash
# Create feature branch
git checkout -b feat/add-product-search

# Make changes, run tests
./mvnw clean verify -pl backend/search-service
cd ui/admin-console && npm run build

# Commit
git add .
git commit -m "feat(search): add full-text product search endpoint"

# Push and create PR
git push origin feat/add-product-search
```
