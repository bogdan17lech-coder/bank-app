# Bank (MVC + REST)
![CI](https://github.com/bogdan17lech-coder/bank-app/actions/workflows/ci.yml/badge.svg?branch=main)

A small Spring Boot 3 project: REST API for customers, accounts and transactions + server-side MVC UI.  
Write operations are protected by Basic auth.

## Modules
- **bank-rest** — REST API (customers, accounts, transactions)
- **bank-mvc** — server-side UI calling the REST via `RestTemplate`
- **bank-core** — shared DTOs

## How to run
```bash
# REST (port 8080)
cd bank-rest
mvn spring-boot:run

# MVC (port 8081) — requires REST running on http://localhost:8080
cd ../bank-mvc
mvn spring-boot:run

## Auth & API docs
- Write operations require Basic auth: **user:** `api`, **password:** `secret`
- Swagger UI (REST): `http://localhost:8080/swagger-ui/index.html`
- Health check: `GET http://localhost:8080/api/ping` → `{"status":"ok"}`

## Tests
```bash
# all modules
mvn -q clean test

# only REST
mvn -q -pl bank-rest test

# only MVC
mvn -q -pl bank-mvc test

