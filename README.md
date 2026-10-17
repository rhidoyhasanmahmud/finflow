# FinFlow

FinFlow is a production-minded digital wallet and payment platform built with Spring Boot.

The project has two goals:

1. Build a realistic backend system instead of a tutorial CRUD application.
2. Use each milestone to understand Spring and Spring Boot deeply, including the framework mechanisms behind the annotations.

## Product Scope

FinFlow will evolve toward supporting:

- User onboarding
- Wallet creation and balance management
- Deposits and withdrawals
- Wallet-to-wallet transfers
- Transaction history
- Authentication and authorization
- Audit trails
- Notifications
- Caching and rate limiting
- Event-driven processing
- Resilience and observability

## Technology Baseline

- Java 21
- Spring Boot 4.1
- Spring MVC
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- Docker Compose

Additional infrastructure such as Redis, Kafka, Testcontainers, Prometheus, and Grafana will be introduced only when the corresponding use case requires it.

## Architecture Direction

FinFlow starts as a modular monolith.

```text
com.finflow
├── user
├── wallet
├── transaction
└── common
```

The initial modules are intentionally small. New infrastructure and abstractions should be introduced because of concrete requirements, not pre-emptively.

### Module Responsibilities

**user**

Owns the user lifecycle and profile domain.

**wallet**

Owns wallet balances and balance-related business invariants.

**transaction**

Owns the financial transaction record and transaction history.

**common**

Contains only genuinely shared primitives and cross-cutting building blocks. Domain logic should remain inside the domain that owns it.

## Local Development

### Prerequisites

- JDK 21
- Maven 3.9+
- Docker with Docker Compose

### 1. Start PostgreSQL

```bash
docker compose up -d
```

PostgreSQL will be available at:

```text
localhost:5432
Database: finflow
Username: finflow
Password: finflow
```

The credentials above are for local development only.

### 2. Run the application

```bash
mvn spring-boot:run
```

The application starts on port `8080` by default.

### 3. Health check

```bash
curl http://localhost:8080/actuator/health
```

## Configuration

Runtime configuration is environment-driven where appropriate.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/finflow` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `finflow` | Database username |
| `DB_PASSWORD` | `finflow` | Database password |
| `SERVER_PORT` | `8080` | Application HTTP port |

## Engineering Principles

- Prefer constructor injection.
- Keep transaction boundaries in the application/service layer.
- Keep controllers thin.
- Keep domain invariants close to the domain model.
- Do not expose JPA entities directly through APIs.
- Avoid a generic `util` dumping ground.
- Prefer explicit behavior over premature abstraction.
- Add infrastructure only when a requirement justifies it.
- Every important Spring feature should be understood beyond its annotation.

## Spring Deep-Dive Rule

For every major Spring feature introduced into FinFlow, answer these questions:

1. What problem does it solve?
2. How would the same behavior be implemented without Spring?
3. What does Spring do internally?
4. Which bean, proxy, filter, interceptor, post-processor, or lifecycle hook is involved?
5. What are the production failure modes?

## Roadmap

### Milestone 1 — Foundation

- [x] Bootstrap Spring Boot application
- [x] PostgreSQL local environment
- [x] Actuator baseline
- [x] Initial modular package boundaries
- [ ] User creation
- [ ] Wallet creation
- [ ] Deposit funds
- [ ] Withdraw funds
- [ ] Wallet-to-wallet transfer
- [ ] Transaction history
- [ ] Request validation
- [ ] Global exception handling

Deep-dive topics:

- IoC container
- Dependency injection
- Bean discovery
- Component scanning
- Bean lifecycle
- Spring MVC request lifecycle
- DispatcherServlet
- Validation

### Milestone 2 — Persistence and Transactions

- [ ] Database migrations
- [ ] JPA entity mapping
- [ ] Persistence context experiments
- [ ] Dirty checking
- [ ] Lazy/eager loading experiments
- [ ] N+1 reproduction and fixes
- [ ] Transaction rollback experiments
- [ ] Isolation-level experiments
- [ ] Optimistic/pessimistic concurrency control

Deep-dive topics:

- Hibernate entity lifecycle
- Persistence context
- `@Transactional`
- Spring AOP proxies
- `PlatformTransactionManager`
- Transaction propagation
- Isolation levels

### Milestone 3 — Security

- [ ] Authentication
- [ ] JWT access tokens
- [ ] Refresh-token lifecycle
- [ ] Role-based authorization
- [ ] Method-level authorization

Deep-dive topics:

- `SecurityFilterChain`
- `AuthenticationManager`
- `AuthenticationProvider`
- `SecurityContext`
- Spring Security filter architecture

### Milestone 4 — Events and Asynchronous Processing

- [ ] Domain/application events
- [ ] Transaction-aware event handling
- [ ] Asynchronous notifications
- [ ] Kafka integration
- [ ] Retry and dead-letter handling
- [ ] Transactional outbox

Deep-dive topics:

- `ApplicationEventPublisher`
- `@TransactionalEventListener`
- `@Async`
- Executor configuration
- Kafka delivery semantics

### Milestone 5 — Caching and Resilience

- [ ] Redis caching
- [ ] Rate limiting
- [ ] External payment gateway simulation
- [ ] Timeout and retry policies
- [ ] Circuit breaker

Deep-dive topics:

- Spring Cache abstraction
- Cache invalidation
- WebClient
- Resilience4j

### Milestone 6 — Testing and Observability

- [ ] Unit tests
- [ ] MVC slice tests
- [ ] JPA slice tests
- [ ] Integration tests with Testcontainers
- [ ] Metrics
- [ ] Prometheus
- [ ] Grafana
- [ ] Distributed tracing

Deep-dive topics:

- Spring TestContext framework
- Test slices
- Application context caching
- Actuator
- Micrometer

## Current Status

The repository currently contains only the platform foundation. The first business milestone will implement the complete flow:

```text
Create User
    ↓
Create Wallet
    ↓
Deposit
    ↓
Transfer
    ↓
Transaction History
```

The implementation will remain intentionally incremental so that each Spring concept can be introduced, tested, and understood independently.
