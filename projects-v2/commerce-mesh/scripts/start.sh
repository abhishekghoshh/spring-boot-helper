#!/bin/bash
set -e

echo "=== CommerceMesh Platform ==="
echo "Starting infrastructure services..."

# Start databases and message broker
docker compose up -d postgres mongodb redis rabbitmq

echo "Waiting for databases to be healthy..."
sleep 10

# Start Spring Cloud infrastructure
docker compose up -d discovery-server
echo "Waiting for Eureka..."
sleep 15

docker compose up -d config-server
echo "Waiting for Config Server..."
sleep 10

docker compose up -d api-gateway
echo "Waiting for API Gateway..."
sleep 10

# Start all microservices
docker compose up -d \
  authentication-service \
  user-service \
  product-catalog-service \
  cart-service \
  inventory-service \
  order-service \
  payment-service \
  notification-service \
  search-service

# Start monitoring
docker compose up -d prometheus grafana loki promtail jaeger

# Start frontend
docker compose up -d ui-admin-console ui-customer-console

echo ""
echo "=== CommerceMesh is starting up ==="
echo "Eureka:          http://localhost:8761"
echo "API Gateway:     http://localhost:8080"
echo "pgAdmin:         http://localhost:5050"
echo "Mongo Express:   http://localhost:8081"
echo "Redis Insight:   http://localhost:8001"
echo "RabbitMQ:        http://localhost:15672"
echo "Prometheus:      http://localhost:9090"
echo "Grafana:         http://localhost:3001 (admin/admin)"
echo "Jaeger:          http://localhost:16686"
echo "Admin Portal:    http://localhost:3000 (admin/Admin@123)"
echo "Customer Store:  http://localhost:3002"
echo ""
