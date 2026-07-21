# Plan: TaskHub (8-session build)

Source spec: [`SPEC.md`](../SPEC.md) · The public
[TaskHub Roadmap](https://github.com/users/cihadacar/projects/4) and its linked
GitHub issues are the authoritative plan. This file is a non-authoritative index.

## Approach

Each course session is **one vertical slice** — a complete path from HTTP/gRPC
edge through service → repository → DB, shippable as a reviewed PR. The plan is
tracked as issues #3–#10; `tasks/todo.md` is the checklist mirror.

## Dependency graph

```
#3 Setup (skeleton, layering, exception handling)
      │
      ▼
#4 REST + Security (CRUD, DTO/validation, pagination, Swagger, JWT, RBAC, CORS)
      │
      ├────────────► #5 gRPC notification-service (unary + streaming, interceptors)
      │
      ▼
#6 Database (Flyway migrations, relations, N+1 fixes)
      │
      ▼   (needs #4, #5, #6)
#7 Testing (unit → slice → Testcontainers → E2E + coverage gate)
      │
      ▼
#8 Docker & CI/CD (multi-stage image, GH Actions: build/test/sonar/publish)
      │
      ▼
#9 Kubernetes & GitOps (kind, Flux CD, dev/prod overlays, rollback)
      │
      ▼
#10 Observability (JSON logs, Prometheus/Grafana/Loki, tracing, alerting, /ship)
```

The chain is essentially linear (= session order). Only fan-out: #5 (gRPC) and
#6 (DB) both build on #4 and can proceed in parallel once #4 lands, but #7
requires both.

## Checkpoints (human review gates)

| After | Gate — must be true before next phase |
|-------|----------------------------------------|
| **#3** | All modules build; `/actuator/health` UP; problem-JSON error contract works. |
| **#4** | Auth + RBAC enforced (401/403); validation + pagination + Swagger verified. |
| **#6** | Flyway owns schema; `ddl-auto: validate` passes; no N+1 on list endpoints. |
| **#7** | `./mvnw verify` green incl. Testcontainers + E2E; coverage ≥ 80% gate holds. |
| **#8** | Image builds & runs non-root; PR pipeline (test+sonar) green; image published. |
| **#9** | Both services `Ready` on kind via Flux; rollback demonstrated. |
| **#10** | Logs/metrics/traces/alerts verified end-to-end; `/ship` go decision. |

## Work items (GitHub issues)

| # | Slice | Labels | Depends on |
|---|-------|--------|-----------|
| [#3](https://github.com/cihadacar/taskhub/issues/3) | Project setup | session, setup | — |
| [#4](https://github.com/cihadacar/taskhub/issues/4) | REST + Security | session, rest-security | #3 |
| [#5](https://github.com/cihadacar/taskhub/issues/5) | gRPC notifications | session, grpc | #4 |
| [#6](https://github.com/cihadacar/taskhub/issues/6) | Database (JPA+Flyway) | session, database | #4 |
| [#7](https://github.com/cihadacar/taskhub/issues/7) | Test strategy | session, testing | #4, #5, #6 |
| [#8](https://github.com/cihadacar/taskhub/issues/8) | Docker & CI/CD | session, ci-cd | #7 |
| [#9](https://github.com/cihadacar/taskhub/issues/9) | K8s & GitOps | session, kubernetes | #8 |
| [#10](https://github.com/cihadacar/taskhub/issues/10) | Observability | session, observability | #9 |

Each issue carries its own **task checklist**, **acceptance criteria**, and
**verify** steps (copied from the matching `SPEC.md` session section).

## Boundaries

Follow `SPEC.md → Boundaries` (Always / Ask-first / Never) for every slice: DTOs
at the edge, Flyway owns the schema, backward-compatible proto only, no merge
below the coverage gate, secrets never committed.
