# 🚀 Token Management API
![Java](https://img.shields.io/badge/Java-19-blue)&nbsp; ![Spring Boot](https://img.shields.io/badge/SpringBoot-4.x-brightgreen)&nbsp; ![Tests](https://img.shields.io/github/actions/workflow/status/kevjaul/token-api-java/test-ci.yml) &nbsp;![Build Status](https://github.com/kevjaul/token-api-java/actions/workflows/auto-deploy.yml/badge.svg) &nbsp;

A RESTful API designed to manage token-based systems for multiple applications.
Each application can manage its own users and token balances securely using an API key.

---

## 🧱 Tech Stack

* Java 19+
* Spring Boot 4
* Spring Security
* Spring Data JPA
* PostgreSQL / SQL Database
* Swagger (OpenAPI)
* Quartz Scheduler

---

## ⚙️ Developer Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-repo/token-api-java.git
cd token-api-java
```

---

### 2. Create a PostgresSQL Docker container

```bash
docker run -d --name token-api-dev -e POSTGRES_DB=token_db -e POSTGRES_USER=appuser -e POSTGRES_PASSWORD=apppassword -p 5432:5432 postgres:16
```

### 3. Run the application in development mode

```bash
./gradlew bootDev
```

API will be available at:

```
http://localhost:5001
```

---

### 4. Swagger documentation

```
http://localhost:5001/swagger-ui/index.html
```

---

## 🔐 Authentication 

### 1. Get an application API Key

All `/api/tokens/**` and `/api/apikeys/` routes require:

```
X-Api-Key header: your-api-key
```

(Generated from `/api/apps/register` route)

---

## 🔁 Token Regeneration

Tokens can be regenerated based on the application's configuration:

* days
* hours
* minutes

A background job (scheduler) can automatically increment tokens.

Optional manual trigger:

```http
POST /api/tokens/regenerate
```

---

## ⚠️ Business Rules

* Token amount must stay between `minTokenAmount` and `maxTokenAmount`
* A user cannot consume more tokens than available
* Duplicate users per application are not allowed

---

## 🧪 Testing

Run tests with:

```bash
./gradlew test
```

Tests include:

* API key validation and recycling
* Token limits
* User creation conflicts
* Token regeneration logic

---

## 📌 Future Improvements

* Webhooks for token updates and enhance visibility:
  * Future route: /api/tokens/list : To consult user application list.
  * Future route: /api/tokens/{userId}/balance : PUT method to reset token amount of an application user to a specific value.
  * Prometheus ready endpoint
* CI/CD release note
* 1 per day job to set expires key and delete unused app (expiresAt/revoked)
* Logs: [Timestamp][hashedApiKey.substring(0,12)/ADMIN]([for AppId: (if ADMIN)]) -> Action processed
* (Collection Postman ?)

---
## 📚 Documentation

- [API Keys Lifecycle & Security](./API_KEYS.md)

## 👨‍💻 Author

Kevin — Cybersecurity & Backend Engineering