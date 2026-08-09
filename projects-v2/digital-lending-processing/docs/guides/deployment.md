# Deployment Guide

## Docker Compose (Local Development)

### Quick Start

```bash
# Start everything
docker compose up -d

# View logs
docker compose logs -f

# Stop everything
docker compose down

# Stop and remove volumes (destructive)
docker compose down -v
```

### Service Dependencies

```
mongodb ← config-server, authentication, customer, offer, application, processing, notification, document
redis ← api-gateway, authentication, offer
discovery-server ← all services
config-server ← all services (optional)
```

### Port Mappings

| Service | Host Port | Container Port |
|---|---|---|
| API Gateway | 8080 | 8080 |
| Authentication | 8081 | 8081 |
| Customer Profile | 8082 | 8082 |
| Loan Offer | 8083 | 8083 |
| EMI Calculation | 8084 | 8084 |
| Loan Application | 8085 | 8085 |
| Loan Processing | 8086 | 8086 |
| Notification | 8087 | 8087 |
| Document | 8088 | 8088 |
| MongoDB | 27017 | 27017 |
| Mongo Express | 8089 | 8081 |
| Redis | 6379 | 6379 |
| Redis Insight | 5540 | 5540 |
| Eureka | 8761 | 8761 |
| Config Server | 8888 | 8888 |
| Mailpit Web | 8025 | 8025 |
| Mailpit SMTP | 1025 | 1025 |
| Customer Portal (React) | 5173 | 5173 |
| Admin Console (React) | 5174 | 5174 |

### Health Checks

All services implement health endpoints:

```bash
# Check specific service
curl http://localhost:8081/actuator/health

# Check Eureka registration
curl http://localhost:8761/eureka/apps
```

## Docker Images

### Building Individual Service

```bash
# Build a single service image
docker build -f authentication-service/Dockerfile -t loansphere/auth:latest .

# Build from module
mvn clean package -pl authentication-service -am -DskipTests
```

### Dockerfile Structure

Each service uses a multi-stage build:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
COPY <service>/pom.xml <service>/
RUN mvn dependency:go-offline -pl <service> -am -B
COPY . .
RUN mvn clean package -pl <service> -am -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser
WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /app/<service>/target/*.jar app.jar
USER appuser
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:<port>/actuator/health || exit 1
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Security Best Practices

- **Non-root user**: All containers run as `appuser:1001`
- **Specific tags**: Base images use exact versions (not `latest`)
- **Minimal runtime**: `jre-alpine` for smallest attack surface
- **Health checks**: All services have Docker HEALTHCHECK
- **No secrets in images**: All secrets passed via environment variables

## Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (minikube, kind, or cloud)
- Helm 3+
- kubectl configured
- NGINX Ingress Controller installed

### Helm Installation

```bash
# Install the umbrella chart
helm install loansphere ./charts

# Check status
helm status loansphere

# List all resources
kubectl get all -l app.kubernetes.io/instance=loansphere
```

### Chart Structure

```
charts/
├── Chart.yaml           # Umbrella chart metadata
├── values.yaml          # Default configuration
└── (per-service subcharts)
```

### Per-Service Helm Chart

Each service contains:
```
<service>/helm/
├── Chart.yaml
├── values.yaml
├── NOTES.txt
└── templates/
    ├── deployment.yaml      # Rolling updates, resource limits
    ├── service.yaml         # ClusterIP service
    ├── configmap.yaml       # Non-sensitive config
    ├── secret.yaml          # Sensitive data (passwords, JWT keys)
    ├── ingress.yaml         # External access routing
    └── serviceaccount.yaml  # Service identity
```

### Kubernetes Manifests (Key Patterns)

**Deployment**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Release.Name }}-auth
spec:
  replicas: {{ .Values.replicaCount }}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: auth
  template:
    spec:
      containers:
      - name: auth
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        ports:
        - containerPort: 8081
        resources:
          requests:
            cpu: 100m
            memory: 256Mi
          limits:
            cpu: 500m
            memory: 512Mi
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
```

**Service**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ .Release.Name }}-auth
spec:
  type: ClusterIP
  ports:
  - port: 8081
    targetPort: 8081
  selector:
    app: auth
```

### NGINX Ingress

Single ingress routes all traffic:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: loansphere-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: loansphere.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ui-loan-portal
            port:
              number: 80
      - path: /admin
        pathType: Prefix
        backend:
          service:
            name: ui-admin-console
            port:
              number: 80
      - path: /api/v1/auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 8081
      # ... other services
```

## Environment Variables (Production)

For production deployment, override these variables:

| Variable | Dev Default | Production |
|---|---|---|
| `JWT_SECRET` | Static string | Random 256+ bit key |
| `MONGODB_PASSWORD` | `rootroot` | Strong password |
| `REDIS_PASSWORD` | `rootroot` | Strong password |
| `EUREKA_SERVER_URL` | `localhost:8761` | Internal service URL |
| `MAIL_HOST` | `mailpit` | Production SMTP server |
| `MAIL_PORT` | `1025` | `587` (TLS) |

### Using Kubernetes Secrets

```bash
# Create secret
kubectl create secret generic loansphere-secrets \
  --from-literal=jwt-secret=$(openssl rand -base64 64) \
  --from-literal=mongodb-password=<strong-password> \
  --from-literal=redis-password=<strong-password>

# Reference in values.yaml
jwt:
  secretFrom:
    secretKeyRef:
      name: loansphere-secrets
      key: jwt-secret
```

## Monitoring

### Health Check Endpoints

Each service exposes:
- `/actuator/health` — Liveness and readiness
- `/actuator/info` — Build and git info
- `/actuator/metrics` — JVM and application metrics

### Eureka Dashboard

Monitor service health at [http://eureka-host:8761](http://eureka-host:8761) — shows registered instances, status, and metadata.

### Logging

All services log to stdout (Docker/K8s compatible):
```bash
# Docker
docker compose logs -f <service>

# Kubernetes
kubectl logs -f deployment/<service>
```
