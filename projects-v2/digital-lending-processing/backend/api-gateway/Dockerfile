FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml settings.xml ./
RUN mvn -s settings.xml dependency:go-offline -B
COPY src ./src
RUN mvn -s settings.xml package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache wget && addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseZGC -Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=docker
USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.eureka.client.service-url.defaultZone=http://discovery-server:8761/eureka/ -jar app.jar"]
