# 02 — Architecture Options

There is no final selection yet. We should choose based on user experience, maintenance burden, and what we want to learn.

## Option A: Spring Security with server-side sessions

The server verifies credentials and keeps a login session. The browser uses a secure session cookie.

**Pros:** mature pattern, session invalidation is straightforward, good fit for server-rendered web apps.

**Cons:** distributed deployments need session storage or session affinity; cross-origin browser behavior and CSRF defenses must be designed carefully.

**Choose when:** a browser-centric app and server-managed session model are the priority.

## Option B: Custom login + JWT

FinFlow verifies passwords, signs JWT access tokens, handles refresh tokens, and manages logout/revocation itself.

**Pros:** deep hands-on learning about Spring Security and tokens; no separate identity server initially.

**Cons:** **high security responsibility**. The team must design password hashing, brute-force defenses, account recovery, key rotation, refresh-token storage/rotation, MFA, revocation and monitoring.

**Choose when:** implementation of the identity system itself is the learning objective. Do not assume a custom JWT endpoint is a standards-compliant OAuth2/OIDC provider.

## Option C: Keycloak + Spring Security OAuth2 Resource Server

Keycloak is the identity provider. It handles user login and identity flows. FinFlow validates access tokens and enforces its own domain authorization.

**Pros:** standards-based OIDC/OAuth2, MFA and identity-management support, easier to add multiple types of clients later.

**Cons:** extra service and operational complexity: realm/client setup, backups, upgrades, availability and observability.

**Choose when:** we want a production-oriented identity boundary and still want to learn Spring filter chains, JWT validation and method security.

**Initial research preference: Option C**, subject to an architecture decision together.

## Option D: Spring Authorization Server

Build and operate our own standards-based authorization server with Spring tooling, plus a separate FinFlow resource server.

**Pros:** strong protocol-level learning and control.

**Cons:** much larger scope and significant responsibility for identity and OAuth2/OIDC operations.

**Choose when:** developing an authorization server is explicitly a project goal.

## Comparison

| Topic | Server session | Custom JWT | Keycloak | Spring Authorization Server |
| --- | --- | --- | --- | --- |
| Initial complexity | Low–Medium | Medium | Medium–High | High |
| Spring Security learning | High | Very high | High | Very high |
| OAuth2/OIDC out of box | No | No | Yes | Yes, with configuration |
| External identity infrastructure | No | No | Yes | Yes (ours) |
| Identity maintenance effort | Medium | High | Medium + operations | High |
| Future web/mobile clients | Possible | Possible, custom work | Strong fit | Strong fit |
| Recommended for first FinFlow security release | Possible | Not preferred | **Preferred for discussion** | Not preferred |

## Suggested client flows

- **Web/mobile user:** OIDC Authorization Code flow with PKCE; no deprecated password grant.
- **Service-to-service:** OAuth2 client credentials with narrowly scoped service permissions, only if needed later.
- **Backend API:** Spring Security OAuth2 Resource Server, which validates access tokens.

Do not put a confidential client secret in a public browser/mobile app. Never use an ID token as the API access token.

## Decision not yet made

Before deciding, we should agree on whether FinFlow will have a browser frontend, mobile app, external partners, and whether maintaining Keycloak is acceptable.

Official sources: [Spring Security](https://docs.spring.io/spring-security/reference/), [Keycloak](https://www.keycloak.org/documentation), [OAuth security best practices](https://www.rfc-editor.org/rfc/rfc9700).
