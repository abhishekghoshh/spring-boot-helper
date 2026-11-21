# Spring boot with Kubernetes

> **Primer:** Kubernetes runs your Spring Boot jar as a container in a Pod,
> keeps N replicas alive, and routes traffic via a Service. Config comes from
> ConfigMaps/Secrets (mounted as env vars or files), health from Actuator
> probes (`/actuator/health/liveness`, `/readiness`), and manifests can be
> generated from code with Dekorate instead of hand-written YAML.

## Real-life use case

Checkout service at 10x Diwali traffic: HPA scales 3 → 30 pods on CPU/RPS;
a bad deploy fails the readiness probe so the rollout auto-rolls back —
zero-downtime without anyone SSHing into a box.

## Mental model

```text
  Image (Jib/Buildpacks) → Deployment (replicas: 3, rollingUpdate)
        ├─ env: ConfigMap (app.yml) + Secret (db password)
        ├─ livenessProbe: GET /actuator/health/liveness   → restart if dead
        ├─ readinessProbe: GET /actuator/health/readiness → remove from Service if not ready
        └─ Service (ClusterIP) → Ingress → internet
  HPA watches CPU/custom RPS → scales replicas 3..30
```

## Minimal example

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: checkout }
spec:
  replicas: 3
  selector: { matchLabels: { app: checkout } }
  template:
    metadata: { labels: { app: checkout } }
    spec:
      containers:
        - name: app
          image: ghcr.io/org/checkout:abc1234
          ports: [{ containerPort: 8080 }]
          envFrom:
            - configMapRef: { name: checkout-config }
            - secretRef: { name: checkout-secrets }
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            initialDelaySeconds: 60
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
```

```yaml
# application-k8s.yaml (values come from the ConfigMap/Secret above)
management:
  endpoint:
    health:
      probes:
        enabled: true
```

## Pitfalls

- Default Actuator health is too shallow for readiness — add `readinessState` contributors (DB, Kafka reachable) or the pod serves traffic before it can.
- `latest` image tags + `imagePullPolicy: IfNotPresent` = nodes running stale code; pin SHAs.
- JVM must respect container limits (`-XX:MaxRAMPercentage=75.0`) or it OOMs against the cgroup.

## Interview Q&A

**Q1. Liveness vs readiness — why do you need both?**
A: Liveness ("am I deadlocked?") triggers a container restart. Readiness ("can
I serve traffic yet?") only removes the pod from the Service endpoints. A slow-
starting app with only liveness gets killed in a restart loop; with readiness
it warms up safely.

**Q2. Dekorate vs Skaffold vs hand-written YAML — what is each for?**
A: Dekorate generates K8s manifests from Java annotations at compile time
(no YAML drift). Skaffold is a dev loop (code change → rebuild → redeploy
automatically). Hand-written YAML (or Helm/Kustomize) is the production source
of truth for GitOps.

---





## Youtube

- [Daily Code Buffer](https://www.youtube.com/@DailyCodeBuffer)
    - [Kubernetes Tutorial with Microservices and Cloud | Kubernetes Course](https://www.youtube.com/watch?v=EY3uDfBG0OY)
      	- [shabbirdwd53/spring-boot-microservices/tree/kubernetes_as_service_registry](https://github.com/shabbirdwd53/spring-boot-microservices/tree/kubernetes_as_service_registry)
      	- [shabbirdwd53/spring-boot-microservices/tree/kubernetes](https://github.com/shabbirdwd53/spring-boot-microservices/tree/kubernetes)
    - [Deploy Springboot Microservices to Kubernetes Cluster | Full Example](https://www.youtube.com/watch?v=VAmntTPebKE)
    - [Dekorate: Generating Kubernetes Manifests in Spring Boot](https://www.youtube.com/watch?v=G_BhoEGFWWg)
    - [Skaffold Tutorial - Easy Kubernetes Development workflow with Spring Boot](https://www.youtube.com/watch?v=G_BhoEGFWWg)


- [SpringDeveloper](https://www.youtube.com/@SpringSourceDev/playlists)
  - [Getting Started with Spring Cloud Kubernetes](https://www.youtube.com/watch?v=u64jexEX_RY)
  - [Spring Boot Loves K8s](https://www.youtube.com/watch?v=nPACI6-J9Jc)
  - [Bulletproof Microservices with Spring and Kubernetes](https://www.youtube.com/watch?v=aI19ZYV4-7o)
  - [Best Practices to Spring to Kubernetes Easier and Faster](https://www.youtube.com/watch?v=YTPUNesUIbI)
  - [Pack to the Future: Cloud-Native Buildpacks on k8s](https://www.youtube.com/watch?v=J2SXkmOo8iQ)
  - [Deploying Spring Boot Apps on Kubernetes - Thomas Risberg](https://www.youtube.com/watch?v=RelPurLZnII)
  - [Spring Tips: Spring and Kubernetes, Redux (2022)](https://www.youtube.com/watch?v=Xe7K1biKcs0)
  - [Next-Generation Cloud Native Apps with Spring Cloud and Kubernetes](https://www.youtube.com/watch?v=UDg7y9TheI4)
  - [Spring Tips: Kubernetes Native Java](https://www.youtube.com/watch?v=6XIakve0GjI)
  - [Spring Office Hours: Episode 15 - Spring to Production](https://www.youtube.com/watch?v=zE2GW_CEJy4)
  - [Spring Boot on Amazon Web Services with Spring Cloud AWS](https://www.youtube.com/watch?v=cgMjPBmBkyE)



