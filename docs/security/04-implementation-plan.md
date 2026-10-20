# 04 — Implementation Plan and Decision Checklist

**This is a future plan, not work completed in this PR.** We will decide together before writing security code.

## Prerequisites

1. Decide: Keycloak, server-side sessions, custom JWT, or Spring Authorization Server.
2. Identify clients: web, mobile, machine-to-machine.
3. Define roles, permissions, wallet ownership rules and account lifecycle.
4. Decide MFA and session/logout requirements.
5. Confirm how existing local users will map to identities.
6. Agree on acceptable operational complexity and deployment model.

## Suggested implementation slices (separate PRs)

### PR A — Security foundations

- Add Spring Security dependencies and secure defaults.
- Protect all non-public endpoints (deny by default).
- Document 401 vs 403 behavior and CSRF policy for chosen client architecture.
- Add automated tests that anonymous clients cannot access protected APIs.

### PR B — Identity integration

- Configure selected identity approach (for example Keycloak OIDC + Resource Server).
- Validate issuer, audience, signing key and expiration.
- Model stable provider `sub` to local FinFlow user mapping.
- Test valid/invalid/expired tokens; test invalid issuer and audience.
- Keep credentials out of code and Git history.

### PR C — Permissions and business ownership

- Roles/permissions, method security and default-deny behavior.
- Enforce own-profile and own-wallet access.
- Remove unprotected arbitrary user modification/deletion.
- Test that two different users cannot access each other's resources.

### PR D — Sessions, MFA and operational security

- Decide session termination, token refresh, rotation/revocation.
- Step-up authentication for high-risk business operations.
- Audit authentication/authorization decisions without leaking secrets.
- Rate limiting, monitoring, threat cases and recovery flow.

### PR E — CI security tests and developer docs

- Spring Security-focused unit/integration tests.
- Negative tests (missing token, wrong role, cross-user access).
- CI quality gate must fail when critical security behavior breaks.
- Simple English explanations plus setup and troubleshooting guides.

## Minimum acceptance cases

| Test | Expected |
| --- | --- |
| Anonymous GET on a protected user | 401 |
| Expired or invalid JWT | 401 |
| Valid identity but insufficient permission | 403 |
| User A tries to read User B's wallet | Denied |
| User A attempts transfer from User B's wallet | Denied |
| Admin-only API called by normal user | 403 |
| Wrong token audience/issuer | 401 |
| Disabled business account attempts transfer | Denied |
| Sensitive data in logs | Not present |

## What we explicitly will not claim

- CodeQL passing does not prove runtime authorization is correct.
- JWT validity does not prove wallet ownership.
- A documented approach is not deployed security.
- A staging security test is not proof of production compliance.

## Decision record (fill in together)

| Question | Decision |
| --- | --- |
| Identity solution | Pending |
| Supported client types | Pending |
| Web token storage approach | Pending |
| Access token lifetime | Pending |
| MFA/step-up requirements | Pending |
| Role + permission model | Pending |
| Who can modify/delete users | Pending |
| Token revocation policy | Pending |
| Deployment and backup responsibilities | Pending |

**Current recommendation for discussion:** Keycloak + Spring Security OAuth2 Resource Server, plus domain-level ownership checks. The team may choose another option after discussing trade-offs.

**Delivery rule:** Experimental local work can continue without this module, but no internet-exposed wallet or money-transfer release until these security gates are implemented and tested.
