# FinFlow Security Research

> Status: **Research only — implementation postponed**  
> Last reviewed: October 2026  
> Decision status: **Open — team to select an approach before implementation**

This folder explains how authentication and authorization could work in FinFlow. It is written in simple English so that new developers can understand the ideas before reading Spring Security code.

## Read in this order

1. [01 — Authentication and authorization basics](01-authentication-authorization-basics.md)
2. [02 — Architecture options and comparison](02-architecture-options.md)
3. [03 — Proposed design and security requirements](03-proposed-design.md)
4. [04 — Implementation plan and decision checklist](04-implementation-plan.md)

## Scope of this PR

Only documentation is included. **No authentication, login, authorization, token issuance, or API protection is currently implemented by this PR.** The architecture recommendation is not an approved decision.

## When should security be implemented?

We can postpone implementation while building local learning experiments and isolated read-only features. **Do not expose user-management, wallet or transfer APIs to untrusted clients until authentication and authorization are enforced.** Real-money transfers must not launch without ownership checks, transaction controls, and security testing.

## Official reference material

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Security OAuth2 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OAuth 2.0 Security Best Current Practice (RFC 9700)](https://www.rfc-editor.org/rfc/rfc9700)
- [OAuth 2.0 for Browser-Based Applications (RFC 9700 and current OAuth guidance should be evaluated for applicable flows)](https://www.rfc-editor.org/rfc/rfc9700)
- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)

> Verify framework versions, provider capabilities, and supported configuration against current official documentation when implementation begins.
