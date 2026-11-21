# Spring apps CI/CD integration

> **Primer:** A CI/CD pipeline turns every push into a verified, deployable
> artifact: compile → unit test → integration test → security scan → Docker
> image (tagged by commit SHA) → staged deploy → production. GitHub Actions is
> the most common runner for Spring Boot projects; the same shape ports to
> GitLab CI / Jenkins.

## Real-life use case

Team of 8 shipping a payment service: `main` is always deployable. A PR runs
tests + Sonar; merge builds `payment:abc1234`, deploys to staging, runs
smoke tests, then promotes the *same image* to prod — no rebuild between
environments.

## Mental model

```text
git push → [build: mvn -B verify] → [test: unit + Testcontainers ITs]
    → [scan: Trivy/Sonar] → [image: docker build -t app:$SHA → push registry]
    → [deploy staging] → [smoke: /actuator/health + 1 API call]
    → [promote SAME image to prod] → [rollback: re-point to previous SHA]
```

## Minimal example

```yaml
# .github/workflows/ci.yml
name: ci
on:
  push:
    branches: [main]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: 'maven' }
      - run: mvn -B verify
      - uses: docker/build-push-action@v5
        with:
          push: true
          tags: ghcr.io/org/app:${{ github.sha }}
```

```dockerfile
# Multi-stage: build once, ship a slim JRE image
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
COPY --from=build /app/target/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Pitfalls

- Never bake secrets into the image — inject via env vars / vault at deploy time.
- Cache Maven/Gradle deps between runs or every build re-downloads the world.
- Tag images by commit SHA, not just `latest`, or rollbacks become guesswork.

## Interview Q&A

**Q1. Where do Testcontainers integration tests run in the pipeline?**
A: In the `verify` step on the CI runner (Docker must be available). They spin
up real Postgres/Kafka containers, so the same tests that pass locally gate
the merge — no mocks hiding integration bugs.

**Q2. How do you roll back a bad deploy?**
A: Keep the previous image + DB migrations backward-compatible (expand then
contract). Rollback re-points the deployment to the previous SHA — a 1-minute
operation, not a rebuild.

---


## Youtube


### Github Actions

- [SpringBoot - Build CI/CD Pipeline Using GitHub Actions | Build & Push Docker Image | JavaTechie](https://www.youtube.com/watch?v=NppkHKvnrqc)

