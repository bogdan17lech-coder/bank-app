# Bank (MVC + REST)
![CI](https://github.com/bogdan17lech-coder/bank-app/actions/workflows/ci.yml/badge.svg?branch=main)

Small Spring Boot 3 project: REST API (customers, accounts, transactions) + server-side MVC UI.  
Write operations are protected by Basic auth.

---

## Modules
- **bank-rest** — REST API (customers, accounts, transactions)
- **bank-mvc** — server-side UI calling the REST via `RestTemplate`
- **bank-core** — shared DTOs

## Ports
- **REST**: `8081`
- **MVC**:  `8082`

> MVC talks to REST via `bank-mvc/src/main/resources/application.yml` → `bank.api.base-url`  
> (default: `http://localhost:8081`).

---

## How to run

### 1) REST (default **MySQL**, persistent data)
MySQL must be available on `localhost:3306`. Credentials/URL live in  
`bank-rest/src/main/resources/application-mysql.yml`.

cd bank-rest
# start REST with the default profile (mysql)
mvn spring-boot:run
Switch DB profile (REST)

The default active profile is mysql (set in bank-rest/src/main/resources/application.yml).
You can override it from the command line:

# Use MySQL explicitly
cd bank-rest
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# Use H2 in-memory DB (data resets on each restart)
cd bank-rest
mvn spring-boot:run -Dspring-boot.run.profiles=h2
# H2 console: http://localhost:8081/h2-console
# Example JDBC URL: jdbc:h2:mem:bank_dev   (user: sa, empty password)

---

### 2) MVC (requires REST running)
cd bank-mvc
mvn spring-boot:run

Auth & API docs (REST)

Basic auth (write operations): user api, password secret

Swagger UI: http://localhost:8081/swagger-ui/index.html

Health check: GET http://localhost:8081/api/ping → {"status":"ok"}


---

Tests
# All modules
mvn -q clean test

# Only REST
mvn -q -pl bank-rest test

# Only MVC
mvn -q -pl bank-mvc test

---

Notes

On MySQL, foreign keys prevent deleting an account if it has transactions (audit-friendly).
Typical flow is to close accounts rather than hard-delete; or purge related rows from
account_transactions first (or use TRUNCATE with FK checks disabled in a dev environment).

If you change the REST port, update bank-mvc/src/main/resources/application.yml → bank.api.base-url.

---

Quick troubleshooting

H2 console won’t connect: use JDBC jdbc:h2:mem:bank_dev, user sa, empty password
(only when profile h2 is active).

MySQL “No database selected” in Workbench: run USE bankdb; (or your DB name from
application-mysql.yml) before truncating tables.

Port already in use: change server.port in the respective application-*.yml
or stop other processes on that port.
