# 01 — Authentication and Authorization (Easy English)

## Two different questions

**Authentication:** *Who are you?* A person signs in and proves their identity.

**Authorization:** *What are you allowed to do?* The application checks whether the signed-in person may perform an action.

Example:

- Hasan signs in successfully: **authentication succeeds**.
- Hasan reads his own profile: **authorization allows it**.
- Hasan tries to transfer money from Rafi's wallet: **authorization must deny it**, even though Hasan is logged in.

## Simple request journey

```text
Client sends a request
       |
       v
Spring Security filter chain
       |
       +-- Missing / invalid login? --> 401 Unauthorized
       |
       v
Authenticated user is known
       |
       v
Check role / permission
       |
       +-- Permission missing? ------> 403 Forbidden
       |
       v
Check wallet ownership
       |
       +-- Not their wallet? --------> Deny (403 or 404 by policy)
       |
       v
Run business operation
```

A **401** response generally means the request has no acceptable authentication. A **403** generally means the identity is known but the action is not permitted. An API may intentionally return 404 to avoid revealing whether another person's resource exists.

## Terms you will meet

| Term | Plain explanation |
| --- | --- |
| Principal | The user or service making the request |
| Credential | Something used to prove identity, such as a password or MFA factor |
| Session | Server-managed login state |
| Access token | A short-lived credential presented to an API |
| Refresh token | A credential used to obtain a new access token |
| JWT | A structured token format; **not automatically encrypted** |
| OAuth 2.0 | A framework for delegated access/authorization |
| OpenID Connect (OIDC) | Authentication layer built on OAuth 2.0 |
| Role | Broad group such as USER, SUPPORT, ADMIN |
| Permission | Specific action such as wallet:read |
| Ownership | Check that the requested wallet belongs to the current user |
| MFA | Additional authentication factor |

## Important misconception

**JWT does not mean OAuth2.** You can create a JWT yourself without implementing OAuth2 correctly. Also, a valid JWT does not give permission to access any wallet. FinFlow must still make authorization decisions.

## FinFlow today

FinFlow currently has basic user CRUD endpoints. The `users` table stores name/email; it is **not** an identity system. This R&D does not secure those endpoints.

## Learning goals

When we implement security later, trace the full Spring request flow:

```text
HTTP -> SecurityFilterChain -> authentication
     -> SecurityContext -> authorization
     -> controller -> service ownership check -> database
```

Read: [Spring Security reference](https://docs.spring.io/spring-security/reference/).
