# CLearn Local Run

## Local Services

Start local MySQL, RabbitMQ, and Redis first. Keep secrets in environment variables instead of `application.yml`.

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"

docker compose up -d mysql rabbitmq redis

$env:CLEARN_DB_URL='jdbc:mysql://localhost:3306/clearn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:CLEARN_DB_USERNAME='clearn'
$env:CLEARN_DB_PASSWORD='clearn'
$env:CLEARN_RABBITMQ_HOST='localhost'
$env:CLEARN_RABBITMQ_USERNAME='guest'
$env:CLEARN_RABBITMQ_PASSWORD='guest'
$env:CLEARN_REDIS_HOST='localhost'
$env:CLEARN_REDIS_PORT='6379'
$env:CLEARN_SUBMISSION_RATE_LIMIT_ENABLED='true'
$env:CLEARN_TOKEN_SECRET='replace-with-a-long-random-secret'
$env:CLEARN_INTERNAL_TOKEN='replace-with-a-worker-shared-token'
$env:CLEARN_WEB_ALLOWED_ORIGINS='http://localhost:5173'

$env:CLEARN_WORKER_DATASOURCE_URL=$env:CLEARN_DB_URL
$env:CLEARN_WORKER_DATASOURCE_USERNAME='clearn'
$env:CLEARN_WORKER_DATASOURCE_PASSWORD='clearn'
$env:CLEARN_WORKER_RABBITMQ_HOST=$env:CLEARN_RABBITMQ_HOST
$env:CLEARN_WORKER_RABBITMQ_USERNAME=$env:CLEARN_RABBITMQ_USERNAME
$env:CLEARN_WORKER_RABBITMQ_PASSWORD=$env:CLEARN_RABBITMQ_PASSWORD
$env:CLEARN_WORKER_INTERNAL_API_BASE_URL='http://localhost:8080'
$env:CLEARN_WORKER_INTERNAL_API_TOKEN=$env:CLEARN_INTERNAL_TOKEN
```

## Optional Remote Services

The checked-in defaults point MySQL, RabbitMQ, and Redis at `localhost`. For a remote deployment, override the same variables above with the remote host, user names, and passwords from your secret store. Do not commit those passwords into this file.

```powershell
$env:CLEARN_DB_URL='jdbc:mysql://<remote-host>:3306/clearn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:CLEARN_DB_USERNAME='clearn'
$env:CLEARN_DB_PASSWORD='<remote-db-password>'
$env:CLEARN_RABBITMQ_HOST='<remote-host>'
$env:CLEARN_RABBITMQ_PASSWORD='<remote-rabbitmq-password>'
$env:CLEARN_REDIS_HOST='<remote-host>'
$env:CLEARN_REDIS_PORT='6379'
$env:CLEARN_WORKER_DATASOURCE_URL=$env:CLEARN_DB_URL
$env:CLEARN_WORKER_DATASOURCE_USERNAME=$env:CLEARN_DB_USERNAME
$env:CLEARN_WORKER_DATASOURCE_PASSWORD=$env:CLEARN_DB_PASSWORD
$env:CLEARN_WORKER_RABBITMQ_HOST=$env:CLEARN_RABBITMQ_HOST
$env:CLEARN_WORKER_RABBITMQ_PASSWORD=$env:CLEARN_RABBITMQ_PASSWORD
```

## Judge Runner Image

The worker compiles C code with host `gcc`, then runs the compiled Linux executable in Docker. Use a Linux worker host or WSL environment for real judging.

```powershell
docker build -t clearn-c-runner:latest docker/c-runner
```

## Start Services

Open three PowerShell terminals after setting the environment variables above.

```powershell
.\mvnw.cmd -pl clearn-api spring-boot:run
```

```powershell
.\mvnw.cmd -pl clearn-worker spring-boot:run
```

```powershell
cd clearn-web
npm.cmd install
npm.cmd run dev
```

Open `http://localhost:5173`. The Vue development server proxies `/api` to `http://localhost:8080`.

For a separated deployment, build the frontend independently:

```powershell
cd clearn-web
$env:VITE_API_BASE_URL='https://api.example.com'
npm.cmd run build
```

Deploy `clearn-web/dist` with a static web server or CDN. Set `CLEARN_WEB_ALLOWED_ORIGINS` on the API service to the frontend origin, for example `https://learn.example.com`. Do not copy the frontend build into `clearn-api`; the API service only exposes `/api`.

Development accounts from test/dev seed data:

- `student` / `password`
- `admin` / `password`

## Manual Smoke Test

1. Log in as `student`.
2. Open `A+B Problem`.
3. Submit AC code:

```c
#include <stdio.h>
int main(void) {
    int a, b;
    scanf("%d%d", &a, &b);
    printf("%d\n", a + b);
    return 0;
}
```

4. Wait for the submission status to become `AC`.
5. Submit code that prints a constant and wait for `WA`.
6. Log in as `admin`, create an exam, bind an enabled problem, and verify the result page.

## Tests

Automated tests use H2 and mocks where possible; they do not require a Docker daemon or the remote services.

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

This workspace may not be a usable Git repository. If Git commands fail, treat the files as local workspace changes.
