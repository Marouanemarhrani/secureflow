# SecureFlow

[![security-pipeline](https://github.com/Marouanemarhrani/secureflow/actions/workflows/security-pipeline.yml/badge.svg)](https://github.com/Marouanemarhrani/secureflow/actions/workflows/security-pipeline.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F)](https://spring.io/projects/spring-boot)

An open-source **DevSecOps reference pipeline** with **6 security gates** running on GitHub Actions — from secrets scanning to DAST — built around a small Spring Boot REST API.

> ⚠️ **Work in progress.** This repository is being built publicly, one weekend at a time. Follow along.

## Why this exists

Most DevSecOps tutorials cover one or two security gates. This project wires together a complete, reproducible pipeline using only free, open-source tools — and documents the *why* behind every choice, not just the *how*.

## The 6 security gates

| # | Gate | Tool | Catches | Status |
|---|---|---|---|---|
| 1 | Secrets scanning | Gitleaks | Hardcoded API keys, tokens, passwords | ✅ live |
| 2 | SAST | Semgrep | Code-level vulnerabilities (SQLi, XSS patterns) | ✅ live |
| 3 | Dependency scanning | Trivy (fs) | Known CVEs in libraries | ✅ live |
| 4 | IaC scanning | Checkov | Misconfigured Dockerfile / K8s manifests | 🚧 Weekend 3 |
| 5 | Container scanning | Trivy (image) | Vulnerabilities in the final Docker image | 🚧 Weekend 3 |
| 6 | DAST | OWASP ZAP | Runtime vulnerabilities on the deployed app | 🚧 Weekend 3 |

## Target application

A minimal Spring Boot REST API (`notes-api/`) exposing a Notes endpoint. The app is the vehicle; the pipeline is the show.

Endpoints:
- `POST /notes` — create a note
- `GET /notes` — list all notes
- `DELETE /notes/{id}` — delete a note

## Quick start

Requires Docker and Java 21 for local dev.

    git clone git@github.com:Marouanemarhrani/secureflow.git
    cd secureflow/notes-api

    # Option A — run with Maven
    ./mvnw spring-boot:run

    # Option B — build and run in Docker
    docker build -t secureflow/notes-api:latest .
    docker run --rm -p 8080:8080 secureflow/notes-api:latest

    # In another terminal:
    curl http://localhost:8080/notes

## Project status

- [x] Weekend 1 — Spring Boot skeleton, Dockerfile, verified running
- [x] Weekend 2 — Gates 1–3 (Gitleaks, Semgrep, Trivy fs) with demo branches proving each catches real issues
- [ ] Weekend 3 — Gates 4–6 (Checkov, Trivy image, OWASP ZAP)
- [ ] Weekend 4 — Architecture docs, pipeline diagram, findings.md, polish

## License

MIT — see [LICENSE](LICENSE).