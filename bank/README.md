# Bank (MVC + REST)

Spring Boot 3, Spring MVC, Spring Security (Basic), JPA/Hibernate, H2/MySQL.

## Modules
- `bank-rest` — REST API (customers, accounts, transactions)
- `bank-mvc`  — UI (server-side MVC), ходит в REST через `RestTemplate`

## Run
```bash
# REST
cd bank-rest
mvn spring-boot:run

# MVC (нужен запущенный REST на localhost:8080)
cd ../bank-mvc
mvn spring-boot:run
