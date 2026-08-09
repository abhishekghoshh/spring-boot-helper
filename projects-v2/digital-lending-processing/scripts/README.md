# LoanSphere Utility Scripts

## Available Scripts

- `build-all.sh` — Build all backend services
- `test-all.sh` — Run all tests
- `docker-build.sh` — Build all Docker images
- `seed-data.sh` — Seed initial data (admin user, sample offers)

## Local Development

```bash
# Start infrastructure only
docker compose up -d mongodb mongo-express redis redis-insight mailpit

# Build and start all services
docker compose up --build -d

# Stop everything
docker compose down -v
```
