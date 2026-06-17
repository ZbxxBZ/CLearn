# CLearn

CLearn is a C programming online judge platform for classroom practice and exams. It provides separated student and administrator portals, a Spring Boot API, an asynchronous judge worker, and a Vue 3 + Element Plus frontend.

## Features

- Student login, problem browsing, practice submissions, and submission history.
- Administrator problem, test case, and exam management.
- Practice mode and exam mode with per-problem scoring.
- RabbitMQ-backed asynchronous judging.
- Docker-isolated C execution through a dedicated judge worker.
- MySQL persistence and Redis-backed submission rate limiting.
- Separated frontend deployment with Vite and Nginx-friendly production output.

## Architecture

- `clearn-api`: Spring Boot REST API for auth, problems, exams, submissions, and internal judge callbacks.
- `clearn-worker`: Spring Boot worker that consumes judge tasks, compiles C code with GCC, runs executables in Docker, and reports results.
- `clearn-common`: Shared messaging and result models.
- `clearn-web`: Vue 3 + Element Plus frontend with student and admin portals.
- `docker/c-runner`: Minimal Linux runner image for executing compiled C programs.

## Quick Start

Start MySQL, RabbitMQ, and Redis, then run the API, worker, and frontend as separate services. The default checked-in configuration targets local services and can be overridden with environment variables.

```powershell
docker compose up -d mysql rabbitmq redis
docker build -t clearn-c-runner:latest docker/c-runner

.\mvnw.cmd -pl clearn-api spring-boot:run
.\mvnw.cmd -pl clearn-worker spring-boot:run

cd clearn-web
npm install
npm run dev
```

Open `http://localhost:5173`. See [docs/run-local.md](docs/run-local.md) for environment variables, seed accounts, and smoke-test steps.

## Deployment Notes

Do not commit production passwords, tokens, database hosts, or server-specific settings. Configure production services through environment variables or systemd `Environment=` entries.
