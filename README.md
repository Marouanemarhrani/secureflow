# SecureFlow

[![security-pipeline](https://github.com/Marouanemarhrani/secureflow/actions/workflows/security-pipeline.yml/badge.svg)](https://github.com/Marouanemarhrani/secureflow/actions/workflows/security-pipeline.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F)](https://spring.io/projects/spring-boot)

A production-grade **DevSecOps platform** built from scratch — 9-gate CI/CD security pipeline, zero-trust Kubernetes cluster, GitOps delivery, and full observability. Every layer is real, enforced, and running.

## What this project demonstrates

This isn't a tutorial follow-along. Every component was configured, debugged, and integrated by hand on a local Kubernetes cluster (kind on Ubuntu ARM64). The pipeline doesn't just *report* — it **enforces**. Branch protection requires all 9 gates to pass before any code reaches `main`.

**Key proof points:**
- Broken unit test → PR merge blocked → fix pushed → merge allowed ([PR #4](https://github.com/Marouanemarhrani/secureflow/pull/4))
- PostgreSQL data survives pod kills via StatefulSet + PVC persistence
- Default-deny NetworkPolicy blocks all traffic unless explicitly allowed
- Kyverno rejects any pod that runs as root, uses hostPath, or lacks resource limits

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        GitHub Actions                           │
│  ┌─────────┬─────────┬─────────┬─────────┬─────────┬─────────┐ │
│  │ Gate 0  │ Gate 1  │ Gate 2  │ Gate 3  │ Gate 4  │ Gate 5  │ │
│  │  Tests  │Gitleaks │ Semgrep │Trivy FS │ Checkov │Trivy Img│ │
│  └─────────┴─────────┴─────────┴─────────┴─────────┴─────────┘ │
│  ┌─────────┬─────────┬─────────┐                                │
│  │ Gate 6  │ Gate 7  │ Gate 8  │                                │
│  │OWASP ZAP│Checkov  │ Kyverno │                                │
│  │ (DAST)  │  (K8s)  │  (CLI)  │                                │
│  └─────────┴─────────┴─────────┘                                │
│         All 9 gates must pass │ Branch protection enforced      │
└────────────────────────────────┼────────────────────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Argo CD (GitOps)                            │
│              Auto-syncs main → cluster state                    │
└────────────────────────────────┼────────────────────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                   kind Kubernetes Cluster                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                  secureflow namespace                     │   │
│  │                                                          │   │
│  │  ┌─────────────────────┐    ┌──────────────────────┐    │   │
│  │  │   notes-api (x3)    │───▶│  PostgreSQL (x1)     │    │   │
│  │  │   Deployment        │    │  StatefulSet + PVC   │    │   │
│  │  │   non-root UID 10001│    │  UID 999, 1Gi volume │    │   │
│  │  └─────────────────────┘    └──────────────────────┘    │   │
│  │           │                                              │   │
│  │  ┌────────┴───────────────────────────────────────────┐  │   │
│  │  │              NetworkPolicies (5 rules)             │  │   │
│  │  │  default-deny → allow-dns → allow-ingress-nginx   │  │   │
│  │  │  → allow-prometheus → allow-notes-api↔postgres    │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  │                                                          │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │           Kyverno ClusterPolicies (5)              │  │   │
│  │  │  require-run-as-nonroot │ disallow-privileged      │  │   │
│  │  │  require-resource-limits │ disallow-host-path      │  │   │
│  │  │  require-read-only-root-fs                         │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                 monitoring namespace                      │   │
│  │     Prometheus ──▶ Grafana (7-panel dashboard)           │   │
│  │     ServiceMonitor scrapes /actuator/prometheus           │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## The 9 security gates

| # | Gate | Tool | What it catches |
|---|------|------|-----------------|
| 0 | Unit Tests | Maven/JUnit 5 | Logic errors, regressions, broken contracts |
| 1 | Secrets | Gitleaks | Hardcoded API keys, tokens, passwords in git history |
| 2 | SAST | Semgrep | Code-level vulnerabilities — SQLi, XSS, command injection |
| 3 | Dependencies | Trivy (fs) | Known CVEs in Java libraries (pom.xml, direct + transitive) |
| 4 | Dockerfile | Checkov | Missing HEALTHCHECK, running as root, unpinned base images |
| 5 | Container image | Trivy (image) | CVEs in the JRE and OS packages baked into the final image |
| 6 | DAST | OWASP ZAP | Runtime issues — missing security headers, info leaks, CORS |
| 7 | K8s manifests | Checkov | Pods as root, missing resource limits, hostPath mounts |
| 8 | Policy-as-code | Kyverno CLI | Same policies enforced at cluster admission, validated in CI |

All 9 gates run in parallel on every PR. All are **required** — a single failure blocks the merge.

## Branch workflow

```
feature/xyz ──PR──▶ main (protected: 9 gates + PR required)
     │
     └── branched from dev (default branch)
```

- **`dev`** — default branch, daily work
- **`feature/*`** — feature branches for changes
- **`main`** — protected, only accepts PRs after all 9 gates pass
- Direct pushes to `main` are blocked, even for the repo owner

## Target application

A minimal Spring Boot 4 REST API (`notes-api/`) backed by PostgreSQL. The app is the vehicle — the pipeline and infrastructure are the show.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/notes` | `POST` | Create a note |
| `/notes` | `GET` | List all notes |
| `/notes/{id}` | `DELETE` | Delete a note |

## Kubernetes infrastructure

**Cluster:** kind (Kubernetes-in-Docker) with 1 control plane + 2 workers, Calico CNI.

**Workloads:**
- `notes-api` Deployment — 3 replicas, non-root (UID 10001), read-only rootFS, CPU/memory limits, liveness/readiness probes
- `postgres` StatefulSet — 1 replica, 1Gi PVC, non-root (UID 999), tmpfs for writable paths, credentials from Secret

**Network security (zero-trust):**
- Default-deny on all ingress AND egress
- Explicit allow rules for DNS, ingress-nginx, Prometheus scraping, and app↔database (bidirectional, port 5432 only)

**Admission control:**
- 5 Kyverno ClusterPolicies in `Enforce` mode — violations are rejected at admission, not just logged

**GitOps:**
- Argo CD watches `main` and auto-syncs cluster state from `k8s/manifests/`

**Observability:**
- Prometheus + Grafana deployed via kube-prometheus-stack Helm chart
- Custom ServiceMonitor scrapes Spring Boot Actuator metrics
- 7-panel Grafana dashboard: request rate, error rate, latency (p50/p99), JVM heap, CPU, pod restarts, active HTTP threads

## Tech stack

| Layer | Tools |
|-------|-------|
| Application | Java 21, Spring Boot 4.0.5, Spring Data JPA, PostgreSQL 16.4 |
| Container | Docker multi-stage build, eclipse-temurin:21-jre, non-root, HEALTHCHECK |
| Orchestration | Kubernetes (kind v1.31), Calico CNI, Helm |
| CI/CD | GitHub Actions (9 parallel gates), branch protection |
| Security scanning | Gitleaks, Semgrep, Trivy, Checkov, OWASP ZAP, Kyverno |
| GitOps | Argo CD |
| Observability | Prometheus, Grafana, Spring Boot Actuator, Micrometer |
| Testing | JUnit 5, MockMvc, Mockito |

## Repository structure

```
secureflow/
├── .github/workflows/
│   └── security-pipeline.yml        # 9-gate CI/CD pipeline
├── .checkov.yaml                     # Checkov skip rules with documented justifications
├── .trivyignore                      # Trivy CVE exceptions with documented justifications
├── notes-api/
│   ├── Dockerfile                    # Multi-stage, non-root, HEALTHCHECK
│   ├── pom.xml                       # Spring Boot 4.0.5, Java 21
│   └── src/
│       ├── main/java/.../notesapi/   # NoteController, Note, NoteRepository
│       ├── main/resources/           # application.properties (Postgres via env vars)
│       └── test/java/.../notesapi/   # NoteControllerTest (5 unit tests)
├── k8s/
│   ├── kind-cluster.yaml             # 1 control plane + 2 workers
│   ├── calico-installation.yaml      # Calico CNI config
│   ├── manifests/
│   │   ├── 00-namespace.yaml         # secureflow namespace
│   │   ├── 10-deployment.yaml        # notes-api (3 replicas, hardened)
│   │   ├── 20-service.yaml           # ClusterIP service
│   │   ├── 30-ingress.yaml           # Ingress rule
│   │   ├── 40-netpol-default-deny.yaml          # Zero-trust baseline
│   │   ├── 41-netpol-allow-dns.yaml             # CoreDNS egress
│   │   ├── 42-netpol-allow-ingress-nginx.yaml   # Ingress controller
│   │   ├── 43-netpol-allow-prometheus.yaml      # Prometheus scraping
│   │   ├── 44-netpol-allow-postgres.yaml        # App↔DB (bidirectional)
│   │   ├── 50-postgres-secret.yaml   # Demo credentials (see note below)
│   │   ├── 51-postgres-statefulset.yaml  # PostgreSQL with PVC
│   │   └── 52-postgres-service.yaml  # Headless service for stable DNS
│   ├── policies/                     # 5 Kyverno ClusterPolicies (enforce mode)
│   ├── monitoring/                   # Prometheus/Grafana values, ServiceMonitor, dashboard
│   └── gitops/                       # Argo CD Application manifest
└── docs/reports/                     # ZAP scan reports
```

## Security decisions and trade-offs

Every skipped security check is documented with a justification in `.checkov.yaml`:

| Check | Decision | Reasoning |
|-------|----------|-----------|
| CKV_K8S_15 | Skip | `imagePullPolicy: Always` required for kind local dev with locally-built images |
| CKV_K8S_35 | Skip | Spring Boot uses env vars idiomatically; file-mounted secrets add complexity with negligible gain on a single-tenant RBAC-protected cluster |
| CKV_K8S_40 | Skip | `postgres:16.4-alpine` hardcodes UID 999; can't override without custom base image |
| CKV_K8S_43 | Skip | Digest pinning impractical for kind dev loop; correct for production registries (ECR/GHCR) |

The `50-postgres-secret.yaml` contains **demo credentials only** (`notes-demo-password-change-me`). The comment in the file documents that production deployments should use Sealed Secrets, External Secrets Operator, or HashiCorp Vault.

## Local setup

**Prerequisites:** Docker, Java 21, kubectl, kind, Helm

```bash
# Clone
git clone https://github.com/Marouanemarhrani/secureflow.git
cd secureflow

# Create kind cluster
kind create cluster --config k8s/kind-cluster.yaml --name secureflow

# Install Calico CNI
kubectl apply -f k8s/calico-installation.yaml

# Build and load the image
cd notes-api
docker build -t secureflow/notes-api:v4 .
kind load docker-image secureflow/notes-api:v4 --name secureflow
cd ..

# Deploy everything
kubectl apply -f k8s/manifests/

# Apply Kyverno policies
kubectl apply -f k8s/policies/

# Run unit tests
cd notes-api && ./mvnw test && cd ..

# Verify pods
kubectl get pods -n secureflow
```

## Key findings from building this

These are real issues encountered and resolved during the build — not theoretical:

1. **Default-deny egress blocks bidirectional traffic.** Ingress rules alone aren't enough — the originating pod also needs explicit egress permission. The app couldn't reach Postgres until both ingress AND egress NetworkPolicies were created.

2. **Spring Boot 4.0.5 reshuffled test packages.** `WebMvcTest` moved to `org.springframework.boot.webmvc.test.autoconfigure`, `TestRestTemplate` moved to `org.springframework.boot.resttestclient`. Jackson 3.0 renamed `com.fasterxml` to `tools.jackson`. No migration guide existed — discovered via JAR spelunking.

3. **Kyverno's `require-read-only-root-fs` blocks Postgres** because PostgreSQL needs writable paths for WAL, sockets, and temp files. Fixed with targeted `emptyDir` tmpfs mounts at specific paths.

4. **Postgres migration broke CI.** After moving from H2 to PostgreSQL, the smoke test container couldn't start (no DB). Fixed with a Postgres sidecar in a Docker network — mirrors production topology honestly instead of faking H2 in CI.

5. **Checkov findings are triaged, not suppressed.** Each skip has a documented justification and a "revisit when" condition, not a blanket ignore.

## License

MIT — see [LICENSE](LICENSE).

---

Built by [Marouane Marhrani](https://github.com/Marouanemarhrani) as a DevSecOps portfolio project.