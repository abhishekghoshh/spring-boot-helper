FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache wget && addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser
WORKDIR /app
COPY target/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseZGC -Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=docker
ENV SPRING_DATA_MONGODB_URI=mongodb://root:rootroot@mongodb:27017/application_db?authSource=admin
USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
