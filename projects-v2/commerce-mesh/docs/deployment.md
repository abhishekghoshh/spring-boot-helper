# Deployment Guide

## Overview

CommerceMesh supports two deployment modes: **Docker Compose** for local development and testing, and **Kubernetes with Helm** for staging and production. Both use the same multi-stage Dockerfiles, ensuring consistency between environments.

---

## Docker Compose (Local & CI)

### Starting All Services

```bash
docker compose up -d
```

This starts 25 containers including all databases, message broker, Spring Cloud infrastructure, 9 microservices, frontend, and the full observability stack.

### Selective Starting

```bash
# Just databases
docker compose up -d postgres mongodb redis

# Just infrastructure
docker compose up -d postgres mongodb redis rabbitmq discovery-server config-server api-gateway

# Just one service (rebuild on code changes)
docker compose up -d --build product-catalog-service
```

### Building Individual Services

```bash
docker compose build authentication-service
docker compose build --no-cache order-service   # Full rebuild
```

### Viewing Logs

```bash
docker compose logs -f                              # All services
docker compose logs -f api-gateway                  # Specific service
docker compose logs --tail=100 order-service        # Last 100 lines
```

### Stopping

```bash
docker compose down           # Stop containers, keep volumes
docker compose down -v        # Stop containers AND delete volumes (DESTRUCTIVE)
```

### Service Dependencies

Docker Compose manages the startup order via `depends_on` with health checks:

```
postgres ──(healthy)──▶ authentication-service, user-service, inventory-service, order-service, payment-service, notification-service
mongodb  ──(healthy)──▶ product-catalog-service, cart-service, search-service
redis    ──(healthy)──▶ authentication-service, cart-service
rabbitmq ──(healthy)──▶ order-service, notification-service
discovery-server ──(healthy)──▶ config-server, api-gateway, all microservices
config-server  ──(healthy)──▶ api-gateway
```

---

## Kubernetes (Production)

### Prerequisites

| Component | Version | Purpose |
|-----------|---------|---------|
| Kubernetes | 1.30+ | Container orchestration |
| Helm | 3.16+ | Package management |
| NGINX Ingress Controller | 1.11+ | External traffic routing |
| cert-manager | 1.15+ | TLS certificate automation (optional) |
| External DNS | (optional) | Automatic DNS record creation |

### Namespace Setup

```bash
kubectl create namespace commercemesh
kubectl config set-context --current --namespace=commercemesh
```

### Secrets Management

Create Kubernetes secrets for sensitive configuration:

```bash
kubectl create secret generic commercemesh-secrets \
  --from-literal=jwt-secret=$(openssl rand -base64 32) \
  --from-literal=postgres-password=$(openssl rand -base64 16) \
  --from-literal=mongo-password=$(openssl rand -base64 16) \
  --from-literal=redis-password=$(openssl rand -base64 16) \
  --from-literal=rabbitmq-password=$(openssl rand -base64 16) \
  -n commercemesh
```

### Deployment

```bash
# Install from root umbrella chart
helm install commerce-mesh charts/commerce-mesh \
  -n commercemesh \
  --set ingress.host=commercemesh.example.com \
  --values values-prod.yaml

# Upgrade
helm upgrade commerce-mesh charts/commerce-mesh \
  -n commercemesh \
  --values values-prod.yaml

# Rollback
helm rollback commerce-mesh -n commercemesh

# Uninstall
helm uninstall commerce-mesh -n commercemesh
```

### Environment-Specific Values

```yaml
# values-prod.yaml
global:
  imageRegistry: registry.example.com/commercemesh

ingress:
  enabled: true
  className: nginx
  host: commercemesh.example.com
  tls:
    enabled: true
    secretName: commercemesh-tls

services:
  product-catalog-service:
    replicaCount: 3
    resources:
      requests:
        memory: "512Mi"
        cpu: "250m"
      limits:
        memory: "1Gi"
        cpu: "1000m"
    autoscaling:
      enabled: true
      minReplicas: 3
      maxReplicas: 10

  order-service:
    replicaCount: 3
    autoscaling:
      enabled: true
      minReplicas: 2
      maxReplicas: 10

  authentication-service:
    replicaCount: 2
```

---

## Kubernetes Resource Configuration

### Pod Security

Every deployment enforces:

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1001
  runAsGroup: 1001
  fsGroup: 1001

containers:
  - securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
          - ALL
```

### Health Probes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 5

startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  failureThreshold: 30
  periodSeconds: 5
```

### Resource Requests & Limits

| Service | Request Memory | Limit Memory | Request CPU | Limit CPU |
|---------|---------------|-------------|------------|-----------|
| discovery-server | 256Mi | 512Mi | 100m | 500m |
| config-server | 256Mi | 512Mi | 100m | 500m |
| api-gateway | 256Mi | 512Mi | 200m | 1000m |
| authentication-service | 256Mi | 512Mi | 200m | 1000m |
| user-service | 256Mi | 512Mi | 100m | 500m |
| product-catalog-service | 512Mi | 1Gi | 250m | 1000m |
| cart-service | 256Mi | 512Mi | 200m | 500m |
| inventory-service | 256Mi | 512Mi | 100m | 500m |
| order-service | 512Mi | 1Gi | 250m | 1000m |
| payment-service | 256Mi | 512Mi | 200m | 1000m |
| notification-service | 256Mi | 512Mi | 100m | 500m |
| search-service | 256Mi | 512Mi | 200m | 1000m |

### Horizontal Pod Autoscaling (HPA)

```yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
  targetMemoryUtilizationPercentage: 80
```

Scaling thresholds:
- **product-catalog-service:** scale up aggressively (heavy read traffic)
- **order-service:** scale up moderately
- **authentication-service, user-service:** scale up conservatively

### Pod Disruption Budget

```yaml
podDisruptionBudget:
  enabled: true
  minAvailable: 1     # At least 1 pod available during voluntary disruptions
```

### Pod Anti-Affinity

Pods of the same service are spread across nodes:

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchLabels:
              app: <service-name>
          topologyKey: kubernetes.io/hostname
```

### Network Policies

Each service only allows ingress from the API Gateway by default:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: <service-name>
spec:
  podSelector:
    matchLabels:
      app: <service-name>
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
      ports:
        - protocol: TCP
          port: 8080
```

---

## NGINX Ingress Configuration

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: commerce-mesh-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - commercemesh.example.com
      secretName: commercemesh-tls
  rules:
    - host: commercemesh.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ui-admin-console
                port:
                  number: 80
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: api-gateway
                port:
                  number: 8080
```

---

## Database Deployment (Production)

### PostgreSQL

Use a managed service (AWS RDS, GCP Cloud SQL, Azure Database for PostgreSQL) or deploy with the Bitnami Helm chart:

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install commercemesh-postgres bitnami/postgresql \
  --set auth.password=<password> \
  --set primary.persistence.size=50Gi \
  --set primary.resources.requests.memory=2Gi \
  --set primary.resources.requests.cpu=1000m
```

### MongoDB

Use MongoDB Atlas or deploy with the Bitnami Helm chart:

```bash
helm install commercemesh-mongodb bitnami/mongodb \
  --set auth.rootPassword=<password> \
  --set persistence.size=50Gi
```

### Redis

Use AWS ElastiCache, GCP Memorystore, or deploy with Bitnami:

```bash
helm install commercemesh-redis bitnami/redis \
  --set auth.password=<password> \
  --set master.persistence.size=20Gi
```

### RabbitMQ

```bash
helm install commercemesh-rabbitmq bitnami/rabbitmq \
  --set auth.username=admin \
  --set auth.password=<password> \
  --set persistence.size=20Gi \
  --set plugins="rabbitmq_management rabbitmq_prometheus"
```

---

## CI/CD Pipeline

### GitHub Actions Workflow

```yaml
name: Build & Deploy

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: 25
          distribution: temurin
          cache: maven

      - name: Build & Test
        run: | 
          for service in backend/*/; do
            cd "$service" && mvn clean verify && cd -
          done

      - name: Build Docker images
        run: |
          for svc in backend/*/; do
            name=$(basename $svc)
            docker build -t registry.example.com/commercemesh/$name:${GITHUB_SHA} $svc
          done

      - name: Push images
        run: |
          for svc in backend/*/; do
            name=$(basename $svc)
            docker push registry.example.com/commercemesh/$name:${GITHUB_SHA}
          done

      - name: Deploy to staging
        run: |
          helm upgrade commerce-mesh charts/commerce-mesh \
            --set global.image.tag=${GITHUB_SHA} \
            -n commercemesh-staging
```

---

## Rollout Strategy

### Rolling Updates (Default)

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0      # Never have fewer pods than desired
    maxSurge: 1            # Allow 1 extra pod during rollout
```

### Blue-Green Deployments (Optional)

For zero-downtime critical services (payment, order):

1. Deploy new version to a separate deployment (`order-service-green`)
2. Run smoke tests against green
3. Switch service selector to green
4. Scale down blue after confirming stability

### Canary Releases

Using service mesh (Istio/Linkerd) for traffic splitting:
- 5% → canary for 10 minutes
- 25% → canary for 30 minutes
- 100% → canary (full rollout)

---

## Rollback Procedures

### Helm Rollback

```bash
# List Helm history
helm history commerce-mesh -n commercemesh

# Rollback to previous revision
helm rollback commerce-mesh -n commercemesh

# Rollback to specific revision
helm rollback commerce-mesh 5 -n commercemesh
```

### Database Rollback

Flyway migrations are one-way by default. For rollbacks:
1. Create a new migration that reverses the change (`V5__revert_add_column.sql`)
2. Never modify applied migrations
3. Test rollback migrations in staging before production

---

## Backup & Disaster Recovery

### PostgreSQL

```bash
# Backup
pg_dump -h <host> -U postgres -d commercemesh -Fc > backup_$(date +%Y%m%d).dump

# Restore
pg_restore -h <host> -U postgres -d commercemesh backup_20260725.dump
```

### MongoDB

```bash
# Backup
mongodump --uri="mongodb://user:pass@host:27017/commercemesh" --out=/backup/$(date +%Y%m%d)

# Restore
mongorestore --uri="mongodb://user:pass@host:27017/commercemesh" /backup/20260725/
```

### Redis

```bash
# Backup (Redis generates RDB snapshots automatically)
# Copy the dump file
cp /data/dump.rdb /backup/redis_$(date +%Y%m%d).rdb
```

### Schedule Automated Backups

Use Kubernetes CronJobs:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
spec:
  schedule: "0 2 * * *"     # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: backup
              image: postgres:17-alpine
              command: ["pg_dump", "-h", "postgres", "-U", "postgres", "-d", "commercemesh", "-Fc", "-f", "/backup/backup.dump"]
          restartPolicy: OnFailure
```

---

## Monitoring Deployments

### Prometheus AlertManager Rules

```yaml
groups:
  - name: deployment_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        annotations:
          summary: "{{ $labels.service }} error rate is {{ $value }}"

      - alert: PodRestarting
        expr: rate(kube_pod_container_status_restarts_total[15m]) > 0
        annotations:
          summary: "Pod {{ $labels.pod }} is restarting"

      - alert: HPAAtCapacity
        expr: kube_hpa_status_current_replicas == kube_hpa_spec_max_replicas
        annotations:
          summary: "HPA {{ $labels.hpa }} at max capacity"
```
