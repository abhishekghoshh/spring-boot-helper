# Stage 1: Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy gradle executable and configuration files first to leverage Docker cache
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Download dependencies (this layer will be cached unless dependencies change)
RUN ./gradlew dependencies --no-daemon

# Copy source code and build the application
COPY src src
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime stage
# Using Distroless for a tiny, secure footprint (~50MB + your app)
FROM mcr.microsoft.com/openjdk/jdk:25-distroless
WORKDIR /app

# Copy the built jar from the build stage
# Assuming your jar is named based on project version, we use a wildcard or specific name
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]