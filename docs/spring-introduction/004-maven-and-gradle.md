### Maven — Project Management and Build Tool

---

#### What is Maven?

**Apache Maven** is an open-source **project management and build automation tool** for Java (and JVM-based) projects. It was created by Jason van Zyl in 2002 as part of the Apache Turbine project and became a top-level Apache project in 2004.

Maven goes far beyond a simple build script. It enforces a **standard project structure**, **manages dependencies** automatically, defines a **build lifecycle**, and generates **project documentation** — all driven by a single XML file called `pom.xml` (Project Object Model).

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Maven Manages:                                                             │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                           pom.xml                                          │ │
│  │                    (Project Object Model)                                  │ │
│  └──────────────┬──────────────┬───────────────┬───────────────┬──────────────┘ │
│                 │              │               │               │                │
│                 ▼              ▼               ▼               ▼                │
│  ┌──────────────────┐ ┌──────────────┐ ┌───────────────┐ ┌─────────────────┐   │
│  │  Build           │ │  Dependency  │ │  Reporting /  │ │  Release /      │   │
│  │  Automation      │ │  Resolution  │ │  Documentation│ │  Deployment     │   │
│  │                  │ │              │ │               │ │                 │   │
│  │  compile         │ │  Downloads   │ │  Javadoc      │ │  version tags   │   │
│  │  test            │ │  JARs from   │ │  Test reports │ │  push to Nexus  │   │
│  │  package         │ │  Maven       │ │  Code quality │ │  push to        │   │
│  │  install         │ │  Central /   │ │  reports      │ │  Artifactory    │   │
│  │  deploy          │ │  Nexus       │ │  Site         │ │                 │   │
│  └──────────────────┘ └──────────────┘ └───────────────┘ └─────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Before Maven**, Java developers had to:
- Manually download dependency JARs and put them in a `lib/` folder
- Write `javac` commands with long classpaths
- Write custom `Ant` build scripts for every project (not standardised)
- Manually manage JAR versions and compatibility

---

#### How Maven Helps Developers — Through POM

The `pom.xml` (Project Object Model) is a single XML file at the root of your project that tells Maven **everything** it needs to know about your project.

##### Build Generation

Maven's build lifecycle defines a sequence of phases. Running `mvn package` automatically runs `validate → compile → test → package` in order.

```xml
<!-- pom.xml — the <build> section controls HOW Maven builds your code -->
<build>
    <sourceDirectory>src/main/java</sourceDirectory>       <!-- javac reads from here -->
    <outputDirectory>target/classes</outputDirectory>       <!-- .class files go here -->
    <testSourceDirectory>src/test/java</testSourceDirectory>
    <testOutputDirectory>target/test-classes</testOutputDirectory>

    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>21</source>
                <target>21</target>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
    </plugins>
</build>
```

##### Dependency Resolution

Maven maintains a **local repository** (`~/.m2/repository`) and downloads dependencies from **remote repositories** (Maven Central, Nexus, Artifactory). Dependencies are declared in `pom.xml` and Maven resolves the **full transitive dependency graph** automatically.

```xml
<!-- Declare a dependency — Maven downloads it and all its transitive deps -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.3.0</version>
</dependency>
<!-- Maven fetches: spring-webmvc, tomcat-embed, jackson-databind, slf4j, ... -->
<!-- You declare 1 line; Maven resolves 50+ transitive dependencies -->
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Dependency Resolution Flow:                                                     │
│                                                                                  │
│  mvn compile                                                                     │
│      │                                                                           │
│      ▼  Check local repository (~/.m2/repository)                                │
│      │  └─ Found? Use it directly (no download)                                  │
│      │  └─ Not found? ──────────────────────────────────────────────►            │
│      │                                                                           │
│      ▼  Query remote repositories (Maven Central, company Nexus)                 │
│      │  └─ Download JAR + POM to ~/.m2/repository                               │
│      │  └─ Read transitive dependencies from the downloaded POM                  │
│      │  └─ Repeat recursively for all transitive deps                            │
│      │                                                                           │
│      ▼  Add all resolved JARs to the compile classpath                           │
│      │                                                                           │
│      ▼  javac src/**/*.java -classpath ~/.m2/repository/...                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Dependency scopes:**

| Scope | Available at | Included in JAR/WAR | Use case |
|---|---|---|---|
| `compile` (default) | compile + runtime + test | YES | Regular dependencies |
| `provided` | compile + test | NO | Servlet API (container provides it) |
| `runtime` | runtime + test | YES | JDBC drivers (not needed to compile) |
| `test` | test only | NO | JUnit, Mockito |
| `system` | compile + test | NO | Explicit filesystem path (avoid) |
| `import` | POM only | NO | Import another POM's dependency management |

##### Documentation

Maven can generate a full project website with reports:

```bash
mvn site           # generate HTML site in target/site/
mvn javadoc:javadoc  # generate Javadoc in target/site/apidocs/
mvn surefire-report:report  # generate HTML test report
```

---

#### Maven Project Structure

Maven enforces a **standard directory layout**. Every Maven project looks the same — any Java developer can navigate it instantly.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Maven Standard Directory Layout:                                                │
│                                                                                  │
│  my-project/                                                                     │
│  ├── pom.xml                          ← Project Object Model (required)          │
│  │                                                                               │
│  ├── src/                                                                        │
│  │   ├── main/                        ← Production code                          │
│  │   │   ├── java/                    ← Java source files                        │
│  │   │   │   └── com/example/                                                   │
│  │   │   │       ├── MyApp.java                                                  │
│  │   │   │       ├── controller/                                                 │
│  │   │   │       ├── service/                                                    │
│  │   │   │       └── repository/                                                 │
│  │   │   ├── resources/               ← Non-Java resources (classpath)           │
│  │   │   │   ├── application.properties                                          │
│  │   │   │   ├── logback-spring.xml                                              │
│  │   │   │   └── db/migration/        ← Flyway SQL scripts                       │
│  │   │   └── webapp/                  ← Web resources (WAR projects only)        │
│  │   │       └── WEB-INF/                                                        │
│  │   │           └── web.xml                                                     │
│  │   │                                                                           │
│  │   └── test/                        ← Test code (NOT included in final JAR)    │
│  │       ├── java/                    ← Test source files                        │
│  │       │   └── com/example/                                                   │
│  │       │       ├── UserServiceTest.java                                        │
│  │       │       └── UserControllerTest.java                                     │
│  │       └── resources/               ← Test-only resources                      │
│  │           └── application-test.properties                                     │
│  │                                                                               │
│  ├── target/                          ← BUILD OUTPUT (generated, not committed)  │
│  │   ├── classes/                     ← Compiled .class files from src/main/java │
│  │   ├── test-classes/                ← Compiled test .class files               │
│  │   ├── surefire-reports/            ← Unit test XML/HTML reports               │
│  │   ├── my-project-1.0.0.jar         ← Final JAR artifact                       │
│  │   └── my-project-1.0.0-sources.jar ← Sources JAR (optional)                  │
│  │                                                                               │
│  ├── mvnw                             ← Maven Wrapper script (Unix)              │
│  ├── mvnw.cmd                         ← Maven Wrapper script (Windows)           │
│  └── .mvn/                                                                       │
│      └── wrapper/                                                                │
│          └── maven-wrapper.properties ← Maven version pinned for the project    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Maven Commands

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  All Common Maven Commands:                                                      │
│                                                                                  │
│  LIFECYCLE PHASES                                                                │
│  mvn validate          Validate pom.xml and project structure                   │
│  mvn compile           Compile src/main/java → target/classes                   │
│  mvn test              Run unit tests (also compiles test sources)               │
│  mvn package           Create JAR/WAR in target/                                │
│  mvn verify            Run integration tests + verify package                   │
│  mvn install           Install JAR into local ~/.m2 repository                  │
│  mvn deploy            Upload JAR to remote repository (Nexus/Artifactory)      │
│  mvn clean             Delete the target/ directory                             │
│  mvn site              Generate project website/reports                         │
│                                                                                  │
│  COMBINED (most commonly used)                                                   │
│  mvn clean compile     Clean then compile                                        │
│  mvn clean test        Clean then compile + test                                 │
│  mvn clean package     Clean then full build to JAR/WAR                         │
│  mvn clean install     Clean, build, install to local repo                      │
│  mvn clean deploy      Clean, build, push to remote repo                        │
│                                                                                  │
│  DEPENDENCY MANAGEMENT                                                           │
│  mvn dependency:tree               Print full dependency tree                   │
│  mvn dependency:resolve            Download all declared deps to local repo      │
│  mvn dependency:analyze            Find unused/undeclared deps                  │
│  mvn dependency:purge-local-repository  Delete and re-download all deps         │
│  mvn dependency:copy-dependencies  Copy all dep JARs to target/dependency/      │
│                                                                                  │
│  PLUGIN GOALS                                                                    │
│  mvn help:describe -Dplugin=compiler   Show all goals of a plugin               │
│  mvn help:effective-pom                Print the fully-merged effective POM      │
│  mvn help:active-profiles             Show which profiles are active             │
│  mvn versions:display-dependency-updates  Show available dep upgrades           │
│  mvn versions:use-latest-releases     Upgrade all deps to latest release        │
│                                                                                  │
│  SKIPPING                                                                        │
│  mvn package -DskipTests             Compile tests but do not RUN them          │
│  mvn package -Dmaven.test.skip=true  Don't even compile tests                   │
│  mvn package -DskipITs               Skip integration tests only               │
│                                                                                  │
│  RUNNING THE APP                                                                 │
│  mvn spring-boot:run                 Run Spring Boot app (no package needed)    │
│  mvn exec:java -Dexec.mainClass=...  Run any main class                        │
│                                                                                  │
│  PROFILES                                                                        │
│  mvn package -Pprod                  Activate the 'prod' profile               │
│  mvn package -P!dev                  Deactivate the 'dev' profile              │
│                                                                                  │
│  VERBOSE / DEBUG                                                                 │
│  mvn package -X                      Debug output (very verbose)               │
│  mvn package -e                      Show full stack traces on error            │
│  mvn package -q                      Quiet mode (errors only)                   │
│  mvn package --offline               Use only local repo, no network            │
│  mvn package -T 4                    Use 4 threads (parallel build)             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### `mvn wrapper:wrapper` — The Maven Wrapper

The **Maven Wrapper** (`mvnw`) pins a specific Maven version to your project so all developers and CI/CD pipelines use **exactly the same Maven version** without any global installation required.

```bash
# Generate the Maven Wrapper for the current project
# (pins the Maven version currently installed on your machine)
mvn wrapper:wrapper

# Pin a specific Maven version
mvn wrapper:wrapper -Dmaven=3.9.6

# This creates:
#   mvnw                           Unix shell script
#   mvnw.cmd                       Windows batch script
#   .mvn/wrapper/maven-wrapper.properties  (version config)

# After generation, NEVER use 'mvn' in the project — use './mvnw' instead
./mvnw clean package
./mvnw spring-boot:run
```

```properties
# .mvn/wrapper/maven-wrapper.properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
```

When you run `./mvnw`:
1. The script checks if the specified Maven version is cached in `~/.m2/wrapper/dists/`
2. If not, downloads it automatically
3. Runs your Maven command with that exact version

---

#### Maven Build Lifecycle Phases

Maven has **three built-in lifecycles**: `default` (build), `clean`, and `site`. The `default` lifecycle has 23 phases. The 7 key phases you work with daily are:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Maven Default Build Lifecycle — Key Phases in Order:                            │
│                                                                                  │
│  Phase          Plugin:Goal that runs by default          Output                 │
│  ────────────────────────────────────────────────────────────────────────────    │
│  1. validate    (built-in validation)                     Checks pom.xml         │
│       │                                                                          │
│       ▼                                                                          │
│  2. compile     maven-compiler-plugin:compile             target/classes/        │
│       │                                                                          │
│       ▼                                                                          │
│  3. test        maven-surefire-plugin:test                target/surefire-reports│
│       │                                                                          │
│       ▼                                                                          │
│  4. package     maven-jar-plugin:jar  (or war-plugin:war) target/myapp-1.0.jar  │
│       │                                                                          │
│       ▼                                                                          │
│  5. verify      (custom plugins, e.g. PMD, checkstyle)    Reports in target/     │
│       │                                                                          │
│       ▼                                                                          │
│  6. install     maven-install-plugin:install              ~/.m2/repository/      │
│       │                                                                          │
│       ▼                                                                          │
│  7. deploy      maven-deploy-plugin:deploy                Remote Nexus repo      │
│                                                                                  │
│  RULE: Running any phase ALSO runs ALL phases before it.                         │
│  mvn install  →  validate + compile + test + package + verify + install          │
└──────────────────────────────────────────────────────────────────────────────────┘
```

##### Phase 1 — `validate`

```bash
mvn validate
```

Checks that the project structure is correct and `pom.xml` is well-formed. Maven verifies:
- `pom.xml` is valid XML with required fields (`groupId`, `artifactId`, `version`)
- All declared dependencies can be resolved (coordinates are valid)
- Parent POM can be found

No output files are generated. Fails fast with a clear error if the project is misconfigured.

##### Phase 2 — `compile`

```bash
mvn compile
```

Compiles `src/main/java/**/*.java` using the `maven-compiler-plugin` and **`javac`** (the standard JDK compiler). Output goes to `target/classes/`.

```xml
<!-- Configuring the compiler plugin in pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <!-- Java source/target version -->
                <source>21</source>
                <target>21</target>
                <!-- Alternatively (Java 9+): -->
                <release>21</release>
                <!-- Source file encoding -->
                <encoding>UTF-8</encoding>
                <!-- Show all compiler warnings -->
                <showWarnings>true</showWarnings>
                <!-- Enable annotation processors (e.g., Lombok, MapStruct) -->
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>1.18.30</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

```text
After 'mvn compile':
target/
└── classes/
    └── com/
        └── example/
            ├── MyApp.class
            ├── controller/
            │   └── UserController.class
            └── service/
                └── UserService.class
```

##### Phase 3 — `test`

```bash
mvn test
```

Compiles test sources (`src/test/java`) to `target/test-classes/`, then runs unit tests using the `maven-surefire-plugin`. Reports are written to `target/surefire-reports/`.

**Skipping tests:**

```bash
# Option 1: Compile tests but do NOT run them
mvn package -DskipTests

# Option 2: Don't even compile the test sources (faster, but you won't catch test compilation errors)
mvn package -Dmaven.test.skip=true

# Option 3: Run only a specific test class
mvn test -Dtest=UserServiceTest

# Option 4: Run only a specific test method
mvn test -Dtest=UserServiceTest#findById_throwsWhenNotFound
```

```xml
<!-- Skip tests in pom.xml — useful for CI jobs that run tests separately -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.1.2</version>
            <configuration>
                <!-- Skip tests globally in this pom -->
                <skipTests>${skipTests}</skipTests>
                <!-- Include/exclude specific test patterns -->
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*Tests.java</include>
                    <include>**/*Spec.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IntegrationTest.java</exclude>
                </excludes>
                <!-- Run tests in parallel -->
                <parallel>methods</parallel>
                <threadCount>4</threadCount>
            </configuration>
        </plugin>
    </plugins>
</build>
```

##### Phase 4 — `package`

```bash
mvn package
```

Packages compiled classes into a distributable format (JAR or WAR) and saves it in `target/`.

```text
Output location:
target/{artifactId}-{version}.jar          for <packaging>jar</packaging>
target/{artifactId}-{version}.war          for <packaging>war</packaging>
```

```xml
<!-- pom.xml — packaging type -->
<packaging>jar</packaging>   <!-- default -->
<!-- or -->
<packaging>war</packaging>
```

**Running the packaged JAR:**

```bash
# Run a regular JAR (requires Main-Class in MANIFEST.MF)
java -jar target/myapp-1.0.0.jar

# Run a Spring Boot fat JAR (Main-Class is set by spring-boot-maven-plugin)
java -jar target/myapp-1.0.0.jar
java -jar target/myapp-1.0.0.jar --server.port=9090
java -jar target/myapp-1.0.0.jar --spring.profiles.active=prod

# Run without packaging (Spring Boot only)
mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"

# Run a WAR deployed to Tomcat
cp target/myapp-1.0.0.war /opt/tomcat/webapps/
/opt/tomcat/bin/startup.sh
```

```xml
<!-- Spring Boot Maven Plugin — makes the JAR executable (fat JAR) -->
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>   <!-- repackages the JAR into a fat/executable JAR -->
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

##### Phase 5 — `verify`

```bash
mvn verify
```

By default, very little happens here. Its purpose is for **integration tests** and **quality checks** that run AFTER packaging. You add custom plugins in `pom.xml` to make this phase useful.

**Example: PMD (Static Code Analysis)**

PMD finds common code issues: unused variables, empty catch blocks, duplicate code, overly complex methods.

```xml
<build>
    <plugins>
        <!-- PMD static analysis — fails build if violations found -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-pmd-plugin</artifactId>
            <version>3.21.0</version>
            <configuration>
                <failOnViolation>true</failOnViolation>
                <printFailingErrors>true</printFailingErrors>
                <rulesets>
                    <ruleset>/rulesets/java/quickstart.xml</ruleset>
                </rulesets>
            </configuration>
            <executions>
                <execution>
                    <phase>verify</phase>   <!-- run during verify phase -->
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

```bash
mvn verify
# PMD scans your code; if violations found, build FAILS with a report
# Report at: target/pmd.xml and target/site/pmd.html

# Run PMD standalone (without going through all lifecycle phases)
mvn pmd:check
```

**Other common `verify`-phase plugins:**

```xml
<!-- Checkstyle: enforce code style (tabs vs spaces, line length, etc.) -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>

<!-- JaCoCo: code coverage — fail build if coverage below threshold -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>   <!-- 80% line coverage required -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

##### Phase 6 — `install`

```bash
mvn install
```

Copies the built JAR/WAR into the **local Maven repository** at `~/.m2/repository/`. Other projects on the same machine can then use it as a dependency.

**What is the local repository?**

```text
~/.m2/repository/                         ← root of local repository
└── com/
    └── example/
        └── my-library/
            └── 1.0.0/
                ├── my-library-1.0.0.jar           ← the JAR
                ├── my-library-1.0.0.pom           ← the POM (for transitive deps)
                ├── my-library-1.0.0-sources.jar   ← sources JAR (optional)
                └── my-library-1.0.0.jar.sha1      ← checksum for integrity
```

**Why use `install`?**

```text
Scenario: You have two projects:
  - my-utils-library  (common code shared across your team)
  - my-web-app        (uses my-utils-library)

# In my-utils-library:
mvn install
# → copies my-utils-library-1.0.0.jar to ~/.m2/repository/

# In my-web-app pom.xml:
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-utils-library</artifactId>
    <version>1.0.0</version>
</dependency>
# Maven finds it in ~/.m2/repository — no network needed
```

**Updating the local repository:**

```bash
# Force re-download all dependencies (clears corrupted/stale JARs)
mvn dependency:purge-local-repository

# Update snapshots (re-download SNAPSHOT versions)
mvn clean install -U

# Manually delete a specific dependency from local repo
rm -rf ~/.m2/repository/com/example/my-library/
```

##### Phase 7 — `deploy`

```bash
mvn deploy
```

Uploads the JAR to a **remote artifact repository** (Nexus, Artifactory, GitHub Packages, etc.) so other teams/projects can use it.

**Configuring `deploy` in `pom.xml`:**

```xml
<project>
    ...

    <!-- distributionManagement: WHERE to deploy -->
    <distributionManagement>
        <!-- Release artifacts (stable versions like 1.0.0, 2.3.1) -->
        <repository>
            <id>nexus-releases</id>                          <!-- must match server id in settings.xml -->
            <name>Nexus Release Repository</name>
            <url>https://nexus.mycompany.com/repository/maven-releases/</url>
        </repository>

        <!-- Snapshot artifacts (development versions like 1.0.0-SNAPSHOT) -->
        <snapshotRepository>
            <id>nexus-snapshots</id>                         <!-- must match server id in settings.xml -->
            <name>Nexus Snapshot Repository</name>
            <url>https://nexus.mycompany.com/repository/maven-snapshots/</url>
        </snapshotRepository>
    </distributionManagement>

</project>
```

Credentials are stored in `~/.m2/settings.xml` (never in `pom.xml` — `pom.xml` is committed to Git):

```xml
<!-- ~/.m2/settings.xml -->
<settings>
    <servers>
        <server>
            <id>nexus-releases</id>         <!-- matches <repository><id> in pom.xml -->
            <username>deploy-user</username>
            <password>${env.NEXUS_PASSWORD}</password>   <!-- from env var — don't hardcode! -->
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>deploy-user</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
```

```bash
# Deploy release (version must NOT end in -SNAPSHOT)
mvn clean deploy -Dspring.profiles.active=prod

# Deploy snapshot (version must end in -SNAPSHOT)
# pom.xml: <version>1.1.0-SNAPSHOT</version>
mvn clean deploy
```

---

#### Why Running One Phase Also Runs All Previous Phases

Maven's lifecycle phases form a **sequential chain** — each phase depends on all prior phases completing successfully. This is by design:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Phase Dependency Chain:                                                         │
│                                                                                  │
│  mvn install   executes:                                                         │
│                                                                                  │
│  validate ──► compile ──► test ──► package ──► verify ──► install               │
│                                                                                  │
│  WHY?                                                                            │
│  • install requires a valid JAR → package must run first                         │
│  • package requires compiled test-free classes → test must run first             │
│  • test requires compiled sources → compile must run first                       │
│  • compile requires a valid pom.xml → validate must run first                   │
│                                                                                  │
│  You cannot install an uncompiled project — it would be meaningless.            │
│  Maven enforces this logical ordering automatically.                             │
│                                                                                  │
│  To run ONLY a specific phase without the chain:                                 │
│  Use plugin goals directly (not lifecycle phases):                               │
│                                                                                  │
│  mvn compiler:compile      → compiles only (no validate phase)                  │
│  mvn surefire:test         → runs tests only (no compile phase)                 │
│  mvn jar:jar               → creates JAR only (no compile/test phases)          │
│  mvn pmd:check             → runs PMD only                                      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Maven Plugins

A **Maven plugin** is a collection of **goals** (executable tasks). All of Maven's functionality comes from plugins — there is no built-in "compile" logic; the `maven-compiler-plugin` provides it.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Plugin Concepts:                                                                │
│                                                                                  │
│  Plugin    = a JAR containing one or more Mojos (Maven plain Old Java Objects)  │
│  Goal      = one executable unit inside a plugin (e.g., compiler:compile)       │
│  Execution = binding a goal to a lifecycle phase                                 │
│                                                                                  │
│  Naming:    plugin-goal = {pluginArtifactId}:{goalName}                          │
│             e.g.,  compiler:compile                                              │
│                    surefire:test                                                 │
│                    spring-boot:run                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Default plugin bindings** (what Maven automatically binds to each phase):

```text
Phase       → Plugin:Goal (default binding)
──────────────────────────────────────────────
compile     → maven-compiler-plugin:compile
test        → maven-surefire-plugin:test
package     → maven-jar-plugin:jar   (or maven-war-plugin:war)
install     → maven-install-plugin:install
deploy      → maven-deploy-plugin:deploy
```

**Calling plugin goals directly:**

```bash
# Syntax: mvn {pluginPrefix}:{goal}
mvn dependency:tree
mvn compiler:compile
mvn spring-boot:run
mvn jacoco:report

# Or with full coordinates:
mvn org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile
```

---

#### Adding/Modifying Tasks in the Build Section

The `<build>` section in `pom.xml` is where you configure plugins and bind goals to lifecycle phases:

```xml
<build>
    <!-- Override default source/output directories (rarely needed) -->
    <sourceDirectory>src/main/java</sourceDirectory>
    <outputDirectory>target/classes</outputDirectory>
    <finalName>myapp</finalName>   <!-- override default: {artifactId}-{version} -->

    <!-- pluginManagement: declare plugins + versions (no execution yet) -->
    <!-- Child POMs inherit these declarations without repeating versions -->
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
        </plugins>
    </pluginManagement>

    <!-- plugins: actually CONFIGURE and EXECUTE plugins -->
    <plugins>

        <!-- ── 1. Configure an existing default plugin ────────────────────────── -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <!-- no <version> needed if declared in pluginManagement -->
            <configuration>
                <source>21</source>
                <target>21</target>
            </configuration>
        </plugin>

        <!-- ── 2. Add a new plugin + bind its goal to a phase ─────────────────── -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-pmd-plugin</artifactId>
            <version>3.21.0</version>
            <executions>
                <execution>
                    <id>pmd-check</id>           <!-- unique id for this execution -->
                    <phase>verify</phase>        <!-- bind to this lifecycle phase -->
                    <goals>
                        <goal>check</goal>       <!-- which goal to run -->
                    </goals>
                    <configuration>
                        <!-- execution-specific config -->
                        <failOnViolation>true</failOnViolation>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- ── 3. Multiple executions of the same plugin ──────────────────────── -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-antrun-plugin</artifactId>
            <version>3.1.0</version>
            <executions>
                <!-- First execution: print a message at compile phase -->
                <execution>
                    <id>compile-start</id>
                    <phase>compile</phase>
                    <goals><goal>run</goal></goals>
                    <configuration>
                        <target>
                            <echo message="Starting compilation..."/>
                        </target>
                    </configuration>
                </execution>
                <!-- Second execution: print a message at package phase -->
                <execution>
                    <id>package-done</id>
                    <phase>package</phase>
                    <goals><goal>run</goal></goals>
                    <configuration>
                        <target>
                            <echo message="Packaging complete!"/>
                        </target>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <!-- ── 4. Spring Boot plugin (repackages JAR into fat/executable JAR) ─── -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <mainClass>com.example.MyApplication</mainClass>   <!-- optional: auto-detected -->
                <excludes>
                    <!-- Don't include Lombok in the fat JAR -->
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

    </plugins>
</build>
```

---

#### How Maven Depends on `pom.xml`

`pom.xml` is the **single source of truth** for everything Maven does. Without `pom.xml`, Maven cannot function at all:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  pom.xml is the Contract Between You and Maven:                                  │
│                                                                                  │
│  What Maven reads from pom.xml:                                                  │
│                                                                                  │
│  1. Project identity   → groupId + artifactId + version                         │
│     Used to: name the output JAR, identify the artifact in repos                 │
│                                                                                  │
│  2. Packaging type     → <packaging>jar|war|pom</packaging>                     │
│     Used to: decide which maven-jar-plugin vs maven-war-plugin goal to bind     │
│                                                                                  │
│  3. Dependencies       → <dependencies> block                                   │
│     Used to: build the compile/test classpath, include in fat JAR               │
│                                                                                  │
│  4. Build config       → <build><plugins> block                                 │
│     Used to: configure compiler version, add custom goals to phases             │
│                                                                                  │
│  5. Properties         → <properties> block                                     │
│     Used to: parameterise versions, encoding, Java version across the POM       │
│                                                                                  │
│  6. Parent             → <parent> block                                         │
│     Used to: inherit config, dependency versions, plugin versions               │
│                                                                                  │
│  7. Distribution       → <distributionManagement>                               │
│     Used to: know WHERE to deploy the artifact (mvn deploy)                     │
│                                                                                  │
│  8. Profiles           → <profiles> block                                       │
│     Used to: conditional config based on environment / activation condition     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

#### Parent and Child POM — POM Hierarchy

##### The Super POM

Every `pom.xml` implicitly inherits from the **Super POM** — Maven's built-in default POM that defines:
- Standard directory locations (`src/main/java`, `target/`, etc.)
- Default plugin versions for core plugins
- Default Maven Central repository URL

You never see it, but it is always there. To inspect it:

```bash
mvn help:effective-pom   # shows the fully-merged POM (your pom + parent POMs + Super POM)
```

##### Parent and Child POMs

In a **multi-module** project (common in microservices or monorepos), a **parent POM** manages shared configuration and dependency versions. Child POMs inherit from it.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  POM Hierarchy:                                                                  │
│                                                                                  │
│  Super POM (Maven built-in)                                                      │
│       │                                                                          │
│       ▼ inherits                                                                 │
│  spring-boot-starter-parent-3.3.0.pom  (Spring Boot's parent)                   │
│       │                                                                          │
│       ▼ inherits                                                                 │
│  my-company-parent/pom.xml             (your company-wide parent POM)            │
│       │                                                                          │
│       ├── my-user-service/pom.xml     (child 1)                                  │
│       ├── my-order-service/pom.xml    (child 2)                                  │
│       └── my-payment-service/pom.xml  (child 3)                                  │
│                                                                                  │
│  Each child inherits:                                                            │
│    • <properties> (Java version, encoding, library versions)                    │
│    • <dependencyManagement> (library versions — not the deps themselves)         │
│    • <build><pluginManagement> (plugin versions + default config)               │
│    • <repositories>                                                             │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Parent POM example:**

```xml
<!-- my-company-parent/pom.xml -->
<project>
    <groupId>com.example</groupId>
    <artifactId>my-company-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>   <!-- MUST be 'pom' for a parent POM -->

    <!-- Inherit from Spring Boot parent -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
    </parent>

    <!-- List all child modules (for multi-module build) -->
    <modules>
        <module>user-service</module>
        <module>order-service</module>
        <module>payment-service</module>
    </modules>

    <!-- Properties inherited by ALL children -->
    <properties>
        <java.version>21</java.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <testcontainers.version>1.19.1</testcontainers.version>
    </properties>

    <!-- dependencyManagement: declares versions WITHOUT adding deps to classpath -->
    <!-- Children STILL need to declare <dependency> — but without <version> -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

</project>
```

**Child POM example:**

```xml
<!-- user-service/pom.xml -->
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-company-parent</artifactId>
        <version>1.0.0</version>
        <!-- relativePath: where to find the parent POM locally -->
        <!-- Default: ../pom.xml. Use empty <relativePath/> to force remote lookup -->
        <relativePath>../pom.xml</relativePath>
    </parent>

    <!-- Only need to declare groupId if different from parent -->
    <artifactId>user-service</artifactId>
    <!-- version inherited from parent unless overridden -->

    <dependencies>
        <!-- No version needed — managed by parent's dependencyManagement -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>

        <!-- Spring Boot starters — versions managed by spring-boot-starter-parent -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

**How POM hierarchy is resolved:**

```text
When Maven evaluates a pom.xml:
1. Load the pom.xml
2. Look for <parent> declaration → load parent POM
3. Repeat for parent's parent, and so on
4. Merge the chain from top (Super POM) to bottom (your pom.xml)
   → Later pom.xml values OVERRIDE earlier (child overrides parent)
5. Result = the "effective POM" = what Maven actually uses

Inheritance rules:
  • <properties>            → merged, child overrides parent key
  • <dependencyManagement>  → merged
  • <dependencies>          → merged (child deps added to parent deps)
  • <build><pluginManagement> → merged
  • <build><plugins>        → merged
  • <repositories>          → merged
  • <distributionManagement> → child overrides parent
```

---

#### All Parts of `pom.xml` — Complete Reference

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <!-- ── 1. MODEL VERSION ────────────────────────────────────────────────────── -->
    <!-- Always 4.0.0 — the POM schema version. Not the project version. -->
    <modelVersion>4.0.0</modelVersion>

    <!-- ── 2. PARENT BLOCK ─────────────────────────────────────────────────────── -->
    <!-- Inherit configuration from a parent POM -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <!-- relativePath: location of parent POM on filesystem.
             Empty = always look up from remote repository (no local lookup) -->
        <relativePath/>
    </parent>

    <!-- ── 3. PROJECT COORDINATES ──────────────────────────────────────────────── -->
    <!-- groupId: organisation/team identifier. Reverse-domain convention.
         Inherited from parent if omitted. -->
    <groupId>com.example</groupId>

    <!-- artifactId: the project/module name. Becomes the JAR filename base. -->
    <artifactId>my-spring-app</artifactId>

    <!-- version: project version.
         SNAPSHOT = in development (re-downloadable, can change).
         Release  = stable (immutable once deployed). -->
    <version>1.0.0-SNAPSHOT</version>

    <!-- packaging: output format. Default = jar.
         Options: jar, war, pom (parent/aggregator), ear, maven-plugin -->
    <packaging>jar</packaging>

    <!-- name: human-readable name (used in reports, site) -->
    <name>My Spring Application</name>

    <!-- description: project description (used in Maven site) -->
    <description>A Spring Boot REST API for user management</description>

    <!-- url: project homepage (used in Maven site) -->
    <url>https://github.com/example/my-spring-app</url>

    <!-- ── 4. PROPERTIES ───────────────────────────────────────────────────────── -->
    <!-- Key-value pairs used to:
         - Parameterise dependency versions  (${mapstruct.version})
         - Configure plugin settings         (${java.version})
         - Override parent properties
         - Avoid repeating values            (DRY principle) -->
    <properties>
        <!-- Standard Maven properties -->
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <!-- Custom version properties (use in <dependency><version>) -->
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <lombok.version>1.18.30</lombok.version>
        <testcontainers.version>1.19.1</testcontainers.version>

        <!-- Skip tests flag (can override from command line: -DskipTests) -->
        <skipTests>false</skipTests>
    </properties>

    <!-- ── 5. REPOSITORIES ─────────────────────────────────────────────────────── -->
    <!-- WHERE to download dependencies from.
         Maven Central is already configured in Super POM.
         Add here for: private Nexus, JitPack, Spring Milestones, etc. -->
    <repositories>
        <repository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
            <releases><enabled>true</enabled></releases>
            <snapshots><enabled>false</enabled></snapshots>
        </repository>

        <!-- Company private repository -->
        <repository>
            <id>company-nexus</id>
            <name>Company Nexus</name>
            <url>https://nexus.mycompany.com/repository/maven-public/</url>
            <releases><enabled>true</enabled></releases>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>   <!-- always|daily|interval:60|never -->
            </snapshots>
        </repository>

        <!-- Spring Milestones (for RC versions) -->
        <repository>
            <id>spring-milestones</id>
            <url>https://repo.spring.io/milestone</url>
        </repository>
    </repositories>

    <!-- Where to download PLUGIN JARs from (separate from dependency repos) -->
    <pluginRepositories>
        <pluginRepository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
        </pluginRepository>
    </pluginRepositories>

    <!-- ── 6. DEPENDENCIES ─────────────────────────────────────────────────────── -->
    <dependencies>

        <!-- Standard compile-scope dependency (default scope) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- No <version> — managed by spring-boot-starter-parent BOM -->
        </dependency>

        <!-- Runtime scope: JDBC driver not needed at compile time -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Provided scope: Servlet API provided by Tomcat, not bundled in WAR -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>
        </dependency>

        <!-- Test scope: only available in src/test, not in final JAR -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Optional: not transitively included when this JAR is used as a dep -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Exclusion: prevent a transitive dependency from being included -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-classic</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

    </dependencies>

    <!-- ── 7. DEPENDENCY MANAGEMENT ────────────────────────────────────────────── -->
    <!-- Declare versions WITHOUT adding to classpath.
         Used in parent POMs to centralise version management.
         Children inherit these and can use the dep WITHOUT specifying <version>. -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- ── 8. BUILD ────────────────────────────────────────────────────────────── -->
    <build>
        <!-- Override output JAR name (default: {artifactId}-{version}.jar) -->
        <finalName>my-app</finalName>

        <!-- Override standard source directories (usually don't need to) -->
        <sourceDirectory>src/main/java</sourceDirectory>
        <testSourceDirectory>src/test/java</testSourceDirectory>
        <outputDirectory>target/classes</outputDirectory>
        <testOutputDirectory>target/test-classes</testOutputDirectory>

        <!-- Resources: non-Java files to copy to target/classes -->
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <!-- Enable Maven property filtering: ${property} in resource files
                     are replaced with actual values at build time -->
                <filtering>true</filtering>
                <includes>
                    <include>**/*.properties</include>
                    <include>**/*.yml</include>
                </includes>
            </resource>
        </resources>

        <!-- pluginManagement: version declarations only — no execution -->
        <!-- Good place in parent POM to declare versions for all children -->
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.1.2</version>
                </plugin>
            </plugins>
        </pluginManagement>

        <!-- plugins: actual plugin configuration and execution -->
        <plugins>

            <!-- ── Compiler plugin ───────────────────────────────────────── -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <release>21</release>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>

            <!-- ── Spring Boot plugin ────────────────────────────────────── -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

        </plugins>
    </build>

    <!-- ── 9. DISTRIBUTION MANAGEMENT ──────────────────────────────────────────── -->
    <!-- Where to deploy artifacts (mvn deploy) -->
    <distributionManagement>
        <repository>
            <id>nexus-releases</id>
            <url>https://nexus.mycompany.com/repository/maven-releases/</url>
        </repository>
        <snapshotRepository>
            <id>nexus-snapshots</id>
            <url>https://nexus.mycompany.com/repository/maven-snapshots/</url>
        </snapshotRepository>
    </distributionManagement>

    <!-- ── 10. PROFILES ─────────────────────────────────────────────────────────── -->
    <!-- Conditional configuration based on environment or activation condition -->
    <profiles>

        <profile>
            <id>dev</id>
            <!-- Activate when no other profile is active -->
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
            <dependencies>
                <dependency>
                    <groupId>com.h2database</groupId>
                    <artifactId>h2</artifactId>
                    <scope>runtime</scope>
                </dependency>
            </dependencies>
        </profile>

        <profile>
            <id>prod</id>
            <!-- Activate with: mvn package -Pprod -->
            <!-- Or auto-activate based on OS, JDK version, file existence, etc. -->
            <activation>
                <property>
                    <name>env</name>
                    <value>production</value>
                </property>
            </activation>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
        </profile>

    </profiles>

</project>
```

---

#### `settings.xml` — Maven Global Configuration

`settings.xml` configures Maven's behaviour for the **user or system** — things that should NOT be in `pom.xml` (which is project-specific and committed to version control). It lives at:

```text
~/.m2/settings.xml          ← user-level settings (takes priority)
$MAVEN_HOME/conf/settings.xml ← system-level settings (affects all users)
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
                              https://maven.apache.org/xsd/settings-1.2.0.xsd">

    <!-- ── 1. LOCAL REPOSITORY ──────────────────────────────────────────────────── -->
    <!-- Where Maven stores downloaded JARs and installed artifacts.
         Default: ${user.home}/.m2/repository -->
    <localRepository>/custom/path/.m2/repository</localRepository>

    <!-- ── 2. INTERACTIVE MODE ──────────────────────────────────────────────────── -->
    <!-- true = Maven can ask the user questions (prompt for input).
         Set to false for CI/CD pipelines (no user present). -->
    <interactiveMode>true</interactiveMode>

    <!-- ── 3. OFFLINE MODE ──────────────────────────────────────────────────────── -->
    <!-- true = Maven never tries to download anything; uses only local repo.
         Useful for air-gapped environments. -->
    <offline>false</offline>

    <!-- ── 4. PLUGIN GROUPS ─────────────────────────────────────────────────────── -->
    <!-- Short group IDs for plugin prefix resolution.
         Allows 'mvn tomcat7:run' instead of 'mvn org.apache.tomcat.maven:...:run' -->
    <pluginGroups>
        <pluginGroup>org.apache.tomcat.maven</pluginGroup>
        <pluginGroup>org.sonarsource.scanner.maven</pluginGroup>
    </pluginGroups>

    <!-- ── 5. PROXIES ───────────────────────────────────────────────────────────── -->
    <!-- HTTP/HTTPS proxy for Maven to reach the internet (corporate firewall). -->
    <proxies>
        <proxy>
            <id>corporate-proxy</id>
            <active>true</active>
            <protocol>https</protocol>
            <host>proxy.mycompany.com</host>
            <port>8080</port>
            <username>proxyuser</username>
            <password>${env.PROXY_PASSWORD}</password>
            <!-- Don't proxy these hosts (pipe-separated) -->
            <nonProxyHosts>localhost|127.0.0.1|*.mycompany.com</nonProxyHosts>
        </proxy>
    </proxies>

    <!-- ── 6. SERVERS ───────────────────────────────────────────────────────────── -->
    <!-- Authentication credentials for remote repositories and deploy targets.
         The <id> MUST match the <id> in pom.xml's <repository> or
         <distributionManagement>.
         NEVER put passwords in pom.xml — put them here (not committed to Git). -->
    <servers>
        <!-- Nexus release repository credentials -->
        <server>
            <id>nexus-releases</id>
            <username>deploy-user</username>
            <!-- Use env variables or Maven password encryption (mvn --encrypt-password) -->
            <password>${env.NEXUS_PASSWORD}</password>
        </server>

        <!-- Nexus snapshot repository credentials -->
        <server>
            <id>nexus-snapshots</id>
            <username>deploy-user</username>
            <password>${env.NEXUS_PASSWORD}</password>
        </server>

        <!-- GitHub Packages -->
        <server>
            <id>github</id>
            <username>your-github-username</username>
            <password>${env.GITHUB_TOKEN}</password>
        </server>

        <!-- Docker registry (used by plugins like jib-maven-plugin) -->
        <server>
            <id>registry.hub.docker.com</id>
            <username>dockeruser</username>
            <password>${env.DOCKER_PASSWORD}</password>
        </server>

        <!-- SSH key for SCM deploy -->
        <server>
            <id>my-scm-server</id>
            <privateKey>${user.home}/.ssh/id_rsa</privateKey>
            <passphrase>${env.SSH_PASSPHRASE}</passphrase>
        </server>
    </servers>

    <!-- ── 7. MIRRORS ───────────────────────────────────────────────────────────── -->
    <!-- Redirect Maven repository requests to a different URL.
         Used to: route ALL downloads through a corporate Nexus/Artifactory proxy
         so the company controls what libraries are allowed. -->
    <mirrors>
        <!-- Route ALL repo requests through company Nexus -->
        <mirror>
            <id>company-nexus-mirror</id>
            <name>Company Nexus Mirror</name>
            <url>https://nexus.mycompany.com/repository/maven-public/</url>
            <!-- mirrorOf: which repo IDs to mirror.
                 *     = all repositories
                 central = only Maven Central
                 *,!my-private-repo = all except my-private-repo -->
            <mirrorOf>*</mirrorOf>
        </mirror>
    </mirrors>

    <!-- ── 8. PROFILES ──────────────────────────────────────────────────────────── -->
    <!-- Define profiles that can be activated in settings.xml.
         Useful for: machine-specific paths, proxy settings, CI-specific config. -->
    <profiles>
        <profile>
            <id>company-defaults</id>
            <properties>
                <sonar.host.url>https://sonar.mycompany.com</sonar.host.url>
                <sonar.login>${env.SONAR_TOKEN}</sonar.login>
            </properties>
            <repositories>
                <repository>
                    <id>company-releases</id>
                    <url>https://nexus.mycompany.com/repository/maven-releases/</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </repository>
            </repositories>
        </profile>
    </profiles>

    <!-- ── 9. ACTIVE PROFILES ───────────────────────────────────────────────────── -->
    <!-- Profiles always active for this user/machine (no -P flag needed). -->
    <activeProfiles>
        <activeProfile>company-defaults</activeProfile>
    </activeProfiles>

</settings>
```

**Encrypt passwords in `settings.xml`** (Maven security best practice):

```bash
# Step 1: Create a master password
mvn --encrypt-master-password myMasterPassword
# Output: {JYBfRm2...==}   ← copy this

# Step 2: Store it in ~/.m2/settings-security.xml
# <settingsSecurity>
#   <master>{JYBfRm2...==}</master>
# </settingsSecurity>

# Step 3: Encrypt the actual password
mvn --encrypt-password myNexusPassword
# Output: {AKGDcfe...==}   ← use this in settings.xml <password>
```

---

#### How Spring Boot Uses Maven and `pom.xml`

Spring Boot has deep integration with Maven through the `spring-boot-starter-parent` and `spring-boot-maven-plugin`.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot + Maven Integration:                                                │
│                                                                                  │
│  spring-boot-starter-parent  (what you inherit in <parent>)                     │
│      │                                                                           │
│      └── spring-boot-dependencies  (the actual BOM — Bill of Materials)         │
│              │                                                                   │
│              └── Defines <dependencyManagement> for 300+ libraries:              │
│                  jackson 2.x, hibernate 6.x, junit 5.x, tomcat 10.x, ...        │
│                  All pre-tested to work together for this Spring Boot version    │
│                                                                                  │
│  spring-boot-starter-parent also configures:                                    │
│    • maven-compiler-plugin: source/target = ${java.version}                     │
│    • maven-surefire-plugin: JUnit 5 support                                     │
│    • maven-failsafe-plugin: integration tests (*IT.java)                        │
│    • maven-jar-plugin: adds build metadata to MANIFEST.MF                      │
│    • Resource filtering for application.properties                              │
│    • UTF-8 encoding throughout                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**A minimal but complete Spring Boot `pom.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Step 1: Inherit from Spring Boot parent → gets BOM + plugin defaults -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <java.version>21</java.version>   <!-- spring-boot-starter-parent reads this -->
    </properties>

    <!-- Step 2: Add starters — no versions needed (BOM manages them) -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- Step 3: Spring Boot Maven plugin — repackages into executable fat JAR -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <!-- no <version> — managed by spring-boot-starter-parent -->
            </plugin>
        </plugins>
    </build>

</project>
```

**What the `spring-boot-maven-plugin` does at each phase:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  spring-boot-maven-plugin Goals:                                                 │
│                                                                                  │
│  spring-boot:repackage   → Runs during 'package' phase                          │
│    1. Maven jar-plugin creates the regular thin JAR first                        │
│    2. spring-boot-maven-plugin repackages it into a fat JAR:                    │
│       - Renames original JAR to myapp-1.0.0.jar.original                        │
│       - Creates myapp-1.0.0.jar with BOOT-INF/lib/ (all deps inside)            │
│       - Sets MANIFEST.MF Main-Class to JarLauncher                              │
│       - Sets MANIFEST.MF Start-Class to your @SpringBootApplication class       │
│                                                                                  │
│  spring-boot:run         → Does NOT produce a JAR; runs the app directly         │
│    1. Compiles (if needed)                                                       │
│    2. Resolves classpath from pom.xml dependencies                               │
│    3. Forks a JVM: java -cp {all deps} com.example.MyApplication                │
│    4. Supports hot-reload with spring-boot-devtools                              │
│                                                                                  │
│  spring-boot:start / spring-boot:stop → Used in integration test lifecycle      │
│    start: begins the app in background before failsafe:integration-test          │
│    stop:  shuts it down after failsafe:verify                                    │
│                                                                                  │
│  spring-boot:build-info  → Generates build-info.properties                      │
│    Creates target/classes/META-INF/build-info.properties with:                  │
│      build.artifact=my-app                                                       │
│      build.version=0.0.1-SNAPSHOT                                               │
│      build.time=2026-05-11T10:30:00Z                                             │
│    Exposed via: GET /actuator/info                                               │
└──────────────────────────────────────────────────────────────────────────────────┘
```



---

## Gradle — Project Management and Build Automation Tool

---

### What is Gradle?

**Gradle** is an open-source **build automation and project management tool** built on top of the JVM. It was created in 2007 by Hans Dockter and became the official build tool for Android in 2013. Unlike Maven (which uses XML) or Ant (which uses XML scripts), Gradle uses a **Groovy DSL** (`build.gradle`) or **Kotlin DSL** (`build.gradle.kts`) — giving you the full power of a programming language inside your build script.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  What Gradle Manages:                                                            │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │              build.gradle / build.gradle.kts                               │ │
│  │                  (Build Script — Groovy or Kotlin DSL)                     │ │
│  └──────────┬──────────────┬───────────────┬──────────────┬────────────────────┘ │
│             │              │               │              │                      │
│             ▼              ▼               ▼              ▼                      │
│  ┌───────────────┐ ┌─────────────┐ ┌────────────┐ ┌────────────────────┐        │
│  │  Build        │ │  Dependency │ │  Task      │ │  Multi-module      │        │
│  │  Automation   │ │  Resolution │ │  Automation│ │  Management        │        │
│  │               │ │             │ │            │ │                    │        │
│  │  compile      │ │  Downloads  │ │  Custom    │ │  root project      │        │
│  │  test         │ │  JARs from  │ │  tasks     │ │  + subprojects     │        │
│  │  jar/war      │ │  Maven      │ │  lifecycle │ │  share deps +      │        │
│  │  publish      │ │  Central /  │ │  hooks     │ │  config centrally  │        │
│  │               │ │  custom     │ │            │ │                    │        │
│  └───────────────┘ └─────────────┘ └────────────┘ └────────────────────┘        │
│                                                                                  │
│  Key differences from Maven:                                                     │
│  • Groovy/Kotlin DSL  vs  XML (far more readable and concise)                   │
│  • Incremental builds  (only rebuilds what changed — much faster)               │
│  • Build cache         (cache task outputs across machines)                     │
│  • Daemon              (warm JVM kept alive between builds — 2-10x faster)     │
│  • Task graph          (fine-grained DAG of tasks, not fixed lifecycle phases)  │
│  • Convention + flexibility (sensible defaults but easily overridden)            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Before Gradle** (with Maven/Ant):
- Build logic in verbose XML — no conditionals, no loops, no helper functions
- Maven's fixed lifecycle phases were hard to extend without writing plugins in Java
- Ant required writing every task from scratch — no dependency management
- Slow builds: Maven always re-ran everything from scratch

---

### How Gradle Helps Developers — Through `build.gradle` / `build.gradle.kts`

The `build.gradle` (Groovy DSL) or `build.gradle.kts` (Kotlin DSL) is the **heart of every Gradle project**. It is a program, not just configuration — you can write conditional logic, loops, and functions directly in your build script.

#### Build Generation

Gradle has a `java` plugin that provides a complete build lifecycle equivalent to Maven's. The `compileJava` task uses `JavaCompile` under the hood.

```groovy
// build.gradle (Groovy DSL)
plugins {
    id 'java'                          // adds compile, test, jar, etc.
    id 'org.springframework.boot' version '3.3.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

```kotlin
// build.gradle.kts (Kotlin DSL)
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

Running `./gradlew build` executes: `compileJava → processResources → classes → compileTestJava → processTestResources → testClasses → test → jar`.

#### Dependency Resolution

Gradle resolves dependencies the same way Maven does — from a local cache first, then from remote repositories. It uses a **configuration** system (similar to Maven scopes) to separate compile-time, runtime, and test dependencies.

```groovy
// build.gradle — declare repositories and dependencies
repositories {
    mavenCentral()                           // Maven Central
    maven { url 'https://repo.spring.io/milestone' }
}

dependencies {
    // implementation = compile + runtime (replaces Maven 'compile' scope)
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // runtimeOnly = runtime only (replaces Maven 'runtime' scope)
    runtimeOnly 'com.mysql:mysql-connector-j'

    // compileOnly = compile only, NOT in JAR (replaces Maven 'provided' scope)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // testImplementation = test compile + runtime (replaces Maven 'test' scope)
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Gradle Dependency Configuration vs Maven Scope:                                 │
│                                                                                  │
│  Gradle Configuration    │ Maven Scope  │ Available at               │ In JAR   │
│  ────────────────────────┼──────────────┼────────────────────────────┼──────────│
│  implementation          │ compile      │ compile + runtime          │ YES       │
│  api (lib module)        │ compile      │ compile + runtime + caller │ YES       │
│  compileOnly             │ provided     │ compile only               │ NO        │
│  runtimeOnly             │ runtime      │ runtime only               │ YES       │
│  testImplementation      │ test         │ test compile + runtime     │ NO        │
│  testCompileOnly         │ -            │ test compile only          │ NO        │
│  testRuntimeOnly         │ -            │ test runtime only          │ NO        │
│  annotationProcessor     │ -            │ compile time only (APT)    │ NO        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Documentation

Gradle generates Javadoc and test reports automatically:

```bash
./gradlew javadoc            # generate Javadoc → build/docs/javadoc/
./gradlew test               # run tests → build/reports/tests/test/index.html
./gradlew jacocoTestReport   # coverage report → build/reports/jacoco/test/html/
```

```groovy
// Javadoc configuration
javadoc {
    options.encoding = 'UTF-8'
    options.addStringOption('Xdoclint:none', '-quiet')
}
```

#### Task Automation

Unlike Maven (fixed phases), Gradle lets you define **any custom task** and wire it anywhere in the build:

```groovy
// Custom task: copy config files before build
tasks.register('copyConfig', Copy) {
    from 'src/main/config'
    into 'build/resources/main'
}

// Make compileJava depend on our custom task
tasks.named('compileJava') {
    dependsOn 'copyConfig'
}

// Custom task: print git commit hash into build info
tasks.register('generateBuildInfo') {
    doLast {
        def gitHash = 'git rev-parse --short HEAD'.execute().text.trim()
        file('build/resources/main/build.properties').text = "git.commit=${gitHash}\n"
    }
}
```

#### Multi-module Management

Gradle handles multi-module (multi-project) builds with a `settings.gradle` file at the root plus subproject `build.gradle` files. Shared configuration goes in the root `build.gradle`.

```groovy
// settings.gradle — declares all subprojects
rootProject.name = 'my-microservices'
include 'user-service'
include 'order-service'
include 'common-lib'
```

```groovy
// root build.gradle — shared config for ALL subprojects
subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    group = 'com.example'
    version = '1.0.0-SNAPSHOT'

    repositories {
        mavenCentral()
    }

    dependencyManagement {
        imports {
            mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.0'
        }
    }

    dependencies {
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
    }
}
```

---

### Gradle Project Structure

Gradle uses the **same standard directory layout as Maven** for Java projects (inherited from convention). The key difference is the build scripts.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Gradle Standard Project Structure:                                              │
│                                                                                  │
│  my-project/                                                                     │
│  ├── build.gradle          ← Build script (Groovy DSL)     ┐                    │
│  │   OR                                                     │ One of these       │
│  ├── build.gradle.kts      ← Build script (Kotlin DSL)     ┘                    │
│  │                                                                               │
│  ├── settings.gradle       ← Project settings + multi-module config ┐           │
│  │   OR                                                               │           │
│  ├── settings.gradle.kts   ← Same but Kotlin DSL           ──────────┘           │
│  │                                                                               │
│  ├── gradlew               ← Gradle Wrapper script (Unix)                        │
│  ├── gradlew.bat           ← Gradle Wrapper script (Windows)                     │
│  ├── gradle/                                                                     │
│  │   └── wrapper/                                                                │
│  │       ├── gradle-wrapper.jar         ← Wrapper bootstrap JAR                 │
│  │       └── gradle-wrapper.properties  ← Pins Gradle version                   │
│  │                                                                               │
│  ├── src/                                                                        │
│  │   ├── main/                          ← Production code                        │
│  │   │   ├── java/                      ← Java source files                      │
│  │   │   │   └── com/example/                                                   │
│  │   │   │       ├── MyApp.java                                                  │
│  │   │   │       ├── controller/                                                 │
│  │   │   │       ├── service/                                                    │
│  │   │   │       └── repository/                                                 │
│  │   │   └── resources/                 ← Non-Java resources (classpath)         │
│  │   │       ├── application.properties                                          │
│  │   │       └── logback-spring.xml                                              │
│  │   │                                                                           │
│  │   └── test/                          ← Test code                              │
│  │       ├── java/                      ← Test source files                      │
│  │       │   └── com/example/                                                   │
│  │       │       └── UserServiceTest.java                                        │
│  │       └── resources/                 ← Test-only resources                    │
│  │           └── application-test.properties                                     │
│  │                                                                               │
│  └── build/                             ← BUILD OUTPUT (generated, not committed)│
│      ├── classes/                                                                │
│      │   ├── java/main/                 ← Compiled .class files                  │
│      │   └── java/test/                 ← Compiled test .class files             │
│      ├── resources/main/               ← Processed resources                    │
│      ├── libs/                          ← Final JAR / WAR artifacts              │
│      │   └── my-project-1.0.0.jar                                               │
│      ├── reports/                       ← Test reports, coverage, etc.           │
│      │   ├── tests/test/index.html      ← Unit test HTML report                  │
│      │   └── jacoco/test/html/          ← JaCoCo coverage report                 │
│      ├── test-results/                  ← JUnit XML test results                  │
│      ├── tmp/                           ← Temporary build files                  │
│      └── docs/javadoc/                  ← Generated Javadoc                      │
│                                                                                  │
│  Multi-module project structure:                                                 │
│  my-microservices/                                                               │
│  ├── settings.gradle        ← includes all subprojects                           │
│  ├── build.gradle           ← shared config for all subprojects                  │
│  ├── user-service/                                                               │
│  │   ├── build.gradle       ← user-service specific config                       │
│  │   └── src/                                                                    │
│  ├── order-service/                                                              │
│  │   ├── build.gradle                                                            │
│  │   └── src/                                                                    │
│  └── common-lib/                                                                 │
│      ├── build.gradle                                                            │
│      └── src/                                                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### All Gradle Commands

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Gradle Commands — Complete Reference                                            │
│                                                                                  │
│  WRAPPER GENERATION                                                              │
│  gradle wrapper                  Generate gradlew + gradle/wrapper/ files        │
│  gradle wrapper --gradle-version 8.7   Generate wrapper for specific version    │
│                                                                                  │
│  ALWAYS USE ./gradlew AFTER WRAPPER IS GENERATED (not 'gradle')                 │
│                                                                                  │
│  BUILD LIFECYCLE TASKS                                                           │
│  ./gradlew build                 Compile + test + jar (full build)              │
│  ./gradlew clean                 Delete the build/ directory                    │
│  ./gradlew clean build           Clean then full build                          │
│  ./gradlew assemble              Compile + jar but NO tests                     │
│  ./gradlew check                 Run all verification tasks (tests + lint)      │
│                                                                                  │
│  COMPILE TASKS                                                                   │
│  ./gradlew compileJava           Compile src/main/java → build/classes/java/main│
│  ./gradlew compileTestJava       Compile src/test/java                          │
│  ./gradlew processResources      Copy src/main/resources → build/resources/main │
│                                                                                  │
│  TEST TASKS                                                                      │
│  ./gradlew test                  Run unit tests                                 │
│  ./gradlew test --tests "*.UserServiceTest"           Run specific test class   │
│  ./gradlew test --tests "*.UserServiceTest.findById"  Run specific test method  │
│  ./gradlew -x test build         Build but skip tests                           │
│  ./gradlew test --rerun-tasks    Force re-run tests (ignore UP-TO-DATE cache)   │
│                                                                                  │
│  JAR / WAR TASKS                                                                 │
│  ./gradlew jar                   Build plain JAR → build/libs/                  │
│  ./gradlew bootJar               Build Spring Boot fat JAR                      │
│  ./gradlew war                   Build WAR file (requires 'war' plugin)         │
│  ./gradlew bootWar               Build Spring Boot WAR                          │
│                                                                                  │
│  DEPENDENCY TASKS                                                                │
│  ./gradlew dependencies          Print full dependency tree for all configs     │
│  ./gradlew dependencies --configuration compileClasspath   Specific config      │
│  ./gradlew dependencyInsight --dependency spring-core   Why is X included?      │
│  ./gradlew buildEnvironment      Show buildscript classpath dependencies        │
│                                                                                  │
│  TASK DISCOVERY                                                                  │
│  ./gradlew tasks                 List all available tasks (with descriptions)   │
│  ./gradlew tasks --all           List ALL tasks including unlisted ones         │
│  ./gradlew help --task compileJava    Show details about a specific task        │
│  ./gradlew properties            Print all project properties                  │
│                                                                                  │
│  PUBLISH TASKS                                                                   │
│  ./gradlew publishToMavenLocal   Install JAR to ~/.m2/repository (mavenLocal)   │
│  ./gradlew publish               Publish to remote repos (publishing block)     │
│  ./gradlew publishMavenPublicationToNexusRepository  Publish to specific repo   │
│                                                                                  │
│  MULTI-PROJECT TASKS                                                             │
│  ./gradlew :user-service:build   Run task in specific subproject                │
│  ./gradlew :user-service:test    Test only a specific subproject                │
│  ./gradlew --project-dir user-service build  Same from different directory      │
│                                                                                  │
│  PERFORMANCE / DIAGNOSIS                                                         │
│  ./gradlew build --scan          Upload build scan to scans.gradle.com          │
│  ./gradlew build --profile       Generate HTML performance profile              │
│  ./gradlew build --info          Info-level logging                             │
│  ./gradlew build --debug         Debug-level logging (very verbose)             │
│  ./gradlew build --stacktrace    Show full stack traces on error                │
│  ./gradlew build --no-daemon     Don't use the Gradle Daemon                    │
│  ./gradlew build --parallel      Build subprojects in parallel                  │
│  ./gradlew build --continue      Continue build even after task failures        │
│  ./gradlew build --offline       Use only local cache, no network               │
│  ./gradlew build -t              Continuous build (watch for file changes)      │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### `gradle wrapper` — The Gradle Wrapper

The **Gradle Wrapper** is equivalent to Maven's `mvnw`. It pins a specific Gradle version to your project so all developers and CI/CD pipelines use the same version without any global installation.

```bash
# Generate the Gradle Wrapper (uses currently installed Gradle version)
gradle wrapper

# Pin a specific Gradle version
gradle wrapper --gradle-version 8.7

# This creates:
#   gradlew                           Unix shell script
#   gradlew.bat                       Windows batch script
#   gradle/wrapper/gradle-wrapper.jar        Bootstrap JAR
#   gradle/wrapper/gradle-wrapper.properties Version config

# ALWAYS use ./gradlew instead of gradle after this
./gradlew build
./gradlew test
```

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

When `./gradlew` is run:
1. The script checks if the specified Gradle version is cached in `~/.gradle/wrapper/dists/`
2. If not, downloads it automatically from `distributionUrl`
3. Runs your Gradle command with that exact version

---

### Gradle Build Lifecycle

Gradle's build lifecycle has **3 phases**, not Maven's sequential phases. These are fundamentally different from Maven:

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Gradle Build Lifecycle — 3 Phases:                                              │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │  Phase 1: INITIALIZATION                                                   │ │
│  │                                                                             │ │
│  │  Gradle reads settings.gradle (or settings.gradle.kts)                     │ │
│  │  Determines: single-project or multi-project build                         │ │
│  │  Creates Project objects for each subproject                               │ │
│  │                                                                             │ │
│  │  Files evaluated: settings.gradle → build.gradle (init scripts first)      │ │
│  └──────────────────────────────────┬──────────────────────────────────────────┘ │
│                                     │                                            │
│                                     ▼                                            │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │  Phase 2: CONFIGURATION                                                    │ │
│  │                                                                             │ │
│  │  Gradle evaluates build.gradle for EVERY project in the build              │ │
│  │  ALL task configuration code runs (regardless of which task you asked for) │ │
│  │  Builds a DAG (Directed Acyclic Graph) of all tasks and their dependencies │ │
│  │                                                                             │ │
│  │  Example: ./gradlew test                                                   │ │
│  │  Gradle still configures jar, compileJava, processResources, etc.          │ │
│  │  (configuration ≠ execution)                                               │ │
│  └──────────────────────────────────┬──────────────────────────────────────────┘ │
│                                     │                                            │
│                                     ▼                                            │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │  Phase 3: EXECUTION                                                        │ │
│  │                                                                             │ │
│  │  Gradle executes only the tasks requested + their dependencies             │ │
│  │  Tasks run in dependency order (DAG order)                                 │ │
│  │  Tasks with no changes are marked UP-TO-DATE and skipped (incremental)     │ │
│  │                                                                             │ │
│  │  ./gradlew test executes:                                                  │ │
│  │  compileJava → processResources → classes →                               │ │
│  │  compileTestJava → processTestResources → testClasses → test               │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### Phase 1 — Initialization

```bash
# settings.gradle is processed first
# Gradle decides: single-project or multi-project
```

```groovy
// settings.gradle
rootProject.name = 'my-spring-app'      // sets the root project name

// For multi-module builds, include subprojects:
// include 'user-service'
// include 'order-service'
```

No build tasks run here. Only project objects are created.

#### Phase 2 — Configuration

Every `build.gradle` is **evaluated** (all top-level code runs). Tasks are **configured** (their inputs/outputs/configuration are registered) but NOT yet executed.

```groovy
// This code runs during CONFIGURATION phase (when build.gradle is evaluated)
println "Configuring project: ${project.name}"

tasks.register('myTask') {
    // This CLOSURE runs during configuration — just registers the task
    description = 'My custom task'
    group = 'custom'

    doLast {
        // This CLOSURE runs during EXECUTION phase only
        println "Running myTask!"
    }
}
```

The key is: **configuration closures** run at configuration time; **`doFirst` / `doLast` closures** run at execution time.

#### Phase 3 — Execution

Gradle runs only the requested tasks and their transitive task dependencies, in the correct order determined by the DAG.

```bash
./gradlew test
# Execution order:
#   1. compileJava        (compiles src/main/java)
#   2. processResources   (copies src/main/resources)
#   3. classes            (lifecycle task — depends on compileJava + processResources)
#   4. compileTestJava    (compiles src/test/java)
#   5. processTestResources
#   6. testClasses        (lifecycle task)
#   7. test               (runs unit tests)
```

---

#### `compileJava` — Compile Source Code

The `java` plugin adds the `compileJava` task which uses the **`JavaCompile`** type. It compiles all `.java` files in `src/main/java/`.

```bash
./gradlew compileJava
```

```text
Input:   src/main/java/**/*.java
Output:  build/classes/java/main/**/*.class
```

```groovy
// Configuring the compileJava task
tasks.named('compileJava') {
    options.encoding = 'UTF-8'
    options.compilerArgs << '-parameters'     // keep parameter names (useful for Spring)
    options.compilerArgs << '-Xlint:unchecked'
}

// Or configure java extension globally
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    // toolchain: pin the exact JDK version (Gradle downloads it if missing)
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

```text
After ./gradlew compileJava:

build/
└── classes/
    └── java/
        └── main/
            └── com/
                └── example/
                    ├── MyApp.class
                    ├── controller/
                    │   └── UserController.class
                    └── service/
                        └── UserService.class
```

---

#### `test` — Run Unit Tests

The `java` plugin adds the `test` task which uses the **`Test`** type. It compiles test sources and runs them using JUnit or TestNG.

```bash
./gradlew test
```

```text
Input:   build/classes/java/test/ (compiled from src/test/java)
Output:  build/test-results/test/    (JUnit XML)
         build/reports/tests/test/   (HTML report — open in browser)
```

**Skipping tests:**

```bash
# Skip the test task entirely (most common — equivalent to -DskipTests in Maven)
./gradlew build -x test

# Skip tests but run other verification tasks
./gradlew check -x test

# Run only a specific test class
./gradlew test --tests "com.example.UserServiceTest"

# Run only a specific test method
./gradlew test --tests "com.example.UserServiceTest.findById_returnsUser"

# Force re-run tests even if nothing changed (Gradle normally skips UP-TO-DATE tasks)
./gradlew test --rerun-tasks
```

**Skipping tests in `build.gradle`:**

```groovy
// Option 1: Disable the test task globally
tasks.named('test') {
    enabled = false
}

// Option 2: Skip based on a project property
// Run with: ./gradlew build -PskipTests
if (project.hasProperty('skipTests')) {
    tasks.named('test') { enabled = false }
}

// Option 3: Configure test task (common useful settings)
tasks.named('test') {
    useJUnitPlatform()         // required for JUnit 5

    // Parallel test execution
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1

    // Fail fast: stop after first test failure
    failFast = true

    // Show test output in console
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}
```

```kotlin
// build.gradle.kts
tasks.named<Test>("test") {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    // Skip tests via property: ./gradlew build -PskipTests
    if (project.hasProperty("skipTests")) {
        enabled = false
    }
}
```

---

#### `jar` / `bootJar` — Package the Compiled Code

The `java` plugin adds the `jar` task. The `org.springframework.boot` plugin adds `bootJar` (fat JAR with all dependencies).

```bash
./gradlew jar         # build/libs/my-project-1.0.0.jar  (thin JAR — no deps)
./gradlew bootJar     # build/libs/my-project-1.0.0.jar  (fat JAR — all deps inside)
./gradlew bootWar     # build/libs/my-project-1.0.0.war  (Spring Boot WAR)
```

```text
Output location:
build/libs/{project.name}-{version}.jar          plain JAR (jar task)
build/libs/{project.name}-{version}.jar          fat JAR (bootJar task)
build/libs/{project.name}-{version}-plain.jar    thin JAR alongside fat JAR
```

```groovy
// Customise the JAR task
tasks.named('bootJar') {
    archiveFileName = 'myapp.jar'        // override default name
    mainClass = 'com.example.MyApplication'
    layered {
        enabled = true                   // layered JAR for better Docker caching
    }
}

// Customise the plain jar task (Spring Boot disables it by default)
tasks.named('jar') {
    enabled = false   // disable plain JAR when using bootJar (default in Spring Boot)
}
```

**Running the packaged JAR:**

```bash
# Run a Spring Boot fat JAR
java -jar build/libs/my-project-1.0.0.jar

# With arguments
java -jar build/libs/my-project-1.0.0.jar --server.port=9090
java -jar build/libs/my-project-1.0.0.jar --spring.profiles.active=prod

# Run without packaging (faster during development)
./gradlew bootRun
./gradlew bootRun --args='--server.port=9090'
./gradlew bootRun --args='--spring.profiles.active=prod'

# Run a WAR deployed to Tomcat
cp build/libs/my-project-1.0.0.war /opt/tomcat/webapps/
/opt/tomcat/bin/startup.sh
```

---

#### `check` — Verify the Package Integrity

The `check` lifecycle task depends on `test` and any other verification tasks (checkstyle, PMD, JaCoCo). It is the Gradle equivalent of Maven's `verify` phase.

```bash
./gradlew check      # runs test + all registered verification tasks
```

**Checkstyle** (code style enforcement):

```groovy
// build.gradle — add Checkstyle plugin
plugins {
    id 'checkstyle'
}

checkstyle {
    toolVersion = '10.14.2'
    configFile = file('config/checkstyle/checkstyle.xml')  // your rules file
    maxWarnings = 0         // fail build on any warning
    ignoreFailures = false  // fail build on violations (default)
}

// checkstyleMain task runs on: ./gradlew checkstyleMain
// checkstyleTest task runs on: ./gradlew checkstyleTest
// Both run automatically when: ./gradlew check
```

**PMD** (static analysis — find bugs, bad patterns):

```groovy
plugins {
    id 'pmd'
}

pmd {
    toolVersion = '6.55.0'
    ruleSetFiles = files('config/pmd/rules.xml')
    ruleSets = []                          // don't use default rules, use our file
    ignoreFailures = false
    consoleOutput = true
}

// Tasks: pmdMain, pmdTest — both run with ./gradlew check
```

**JaCoCo** (code coverage — fail build below threshold):

```groovy
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = '0.8.10'
}

tasks.named('test') {
    finalizedBy tasks.named('jacocoTestReport')   // always generate report after tests
}

tasks.named('jacocoTestReport') {
    dependsOn tasks.named('test')
    reports {
        xml.required = true     // for SonarQube/CI
        html.required = true    // human-readable
    }
}

// Add a coverage check — fails build if coverage is below threshold
tasks.register('jacocoTestCoverageVerification', JacocoCoverageVerification) {
    dependsOn tasks.named('jacocoTestReport')
    violationRules {
        rule {
            limit {
                minimum = 0.80    // 80% line coverage required
            }
        }
    }
}

// Wire into check
tasks.named('check') {
    dependsOn tasks.named('jacocoTestCoverageVerification')
}
```

```bash
./gradlew check
# Runs: test → jacocoTestReport → jacocoTestCoverageVerification
#       + checkstyleMain + checkstyleTest + pmdMain + pmdTest
# HTML report: build/reports/jacoco/test/html/index.html
```

---

#### `publishToMavenLocal` — Install to Local Repository

Gradle's equivalent of `mvn install`. Publishes the JAR to the local Maven repository (`~/.m2/repository`) so other local projects can use it.

```bash
./gradlew publishToMavenLocal
```

**What is the local repository?**

```text
~/.m2/repository/                         ← root (same as Maven's local repo)
└── com/
    └── example/
        └── my-library/
            └── 1.0.0/
                ├── my-library-1.0.0.jar
                ├── my-library-1.0.0.pom
                └── my-library-1.0.0.jar.md5
```

Gradle shares Maven's local repository at `~/.m2/repository` — published artifacts from Gradle can be consumed by Maven projects and vice versa.

**Setup `publishToMavenLocal`:**

```groovy
// build.gradle
plugins {
    id 'java'
    id 'maven-publish'           // required for publish tasks
}

publishing {
    publications {
        maven(MavenPublication) {
            groupId = 'com.example'
            artifactId = 'my-library'
            version = '1.0.0'
            from components.java    // include the built JAR + generated POM
        }
    }
}

// Now you can run:
// ./gradlew publishToMavenLocal
// → installs to ~/.m2/repository/com/example/my-library/1.0.0/
```

**Using the installed artifact in another Gradle project:**

```groovy
// consumer project's build.gradle
repositories {
    mavenLocal()      // check ~/.m2/repository FIRST
    mavenCentral()    // then Maven Central
}

dependencies {
    implementation 'com.example:my-library:1.0.0'
}
```

**Updating / refreshing the local repository:**

```bash
# Re-publish your library to local repo (overwrites previous version)
./gradlew publishToMavenLocal

# Force re-download all remote dependencies (ignore local cache in ~/.gradle/caches/)
./gradlew build --refresh-dependencies

# The Gradle dependency cache is at:
# ~/.gradle/caches/modules-2/files-2.1/
# (different from ~/.m2/repository — Gradle has its own cache format)
```

---

#### `publish` — Deploy to Remote Repository

Publishes the JAR to a remote artifact repository (Nexus, Artifactory, GitHub Packages).

```groovy
// build.gradle — full publishing configuration
plugins {
    id 'java'
    id 'maven-publish'
}

java {
    withJavadocJar()      // attach javadoc JAR to publication
    withSourcesJar()      // attach sources JAR to publication
}

publishing {
    publications {
        maven(MavenPublication) {
            groupId = project.group
            artifactId = project.name
            version = project.version
            from components.java      // main JAR + pom + javadoc JAR + sources JAR

            // Customise the generated POM
            pom {
                name = 'My Library'
                description = 'A shared utility library'
                url = 'https://github.com/example/my-library'
                licenses {
                    license {
                        name = 'The Apache License, Version 2.0'
                        url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                    }
                }
            }
        }
    }

    repositories {
        // Remote repository 1: Nexus releases
        maven {
            name = 'Nexus'     // becomes: publishMavenPublicationToNexusRepository
            def releasesUrl = 'https://nexus.mycompany.com/repository/maven-releases/'
            def snapshotsUrl = 'https://nexus.mycompany.com/repository/maven-snapshots/'
            url = version.endsWith('SNAPSHOT') ? snapshotsUrl : releasesUrl

            // Credentials — ALWAYS use environment variables, never hardcode
            credentials {
                username = System.getenv('NEXUS_USERNAME') ?: project.findProperty('nexusUsername')
                password = System.getenv('NEXUS_PASSWORD') ?: project.findProperty('nexusPassword')
            }
        }

        // Remote repository 2: GitHub Packages
        maven {
            name = 'GitHubPackages'
            url = 'https://maven.pkg.github.com/example/my-library'
            credentials {
                username = System.getenv('GITHUB_ACTOR')
                password = System.getenv('GITHUB_TOKEN')
            }
        }
    }
}
```

```kotlin
// build.gradle.kts — Kotlin DSL equivalent
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "Nexus"
            val releasesUrl = "https://nexus.mycompany.com/repository/maven-releases/"
            val snapshotsUrl = "https://nexus.mycompany.com/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl)
            credentials {
                username = System.getenv("NEXUS_USERNAME")
                password = System.getenv("NEXUS_PASSWORD")
            }
        }
    }
}
```

```bash
# Publish to all configured repositories
./gradlew publish

# Publish to a specific repository (useful when you have multiple)
./gradlew publishMavenPublicationToNexusRepository
./gradlew publishMavenPublicationToGitHubPackagesRepository

# Publish to local Maven repo (~/.m2)
./gradlew publishToMavenLocal
```

**Storing credentials in `~/.gradle/gradle.properties`** (not committed to Git):

```properties
# ~/.gradle/gradle.properties
nexusUsername=deploy-user
nexusPassword=secret123
```

```groovy
// Use in build.gradle with project.findProperty()
credentials {
    username = project.findProperty('nexusUsername') ?: System.getenv('NEXUS_USERNAME')
    password = project.findProperty('nexusPassword') ?: System.getenv('NEXUS_PASSWORD')
}
```

---

### How to Run Tasks Individually and Why Dependencies Run Automatically

```bash
# Run a specific task
./gradlew compileJava
./gradlew test
./gradlew jar
./gradlew bootJar
./gradlew checkstyleMain
./gradlew jacocoTestReport

# Run a task in a specific subproject
./gradlew :user-service:compileJava
./gradlew :user-service:test
```

**Why do dependent tasks run automatically?**

Gradle builds a **Directed Acyclic Graph (DAG)** of all tasks and their `dependsOn` relationships. When you ask for a task, Gradle walks the graph and runs all upstream dependencies first.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Task Dependency DAG — ./gradlew test:                                           │
│                                                                                  │
│                          test                                                    │
│                         /    \                                                   │
│              testClasses      (nothing else)                                     │
│             /          \                                                         │
│  compileTestJava   processTestResources                                          │
│       |                                                                          │
│    classes                                                                       │
│   /       \                                                                      │
│  compileJava  processResources                                                   │
│                                                                                  │
│  Execution order (leaf nodes first):                                             │
│  1. compileJava                                                                  │
│  2. processResources                                                             │
│  3. classes           (UP-TO-DATE if nothing changed → SKIPPED)                 │
│  4. compileTestJava                                                              │
│  5. processTestResources                                                         │
│  6. testClasses                                                                  │
│  7. test                                                                         │
│                                                                                  │
│  Why? — test requires compiled test classes → compileTestJava must run first    │
│  compileTestJava requires compiled main classes → compileJava must run first    │
│  This is enforced by: tasks.named('test') { dependsOn 'compileTestJava' }       │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Task dependency declaration:**

```groovy
// Explicitly declare task A must run before task B
tasks.named('myTask') {
    dependsOn 'compileJava'      // compileJava must run before myTask
    mustRunAfter 'processResources'   // if both are requested, ensure ordering
    shouldRunAfter 'clean'            // soft ordering (not enforced)
}

// finalizedBy: run task B after task A (even if A fails)
tasks.named('test') {
    finalizedBy 'jacocoTestReport'
}
```

**Incremental builds (UP-TO-DATE):**

Gradle tracks task inputs and outputs. If neither has changed since the last run, the task is marked `UP-TO-DATE` and **skipped** — no re-execution. This is one of Gradle's biggest performance advantages over Maven.

```bash
./gradlew test
# > Task :compileJava UP-TO-DATE       ← skipped, nothing changed
# > Task :processResources UP-TO-DATE
# > Task :compileTestJava UP-TO-DATE
# > Task :test UP-TO-DATE

# Force all tasks to re-run (ignore UP-TO-DATE)
./gradlew test --rerun-tasks
```

---

### What is a Gradle Plugin?

A **Gradle plugin** is a reusable bundle of tasks, conventions, and configuration that can be applied to a project. Plugins are the primary way to extend Gradle's capabilities — there is no built-in `compileJava`; the `java` plugin provides it.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Plugin Concepts:                                                                │
│                                                                                  │
│  Core plugins     = bundled with Gradle (java, application, war, jacoco, pmd)   │
│  Community plugins = published to plugins.gradle.org (Spring Boot, SonarQube)   │
│  Convention plugins = plugins you write in buildSrc/ to share across subprojects│
│                                                                                  │
│  What a plugin does:                                                             │
│  1. Adds tasks to the project (compileJava, test, jar, bootJar, ...)            │
│  2. Adds configurations (implementation, testImplementation, ...)               │
│  3. Applies conventions (standard directory layout, naming, etc.)               │
│  4. Extends the DSL (adds new blocks: java { }, checkstyle { }, ...)            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Applying plugins:**

```groovy
// build.gradle — three ways to apply a plugin

// 1. Plugins DSL block (preferred — resolves from plugins.gradle.org)
plugins {
    id 'java'                                           // core plugin (no version)
    id 'application'                                    // core plugin
    id 'org.springframework.boot' version '3.3.0'       // community plugin
    id 'io.spring.dependency-management' version '1.1.4'
    id 'jacoco'                                         // core plugin
    id 'checkstyle'                                     // core plugin
    id 'pmd'                                            // core plugin
}

// 2. apply() method (legacy style — still works)
apply plugin: 'java'
apply plugin: 'maven-publish'

// 3. Fully qualified plugin class (rare, used for custom/local plugins)
apply plugin: com.example.MyCustomPlugin
```

```kotlin
// build.gradle.kts — Kotlin DSL
plugins {
    java
    application
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
    jacoco
    checkstyle
}
```

**What each common plugin adds:**

```text
Plugin              Tasks added                          Configuration added
───────────────────────────────────────────────────────────────────────────────
java                compileJava, compileTestJava,         implementation,
                    processResources, test, jar, javadoc  testImplementation, ...
application         run, installDist, distZip, distTar    mainClass property
war                 war (instead of jar)                  providedCompile, ...
spring-boot         bootJar, bootWar, bootRun             spring-boot DSL block
dependency-mgmt     (no new tasks)                        dependencyManagement {}
maven-publish       publish, publishToMavenLocal          publishing {}
jacoco              jacocoTestReport, jacocoTestCoverage  jacoco {}
checkstyle          checkstyleMain, checkstyleTest        checkstyle {}
pmd                 pmdMain, pmdTest                      pmd {}
```

---

### Adding and Modifying Tasks in `build.gradle`

Gradle provides a full API for creating, configuring, and wiring tasks.

```groovy
// ── 1. Register a new custom task ────────────────────────────────────────────────
tasks.register('hello') {
    group = 'custom'
    description = 'Prints a hello message'

    doLast {
        println "Hello from Gradle! Project: ${project.name}"
    }
}

// ── 2. Configure an existing task ─────────────────────────────────────────────────
// Always prefer tasks.named() over tasks.getByName() — lazy evaluation
tasks.named('compileJava') {
    options.encoding = 'UTF-8'
    options.compilerArgs += ['-parameters', '-Xlint:unchecked']
}

tasks.named('test') {
    useJUnitPlatform()
    maxParallelForks = 4
    testLogging {
        events 'passed', 'skipped', 'failed'
    }
}

// ── 3. Create a typed task ─────────────────────────────────────────────────────────
tasks.register('copyDocs', Copy) {
    from 'docs/'
    into "${buildDir}/site/docs"
    include '**/*.md'
    rename '(.+)\\.md', '$1.html'  // rename .md to .html
}

tasks.register('zipArtifacts', Zip) {
    from "${buildDir}/libs"
    archiveFileName = "release-${version}.zip"
    destinationDirectory = file("${buildDir}/dist")
}

// ── 4. Lifecycle hooks — doFirst / doLast on existing tasks ──────────────────────
tasks.named('jar') {
    doFirst {
        println "About to create JAR for version ${project.version}"
    }
    doLast {
        println "JAR created at: ${archivePath}"
    }
}

// ── 5. Wire tasks with dependsOn / finalizedBy ────────────────────────────────────
tasks.named('build') {
    dependsOn 'copyDocs'    // copyDocs must complete before build
}

tasks.named('test') {
    finalizedBy 'jacocoTestReport'   // always run report, even if tests fail
}

// ── 6. Task graph lifecycle hooks ─────────────────────────────────────────────────
// Run code AFTER the task graph is built (all tasks configured)
gradle.taskGraph.whenReady { graph ->
    if (graph.hasTask(':publish')) {
        // Enforce: no SNAPSHOT versions when publishing
        if (version.endsWith('SNAPSHOT')) {
            throw new GradleException("Cannot publish a SNAPSHOT version!")
        }
    }
}

// ── 7. Project lifecycle hooks ────────────────────────────────────────────────────
// afterEvaluate: run code AFTER this project's build.gradle is fully evaluated
afterEvaluate {
    if (project.plugins.hasPlugin('spring-boot')) {
        tasks.named('bootJar') {
            archiveFileName = "${project.name}-${version}.jar"
        }
    }
}

// allprojects / subprojects hooks
subprojects {
    afterEvaluate {
        // Apply to every subproject after it is configured
        tasks.withType(JavaCompile).configureEach {
            options.encoding = 'UTF-8'
        }
    }
}
```

---

### How Gradle Depends on `build.gradle` / `build.gradle.kts`

`build.gradle` (or `build.gradle.kts`) is the **single source of truth** for everything Gradle does with your project. Without it, Gradle has nothing to do.

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  build.gradle is the Contract Between You and Gradle:                            │
│                                                                                  │
│  1. plugins block      → which plugins to apply → which tasks are available     │
│                         Without 'java' plugin, there is no compileJava task     │
│                                                                                  │
│  2. repositories block → WHERE to download dependencies from                    │
│                         Without this, Gradle cannot resolve any dependency      │
│                                                                                  │
│  3. dependencies block → WHAT dependencies to add to each configuration         │
│                         Defines compile/runtime/test classpaths                 │
│                                                                                  │
│  4. group/version      → project identity (used in published artifact names)   │
│                                                                                  │
│  5. java block         → JDK version settings for compilation                  │
│                                                                                  │
│  6. tasks block        → custom tasks and task configuration                    │
│                                                                                  │
│  7. publishing block   → WHERE to publish artifacts and what to include         │
│                                                                                  │
│  8. Test configuration → JUnit platform, parallelism, skip rules                │
│                                                                                  │
│  At every build invocation, Gradle reads and evaluates build.gradle FIRST        │
│  (configuration phase), then executes the requested task graph (execution phase) │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

### Parent and Child Projects — Multi-Module Builds

In Gradle, **multi-project builds** are equivalent to Maven's multi-module builds. The structure is declared in `settings.gradle`, and shared configuration lives in the root `build.gradle`.

#### `settings.gradle` — The Root of a Multi-Project Build

```groovy
// settings.gradle
rootProject.name = 'my-microservices'   // sets the root project name

// Include subprojects (each must have their own build.gradle)
include 'common-lib'
include 'user-service'
include 'order-service'
include 'payment-service'

// Subprojects can be in subdirectories with different names:
include 'services:notification'         // maps to services/notification/
project(':services:notification').projectDir = file('services/notification')

// File system layout:
// my-microservices/
//   settings.gradle
//   build.gradle           ← root build script (shared config)
//   common-lib/
//     build.gradle
//     src/
//   user-service/
//     build.gradle
//     src/
//   order-service/
//     build.gradle
//     src/
```

#### Root `build.gradle` — Shared Configuration

```groovy
// root build.gradle — configure ALL subprojects
plugins {
    id 'org.springframework.boot' version '3.3.0' apply false
    id 'io.spring.dependency-management' version '1.1.4' apply false
    // apply false = declare the plugin version here but don't apply to root project
    // subprojects apply it themselves (or via subprojects block below)
}

// allprojects: applies to root + all subprojects
allprojects {
    group = 'com.example'
    version = '1.0.0-SNAPSHOT'

    repositories {
        mavenCentral()
    }
}

// subprojects: applies to all subprojects only (not root)
subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // Shared BOM — all subprojects get Spring Boot dependency versions
    dependencyManagement {
        imports {
            mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
        }
    }

    // Shared test dependencies for all subprojects
    dependencies {
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
    }

    tasks.named('test') {
        useJUnitPlatform()
    }
}
```

#### Child `build.gradle` — Subproject-Specific Configuration

```groovy
// user-service/build.gradle
plugins {
    id 'org.springframework.boot'      // no version — declared in root with apply false
}

dependencies {
    // Spring Boot starter — no version, managed by root's dependencyManagement BOM
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'

    // Depend on a sibling subproject
    implementation project(':common-lib')
}
```

**How multi-module hierarchy is resolved:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Multi-Project Resolution Order:                                                 │
│                                                                                  │
│  1. settings.gradle processed → all subproject objects created                  │
│                                                                                  │
│  2. Root build.gradle evaluated → allprojects / subprojects closures registered │
│                                                                                  │
│  3. Each subproject's build.gradle evaluated in order                           │
│     → Root's subprojects {} config is applied to each                           │
│     → Subproject's own build.gradle adds/overrides on top                       │
│                                                                                  │
│  4. Task graph built across ALL projects                                         │
│     → project(':common-lib') dependency → Gradle knows to build common-lib first│
│                                                                                  │
│  5. Tasks executed in correct order                                              │
│     common-lib:compileJava → user-service:compileJava → ...                     │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**Running tasks across subprojects:**

```bash
# Build everything (root + all subprojects)
./gradlew build

# Build only one subproject
./gradlew :user-service:build

# Run tests in all subprojects in parallel
./gradlew test --parallel

# Run tests in one subproject
./gradlew :user-service:test
```

---

### All Parts of `build.gradle` / `build.gradle.kts` — Complete Reference

```groovy
// build.gradle (Groovy DSL) — complete annotated reference
// ─────────────────────────────────────────────────────────────────────────────────

// ── 1. PLUGINS BLOCK ──────────────────────────────────────────────────────────────
// Declares which Gradle plugins to apply. Resolved from plugins.gradle.org or
// local buildSrc. Must be the FIRST block (before repositories and dependencies).
plugins {
    id 'java'                                            // core: adds compile/test/jar tasks
    id 'application'                                     // core: adds run/installDist tasks
    id 'org.springframework.boot' version '3.3.0'        // community: adds bootJar/bootRun
    id 'io.spring.dependency-management' version '1.1.4' // community: BOM support
    id 'jacoco'                                          // core: coverage reports
    id 'checkstyle'                                      // core: code style
    id 'maven-publish'                                   // core: publishing to repos
}

// ── 2. GROUP, VERSION, DESCRIPTION ────────────────────────────────────────────────
// Project identity — these become part of the artifact name and published POM
group = 'com.example'                        // like Maven's groupId
version = '1.0.0-SNAPSHOT'                   // like Maven's version
description = 'My Spring Boot application'  // like Maven's description

// ── 3. JAVA EXTENSION BLOCK ───────────────────────────────────────────────────────
// Configures Java source/target compatibility (from the 'java' plugin)
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()    // attach javadoc JAR to publications
    withSourcesJar()    // attach sources JAR to publications
    toolchain {
        // Pin exact JDK version — Gradle downloads it if not present
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// ── 4. REPOSITORIES BLOCK ────────────────────────────────────────────────────────
// WHERE to download dependencies from.
// Gradle checks repositories in ORDER — first match wins.
repositories {
    mavenLocal()         // ~/.m2/repository first (for local artifacts)
    mavenCentral()       // Maven Central (most dependencies)
    gradlePluginPortal() // plugins.gradle.org (for Gradle plugins)

    // Company private repository
    maven {
        name = 'CompanyNexus'
        url = 'https://nexus.mycompany.com/repository/maven-public/'
        credentials {
            username = project.findProperty('nexusUsername') ?: System.getenv('NEXUS_USERNAME')
            password = project.findProperty('nexusPassword') ?: System.getenv('NEXUS_PASSWORD')
        }
    }

    // Spring Milestones / Snapshots (for pre-release Spring versions)
    maven { url = 'https://repo.spring.io/milestone' }
    maven { url = 'https://repo.spring.io/snapshot' }
}

// ── 5. CONFIGURATIONS BLOCK ───────────────────────────────────────────────────────
// Gradle configurations are named classpaths (like Maven scopes).
// The 'java' plugin adds: implementation, runtimeOnly, compileOnly, testImplementation, etc.
// You can define CUSTOM configurations:
configurations {
    // Custom configuration for integration test dependencies
    integrationTestImplementation {
        extendsFrom testImplementation    // includes all testImplementation deps
    }
    integrationTestRuntimeOnly {
        extendsFrom testRuntimeOnly
    }

    // Exclude a transitive dependency from ALL configurations globally
    all {
        exclude group: 'commons-logging', module: 'commons-logging'
        // Force a specific version of a dependency across all configurations
        resolutionStrategy {
            force 'com.fasterxml.jackson.core:jackson-databind:2.15.3'
            // Fail build on any version conflict (strict mode)
            // failOnVersionConflict()
            // Cache dynamic versions for 10 minutes (default: 24 hours)
            cacheDynamicVersionsFor 10, 'minutes'
        }
    }
}

// ── 6. DEPENDENCIES BLOCK ────────────────────────────────────────────────────────
// WHAT dependencies to add to each configuration (classpath).
dependencies {
    // Spring Boot starters — no versions when using dependency-management plugin
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // With explicit version (when not using BOM)
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'

    // Annotation processors (APT tools like Lombok, MapStruct)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'

    // Runtime-only deps (not needed at compile time)
    runtimeOnly 'com.mysql:mysql-connector-j'

    // Test dependencies
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.1'
    testRuntimeOnly 'com.h2database:h2'    // in-memory DB for tests

    // Exclude a transitive dep
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
    }
    implementation 'org.springframework.boot:spring-boot-starter-jetty'  // use Jetty instead

    // Project dependency (for multi-module builds)
    implementation project(':common-lib')
}

// ── 7. SOURCE SETS ────────────────────────────────────────────────────────────────
// SourceSets define sets of source files and their classpaths.
// The 'java' plugin adds: main and test source sets automatically.
// Add CUSTOM source sets (e.g., for integration tests):
sourceSets {
    // Custom: integration test source set
    integrationTest {
        java {
            srcDir 'src/integrationTest/java'
        }
        resources {
            srcDir 'src/integrationTest/resources'
        }
        // Inherit classpath from main
        compileClasspath += sourceSets.main.output + configurations.integrationTestImplementation
        runtimeClasspath += output + compileClasspath + configurations.integrationTestRuntimeOnly
    }

    // Modify the default main source set (rarely needed)
    main {
        java {
            srcDirs = ['src/main/java', 'src/generated/java']   // add extra source dir
        }
        resources {
            srcDirs = ['src/main/resources', 'src/main/config']
        }
    }
}

// Register a task for the integration test source set
tasks.register('integrationTest', Test) {
    description = 'Run integration tests'
    group = 'verification'
    testClassesDirs = sourceSets.integrationTest.output.classesDirs
    classpath = sourceSets.integrationTest.runtimeClasspath
    useJUnitPlatform()
    // Run integration tests after unit tests
    shouldRunAfter tasks.named('test')
}

// Wire into the 'check' lifecycle
tasks.named('check') {
    dependsOn tasks.named('integrationTest')
}

// ── 8. TASKS BLOCK ────────────────────────────────────────────────────────────────
// Configure existing tasks and register new custom ones.
tasks.named('compileJava') {
    options.encoding = 'UTF-8'
    options.compilerArgs += ['-parameters']
}

tasks.named('test') {
    useJUnitPlatform()
    maxParallelForks = 4
    testLogging {
        events 'passed', 'skipped', 'failed'
        exceptionFormat 'full'
    }
    finalizedBy tasks.named('jacocoTestReport')
}

tasks.named('bootJar') {
    archiveFileName = "${project.name}-${version}.jar"
    layered {
        enabled = true   // layered JAR for efficient Docker layer caching
    }
}

// Custom task
tasks.register('printVersion') {
    group = 'help'
    description = 'Print the project version'
    doLast {
        println "Project version: ${project.version}"
    }
}

// ── 9. PUBLISHING BLOCK ───────────────────────────────────────────────────────────
// Configures what to publish and where (from the 'maven-publish' plugin).
publishing {
    publications {
        maven(MavenPublication) {
            from components.java       // JAR + generated POM + javadoc + sources JARs
            groupId = project.group
            artifactId = project.name
            version = project.version
        }
    }
    repositories {
        maven {
            name = 'Nexus'
            url = version.endsWith('SNAPSHOT')
                ? 'https://nexus.mycompany.com/repository/maven-snapshots/'
                : 'https://nexus.mycompany.com/repository/maven-releases/'
            credentials {
                username = System.getenv('NEXUS_USERNAME')
                password = System.getenv('NEXUS_PASSWORD')
            }
        }
    }
}

// ── 10. WRAPPER BLOCK ─────────────────────────────────────────────────────────────
// Configures the Gradle Wrapper (equivalent of mvnw).
// After changing this, run: ./gradlew wrapper
wrapper {
    gradleVersion = '8.7'
    distributionType = Wrapper.DistributionType.BIN   // BIN = binary only (smaller)
    // ALL = includes Gradle source code (needed for IDE debugging of Gradle scripts)
    // distributionType = Wrapper.DistributionType.ALL
}
```

---

### `settings.gradle` / `settings.gradle.kts` — Complete Reference

`settings.gradle` is processed during the **Initialization phase** — before any `build.gradle` is evaluated. It controls which projects are part of the build.

```groovy
// settings.gradle (Groovy DSL) — complete annotated reference

// ── 1. PLUGIN MANAGEMENT ──────────────────────────────────────────────────────────
// Declare repositories where Gradle looks for BUILD SCRIPT plugins.
// Must come BEFORE 'plugins {}' block resolution.
pluginManagement {
    repositories {
        gradlePluginPortal()     // default: plugins.gradle.org
        mavenCentral()
        maven { url 'https://repo.spring.io/milestone' }
        maven { url 'https://nexus.mycompany.com/repository/gradle-plugins/' }
    }

    // Resolve plugin versions centrally so individual build.gradle files
    // don't need to repeat version numbers
    plugins {
        id 'org.springframework.boot' version "${springBootVersion}"
        id 'io.spring.dependency-management' version "${dependencyManagementVersion}"
    }
}

// ── 2. DEPENDENCY RESOLUTION MANAGEMENT ───────────────────────────────────────────
// Global dependency resolution settings applied to ALL projects in the build.
// Available since Gradle 6.8 — the recommended way to configure resolution globally.
dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS = projects cannot declare their own repositories
    // (all repos must be in settings.gradle — good for security control)
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    // PREFER_SETTINGS = settings repos take priority over project repos
    // repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenCentral()
        maven { url 'https://repo.spring.io/milestone' }
    }

    // Version catalog: centralise all dependency versions
    versionCatalogs {
        libs {
            // Define versions
            version('springBoot', '3.3.0')
            version('mapstruct', '1.5.5.Final')

            // Define libraries using versions
            library('spring-web', 'org.springframework.boot', 'spring-boot-starter-web')
                .versionRef('springBoot')
            library('mapstruct', 'org.mapstruct', 'mapstruct')
                .versionRef('mapstruct')
        }
    }
    // Usage in build.gradle: implementation libs.spring.web
}

// ── 3. ROOT PROJECT NAME ──────────────────────────────────────────────────────────
// The name used for the root project. Becomes the default artifact name.
// Must match the directory name or Jenkins/GitHub repo name (by convention).
rootProject.name = 'my-spring-app'

// ── 4. INCLUDE (SUBPROJECTS) ──────────────────────────────────────────────────────
// Declare all subprojects to include in this multi-project build.
// Each subproject MUST have its own build.gradle.
include 'common-lib'
include 'user-service'
include 'order-service'
include 'payment-service'
include 'api-gateway'

// You can organize subprojects in subdirectories
include 'services:email-service'
include 'services:sms-service'

// Override project directory (if directory name doesn't match project name)
project(':services:email-service').projectDir = new File(settingsDir, 'services/email')
project(':services:sms-service').projectDir = new File(settingsDir, 'services/sms')

// ── 5. INCLUDE BUILD (COMPOSITE BUILDS) ───────────────────────────────────────────
// Composite builds: include ANOTHER Gradle build as if it were a subproject.
// Useful for: developing a library and a consumer simultaneously without publishing.
includeBuild '../my-utils-library'   // substitutes published artifact with local source
```

```kotlin
// settings.gradle.kts (Kotlin DSL) — same structure
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "my-spring-app"

include("common-lib")
include("user-service")
include("order-service")
```

---

### How Spring Boot Uses Gradle and `build.gradle`

Spring Boot integrates with Gradle through two plugins:
1. `org.springframework.boot` — adds `bootJar`, `bootRun`, `bootWar` tasks
2. `io.spring.dependency-management` — adds BOM import support (`dependencyManagement {}`)

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Spring Boot + Gradle Integration:                                               │
│                                                                                  │
│  plugins {                                                                       │
│    id 'org.springframework.boot' version '3.3.0'                                │
│    id 'io.spring.dependency-management' version '1.1.4'                         │
│  }                                                                               │
│         │                                                                        │
│         ▼                                                                        │
│  spring-boot-dependencies BOM is imported automatically                          │
│  → ~300 dependency versions are pre-defined and pre-tested                      │
│  → spring-webmvc, tomcat, jackson, hibernate, junit, testcontainers, ...        │
│                                                                                  │
│  org.springframework.boot plugin adds:                                           │
│                                                                                  │
│  Task          Phase            What it does                                     │
│  ─────────────────────────────────────────────────────────────────────────────   │
│  bootJar       assemble         Creates executable fat JAR (BOOT-INF/lib/)      │
│  bootWar       assemble         Creates executable WAR with embedded Tomcat     │
│  bootRun       -                Runs app directly (no JAR needed)               │
│  bootBuildImage -               Builds a Docker image (using Cloud Native       │
│                                 Buildpacks, no Dockerfile needed)               │
└──────────────────────────────────────────────────────────────────────────────────┘
```

**A minimal but complete Spring Boot `build.gradle`:**

```groovy
// build.gradle (Groovy DSL) — standard Spring Boot project
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot starters — NO versions! Managed by BOM via dependency-management plugin
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Database
    runtimeOnly 'com.mysql:mysql-connector-j'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'com.h2database:h2'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

```kotlin
// build.gradle.kts (Kotlin DSL) — standard Spring Boot project
plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("com.mysql:mysql-connector-j")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
```

**`bootJar` vs `jar` in Spring Boot:**

```groovy
// Spring Boot plugin DISABLES the plain 'jar' task and ENABLES 'bootJar'
// If you need BOTH (e.g., for a library used as a dependency by other projects):
tasks.named('bootJar') {
    enabled = true                           // fat JAR with all deps
    archiveClassifier = 'boot'               // name: my-app-1.0.0-boot.jar
}

tasks.named('jar') {
    enabled = true                           // plain thin JAR
    archiveClassifier = ''                   // name: my-app-1.0.0.jar
}
// Common for library modules that are both runnable AND imported by other services
```

**`bootRun` configuration:**

```groovy
tasks.named('bootRun') {
    // Pass JVM arguments
    jvmArgs = ['-Xmx512m', '-Dspring.profiles.active=dev']

    // Pass program arguments
    args = ['--server.port=8081']

    // Enable LiveReload with devtools
    sourceResources sourceSets.main
}
```

**Build a Docker image without a Dockerfile:**

```bash
# Uses Cloud Native Buildpacks — no Dockerfile required!
./gradlew bootBuildImage
# → Creates image: docker.io/library/my-app:0.0.1-SNAPSHOT

# Customise the image name
./gradlew bootBuildImage --imageName=myregistry.io/myteam/my-app:1.0.0
```

```groovy
// Or configure in build.gradle
tasks.named('bootBuildImage') {
    imageName = "myregistry.io/myteam/${project.name}:${project.version}"
    publish = true
    docker {
        publishRegistry {
            username = System.getenv('REGISTRY_USERNAME')
            password = System.getenv('REGISTRY_PASSWORD')
        }
    }
}
```

**Maven vs Gradle comparison for Spring Boot:**

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│  Maven vs Gradle — Spring Boot Context:                                          │
│                                                                                  │
│  Feature              Maven                      Gradle                         │
│  ────────────────────────────────────────────────────────────────────────────    │
│  BOM import           <parent> inheritance        dependencyManagement { imports │
│                       or <dependencyManagement>   { mavenBom '...' } }          │
│  Fat JAR creation     spring-boot-maven-plugin    org.springframework.boot plugin│
│                       :repackage goal              bootJar task                  │
│  Run app              mvn spring-boot:run          ./gradlew bootRun            │
│  Build config file    pom.xml (XML, verbose)       build.gradle (Groovy/Kotlin) │
│  Build speed          Moderate (no daemon)         Fast (daemon + incremental)  │
│  Android              Not supported               Official Android build tool    │
│  Learning curve       Moderate (XML)              Steeper (requires DSL knowledge│
│  IDE support          Excellent                   Excellent (IntelliJ, VS Code) │
│  Spring Initializr    Both supported              Both supported                 │
└──────────────────────────────────────────────────────────────────────────────────┘
```

