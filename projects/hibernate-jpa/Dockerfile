# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw .
COPY .mvn ./.mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the final image
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/target/jpa-helper*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]