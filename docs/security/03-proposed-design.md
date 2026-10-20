# 03 — Proposed FinFlow Security Design

**Status: Proposal, not implemented or approved.**

## 1. Identity is different from business data

For a Keycloak-based design:

```text
Browser / Mobile
    |
    | Login (OIDC Authorization Code + PKCE)
    v
Keycloak (identity provider)
    |
    | Access token
    v
Client ---- Bearer access token ----> FinFlow API
                                         |
                                         v
                                 Spring Security validates:
                                 signature + issuer + audience
                                 + expiry + accepted algorithm
                                         |
                                         v
                                 Role / permission checks
                                         |
                                         v
                                 Wallet owner check
                                         |
                                         v
                                 FinFlow database
```

For JWT verification, the API can normally use cached public keys from Keycloak's JWKS endpoint. It does **not** have to call Keycloak on every request.

## 2. Keep identity and domain data separate

**Identity provider:** credentials, sign-in flow, MFA, login sessions, password recovery.

**FinFlow database:** local user record, wallet ownership, transfers, business status and audit history.

Suggested local user attributes:

| Field | Purpose |
| --- | --- |
| id | Internal FinFlow user ID |
| identity_subject | Unique stable identity provider subject (`sub`) |
| name | Profile display name |
| email | Contact email, not an ownership key |
| status | Business status such as ACTIVE/BLOCKED |

**Do not trust email as the permanent identity key** because it can change.

Existing `POST/PUT/DELETE /api/v1/users` APIs will need revised access rules and possibly a provisioning/synchronization flow when authentication is introduced.

## 3. Two authorization layers

**Layer A — roles and permissions (RBAC)**

Example:

| Action | USER | SUPPORT | ADMIN |
| --- | --- | --- | --- |
| Read own profile | Yes | By explicit policy | By explicit policy |
| Read own wallet | Yes | No by default | No by default |
| Transfer from own wallet | Yes (with controls) | No | No by default |
| Handle support cases | No | Yes | Yes |
| Manage business access | No | Limited | Yes |
| View sensitive audit logs | No | No by default | Only authorized roles |

**Layer B — ownership / domain rules**

A USER role must **not** let user A read or operate on user B's wallet. Ownership must be checked at the service/database layer using the authenticated identity, not a user ID supplied by the client.

The application must apply least privilege and deny by default.

## 4. Token safety

- Validate signature, issuer (`iss`), intended audience (`aud`), expiration (`exp`) and accepted signing algorithms.
- Use asymmetric signing and a safe key-rotation process.
- Use HTTPS everywhere outside local test environments.
- Prefer short-lived access tokens; choose lifetimes after a threat/UX review.
- Protect refresh tokens and rotate/revoke them according to provider/client support.
- Logout does **not** instantly invalidate every already issued stateless JWT. Consider short lifetime and revocation strategy for high-risk use cases.
- Do not store credentials or raw tokens in application logs.
- Treat scopes/roles from tokens as inputs to authorization, not replacements for ownership validation.
- Use secure cookie/BFF or platform-secure storage depending on client type; avoid assuming browser localStorage is safe by default.

## 5. Financial-operation controls beyond login

Authentication is only the start. Before real transfers:

- Verify source wallet ownership and wallet status.
- Apply server-side amount/velocity/limit checks.
- Consider step-up MFA for risky transfers and account recovery.
- Use idempotency keys and transaction integrity controls.
- Maintain auditable security events without leaking secrets.
- Rate-limit login-related paths and sensitive operations.
- Prevent horizontal privilege escalation / broken object-level authorization.

## 6. Caching interaction (important!)

Current FinFlow uses local Caffeine caching for user reads. When authentication arrives, **never cache personalized responses under an insufficient key**. Cache keys and invalidation must account for identity and access rules. Authorization and current business status must not accidentally be bypassed because of cache hits.

## 7. Errors and safe responses

- Missing/invalid access token -> generally 401.
- Authenticated but insufficient rights -> generally 403.
- Non-owned resource -> deny; 403 or deliberate 404 depending on information-disclosure policy.
- Do not reveal whether an email exists in sensitive sign-in/recovery flows.

Reference: [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html) and [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html).
