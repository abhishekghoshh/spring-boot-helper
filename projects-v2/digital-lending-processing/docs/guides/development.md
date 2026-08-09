# Development Guide

## Prerequisites

| Tool | Version | Installation |
|---|---|---|
| Java | 25+ | `brew install openjdk@25` |
| Maven | 3.9+ | `brew install maven` |
| Docker | 29+ | `brew install docker` |
| Node.js | 22+ | `brew install node` |
| Git | 2+ | `brew install git` |

## Environment Setup

### 1. Set Java 25 as Default

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"
java -version  # Should show "25.0.x"
```

Add to `~/.zshrc` or `~/.bashrc` for persistence.

### 2. Verify Maven Uses Java 25

```bash
mvn --version | grep "Java version"
# Expected: Java version: 25.0.x
```

### 3. Start Infrastructure

```bash
docker compose up -d mongodb redis mailpit mongo-express redis-insight
```

Verify with:
```bash
docker compose ps
# All 5 infrastructure services should be "healthy"
```

### 4. Build All Modules

```bash
mvn clean compile
```

All 15 modules should show `BUILD SUCCESS`.

### 5. Start All Services

```bash
docker compose up -d
```

Check Eureka Dashboard at [http://localhost:8761](http://localhost:8761) — all services should appear.

## Development Workflow

### Running a Single Service Locally

Stop the Docker container for the service you want to develop:

```bash
docker compose stop authentication-service
```

Then run it from the IDE or command line:

```bash
mvn spring-boot:run -pl authentication-service -am
```

The service will connect to MongoDB and Redis running in Docker via `localhost`.

### Making Changes

1. **Edit code** in your preferred IDE
2. **Compile**: `mvn clean compile`
3. **Test**: `mvn test -pl <module>`
4. **Run**: `mvn spring-boot:run -pl <module> -am`

### Adding a New Service

1. Create directory: `mkdir new-service`
2. Create `pom.xml` extending the root parent
3. Create main application class with `@SpringBootApplication`
4. Create `application.yml` with service config
5. Add to root `pom.xml` `<modules>` list
6. Add Dockerfile in service directory
7. Add service entry to `docker-compose.yaml`

### Package Structure

Each service follows package-by-feature:

```
service-name/
├── pom.xml
├── Dockerfile
├── helm/
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
└── src/main/java/com/loansphere/<domain>/
    ├── <Service>Application.java    # Main class
    ├── config/                      # Security, CORS, Redis, etc.
    ├── controller/                  # REST controllers
    ├── service/                     # Business logic
    ├── repository/                  # MongoDB repositories
    ├── model/                       # Domain entities
    ├── dto/                         # Request/Response DTOs
    └── client/                      # OpenFeign clients
```

## Testing

### Running Tests

```bash
# All tests
mvn test

# Single module
mvn test -pl authentication-service

# With coverage
mvn verify -pl authentication-service

# Skip tests during build
mvn clean compile -DskipTests
```

### Writing Tests

**Unit Tests** (JUnit 5 + Mockito):
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @InjectMocks private AuthService authService;

    @Test
    void shouldRegisterNewUser() { ... }
}
```

**Controller Tests** (`@WebMvcTest`):
```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @MockBean private AuthService authService;
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldLoginSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
```

**Integration Tests** (Testcontainers):
```java
@SpringBootTest
@Testcontainers
class AuthIntegrationTest {
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8");
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);
}
```

## Common Tasks

### Adding a Dependency

Add to the service's `pom.xml`. If shared across multiple services, add to root `dependencyManagement`.

### Adding a MongoDB Collection

1. Create model class in `model/`
2. Create repository interface in `repository/`
3. Add collection name to the service's `application.yml`
4. Create indexes via `@Indexed` annotations or `@CompoundIndex`

### Adding a New API Endpoint

1. Create DTO records in `dto/`
2. Add method to service in `service/`
3. Add endpoint to controller in `controller/`
4. Add security annotations (`@PreAuthorize`) if needed
5. Add route to API Gateway `application.yml` if new path prefix

### Debugging

**Enable debug logging**:
```yaml
logging:
  level:
    com.loansphere: DEBUG
    org.springframework.security: DEBUG
```

**View service logs in Docker**:
```bash
docker compose logs -f authentication-service
```

**Connect to MongoDB**:
```bash
docker compose exec mongodb mongosh -u root -p rootroot
```

**Connect to Redis**:
```bash
docker compose exec redis redis-cli -a rootroot
```

## IDE Setup

### IntelliJ IDEA

1. Open the root `pom.xml` as a project
2. Enable annotation processing (Settings → Build → Compiler → Annotation Processors)
3. Install Lombok plugin
4. Set Project SDK to Java 25

### VS Code

1. Install "Extension Pack for Java"
2. Install "Spring Boot Extension Pack"
3. Open the root folder
4. Configure `java.configuration.runtimes` for Java 25

## Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Build and test
mvn clean test

# Commit with conventional format
git add .
git commit -m "feat(auth): add email verification endpoint"
```
