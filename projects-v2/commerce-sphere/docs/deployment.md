# Deployment Guide

## Docker Compose (Local Development)

### Quick Start

```bash
docker compose up -d --build
```

This builds and starts all services:

- MongoDB 8 with Mongo Express (http://localhost:8081)
- Redis 7 with Redis Insight (http://localhost:8001)
- Identity Service (http://localhost:8082)
- Catalog Service (http://localhost:8083)
- Cart Service (http://localhost:8084)
- Inventory Service (http://localhost:8085)
- Order Service (http://localhost:8086)
- Admin Console (http://localhost:3000)
- Storefront (http://localhost:3001)

### Start Individual Services

```bash
# Infrastructure only
docker compose up -d mongodb redis mongo-express redis-insight

# Specific services
docker compose up -d identity-service catalog-service
```

### View Logs

```bash
docker compose logs -f identity-service
docker compose logs -f --tail=100
```

### Rebuild After Changes

```bash
docker compose up -d --build identity-service
docker compose up -d --build ui-admin-console
```

### Stop Everything

```bash
docker compose down
docker compose down -v  # Also remove volumes (destroys data)
```

---

## Kubernetes with Helm

### Prerequisites

- Kubernetes cluster (minikube, kind, or cloud provider)
- Helm 3+
- kubectl configured

### Deploy Infrastructure

First deploy MongoDB and Redis using community Helm charts:

```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install mongodb bitnami/mongodb --set auth.rootPassword=rootroot
helm install redis bitnami/redis --set auth.password=rootroot
```

### Deploy Services

Each service has its own Helm chart:

```bash
# Identity Service
cd backend/identity-service/helm
helm install identity-service . \
  --set image.repository=commercesphere/identity-service \
  --set image.tag=latest

# Catalog Service
cd backend/catalog-service/helm
helm install catalog-service . \
  --set image.repository=commercesphere/catalog-service

# Cart Service
cd backend/cart-service/helm
helm install cart-service .

# Inventory Service
cd backend/inventory-service/helm
helm install inventory-service .

# Order Service
cd backend/order-service/helm
helm install order-service .
```

### Verify Deployment

```bash
kubectl get pods
kubectl get services
kubectl get ingress
```

### Resource Requirements

Per service (defaults in Helm values):

| Resource | Request | Limit |
|----------|---------|-------|
| CPU | 250m | 500m |
| Memory | 512Mi | 1Gi |

---

## NGINX Ingress

An NGINX Ingress Controller routes traffic to the appropriate services:

```
/           → Storefront
/admin      → Admin Console
api/auth    → Identity Service
api/catalog → Catalog Service
api/cart    → Cart Service
api/orders  → Order Service
api/inventory → Inventory Service
```

### Install Ingress Controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.0/deploy/static/provider/cloud/deploy.yaml
```

---

## Docker Image Building

### Backend Services

Multi-stage Dockerfiles produce optimized images (~200MB):

```bash
cd backend/identity-service
docker build -t commercesphere/identity-service:latest .
```

### Frontend Apps

Build produces static files served by nginx (~30MB):

```bash
cd ui/admin-console
docker build -t commercesphere/admin-console:latest .
```
