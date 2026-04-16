# 🚀 Token Management API

A RESTful API designed to manage token-based systems for multiple applications.
Each application can manage its own users and token balances securely using an API key.

---

## 🧱 Tech Stack

* Java 19+
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL / SQL Database
* Swagger (OpenAPI)

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

All `/api/tokens/**` routes require:

```
X-Api-Key: your-api-key
```

(Generated from `/api/applications/register` route)

---

## 🔁 Token Regeneration

Tokens can be regenerated based on the application's configuration:

* days
* hours
* minutes

A background job (scheduler) can automatically increment tokens.

Optional manual trigger:

```http
POST /api/tokens/regeneration
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

* API key validation
* Token limits
* User creation conflicts
* Token regeneration logic

---

## 📌 Future Improvements

* Webhooks for token updates
* Rate limiting per application
* Schedule jobs for token auto-regeneration

---

## 👨‍💻 Author

Kevin — Cybersecurity & Backend Engineering