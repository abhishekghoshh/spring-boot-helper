FROM eclipse-temurin:25-jre-alpine
RUN addgroup -g 1001 -S appgroup && adduser -S appuser -u 1001 -G appgroup
WORKDIR /app
COPY target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8089
HEALTHCHECK --interval=30s --timeout=3s --retries=5 --start-period=60s CMD wget -qO- http://localhost:8089/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
