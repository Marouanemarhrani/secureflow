# SecureFlow

An open-source DevSecOps reference pipeline with **6 security gates** running on GitHub Actions — from secrets scanning to DAST — built around a small Spring Boot REST API.

> ⚠️ **Work in progress.** This repository is being built publicly, one weekend at a time. Follow along.

## Why this exists

Most DevSecOps tutorials cover one or two security gates. This project wires together a complete, reproducible pipeline using only free, open-source tools — and documents the *why* behind every choice, not just the *how*.

## The 6 security gates

| # | Gate | Tool | Catches |
|---|---|---|---|
| 1 | Secrets scanning | Gitleaks | Hardcoded API keys, tokens, passwords |
| 2 | SAST | Semgrep | Code-level vulnerabilities (SQLi, XSS patterns) |
| 3 | Dependency scanning | Trivy (fs) | Known CVEs in libraries |
| 4 | IaC scanning | Checkov | Misconfigured Dockerfile / K8s manifests |
| 5 | Container scanning | Trivy (image) | Vulnerabilities in the final Docker image |
| 6 | DAST | OWASP ZAP | Runtime vulnerabilities on the deployed app |

## Target application

A minimal Spring Boot REST API (`notes-api/`) exposing a small Notes endpoint. The app is the vehicle; the pipeline is the show.

## Project status

- [x] Weekend 1 — Spring Boot skeleton, builds & runs
- [ ] Weekend 2 — Gates 1–3 (Gitleaks, Semgrep, Trivy fs)
- [ ] Weekend 3 — Gates 4–6 (Checkov, Trivy image, OWASP ZAP)
- [ ] Weekend 4 — Documentation, diagrams, polish

## License

MIT — see [LICENSE](LICENSE) (coming in Weekend 4).
