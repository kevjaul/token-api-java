# 🔑 API Keys – Roles, Lifecycle & Security

This document describes how API keys are managed, their lifecycle, constraints, and security rules.

---

## 📌 Overview

API keys are used to authenticate and authorize access to the API.  
Each key is associated with a role and, for `CLASSIC` keys, linked to a single application.

---

## 🧩 Roles

### 1. `Role.CLASSIC`

- Used for standard application access
- Linked to **one and only one application**
- Subject to:
  - Expiration
    - Default validity: **30 days**
    - After expiration status becomes `EXPIRED` and key is rejected on any request
    - Expiration is enforced automatically using a daily scheduled job.
  - Rotation rules
    - Using `/api/apikeys/recycle` route.
    - A 10 minutes cooldown after key creation before rotating key again. To prevents abuse and excessive key creation.

---

### 2. `Role.ADMIN`

- For now, there's **only one `Role.ADMIN` API key**. Automatically generated on API start, it must be configured via the `ADMIN_API_KEY` environment variable and is referred has `admin.api.key` in the code. 
- Not linked to a specific application, but used for saved applications management.
- Not subject to:
  - Expiration
  - Rotation constraints
  - Rate limiting
- Must be used with a valid `X-Target-App` header for scoped operations.
- Can generate new `CLASSIC` keys for any existing application.

---

## ⏳ Lifecycle

Each API key can be in one of the following states:

- **`Status.ACTIVE`** → On API key creation, fully usable.  
- **`Status.ROTATING`** → Take this state when the API key is used for the `/api/apikeys/recycle` route. **Is still usable** but only for a grace period of 24 hours.
- **`Status.EXPIRED`** → Automatically set by a daily scheduled job (`cleanupExpiredApiKeysAndApps()`). Set `Status.EXPIRED` for every key which has her `expiresAt` Instant value lower than Instant.now() when the job is running. With this state, the API key is **no longer usable**.  
- **`Status.REVOKED`** → Manually disabled with immediate effect. The API key is **no longer usable**.

API keys that remain in `Status.EXPIRED` or `Status.REVOKED` for more than **30 days** are automatically deleted.

If **an application no longer has any associated API keys** (after cleanup), it is automatically **removed along with its related datas**.

### `Role.CLASSIC` key rotation

Only accessible via `/api/apikeys/recycle` route.

- Only allowed if key is in `Status.ACTIVE` state. (Or need a `Role.ADMIN` key to enforce a new `Role.CLASSIC` key generation)
- New generated key is valid for: **30 days**
- Not allowed if key is:
  - `Status.ROTATING` → If the key is in this state it means, it has been used in the last 24 hours to recycle your key. Then, use the new generated key.
  - `Status.EXPIRED` → The key was not recycle before expiration. Contact someone who has access to the `Role.ADMIN` key, and ask him to processed a recycle for your `appId`.
  - `Status.REVOKED` → Contact someone who has access to the `Role.ADMIN` key, to either modify the status to `Status.ACTIVE` or recreate a new API key for your app.

In case of invalid condition on API key recycling, the API returns a JSON object like:

```json
{ "error": "API_KEY_<STATE>",
  "message": "<A more descriptive and explicative message.>" }
```

---

## 🛡️ Rate Limiting (Key-Based)

Rate limiting is enforced **per API key**, meaning each key has its own usage quota.

- Limits are applied based on the **`X-Api-Key` header**
- Each API key is independently tracked

### Default Limits

- `GET`, `POST`, `PUT`: **20 requests per day**
- `DELETE`: **5 requests per day**

### Strategy

Rate limiting is currently in-memory (Caffeine).
States aren't database persistent and are automatically reset on application restart.

In a production  or a more professionnal environment, please consider Redis-backed Bucket4J.

Counters reset progressively over time, thanks to:
  - Token Bucket algorithm
  - Sliding time window (24h)

### Dedicated Headers

Each response includes the `X-RateLimit-Remaining` header, indicating how many requests remain for the current request type (GET, POST, etc.) for the given API key.

When the **limit reaches 0**, the API returns a `429 TOO_MANY_REQUESTS` response along with a `Retry-After` header, indicating how many seconds to wait before retrying.

### Notes

- `Role.ADMIN` keys are **not subject to rate limiting** and bypass all usage restrictions.
- During key rotation:
  - Both keys (ACTIVE + ROTATING) may be usable
  - Each key maintains its own rate limit quota
- Abuse prevention is reinforced via:
  - rotation cooldown (10 minutes)
  - lifecycle constraints

---

## 🔐 Security Considerations

- API keys are UUID auto-générated and **stored as SHA-256 hashes** in the database.
- The **raw API key is only returned once at creation time** and cannot be retrieved afterward.
- It is the **client's responsibility to securely store the API key** (e.g., environment variables, secret managers).
- API keys should **never be exposed in logs, client-side code, or public repositories**.
- For logging purposes, and auditability, Logs contain partial hashed API key values.(The first 12 characters)
