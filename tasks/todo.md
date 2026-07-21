# TODO: TaskHub

Mirrors the authoritative public
[TaskHub Roadmap](https://github.com/users/cihadacar/projects/4). Check off when
the slice's PR merges. See [`plan.md`](plan.md) and [`SPEC.md`](../SPEC.md).

- [ ] **[#3](https://github.com/cihadacar/taskhub/issues/3) — Session 1: Project setup** (skeleton, layering, exception handling, SPEC)
- [ ] **[#4](https://github.com/cihadacar/taskhub/issues/4) — Session 2: REST + Security** (CRUD, DTO/validation, pagination, Swagger, JWT, RBAC, CORS) · _needs #3_
- [ ] **[#5](https://github.com/cihadacar/taskhub/issues/5) — Session 3: gRPC notifications** (proto, unary + streaming, interceptors) · _needs #4_
- [ ] **[#6](https://github.com/cihadacar/taskhub/issues/6) — Session 4: Database** (Flyway, relations, N+1 fixes) · _needs #4_
- [ ] **[#7](https://github.com/cihadacar/taskhub/issues/7) — Session 5: Testing** (unit/slice/Testcontainers/E2E + coverage gate) · _needs #4,#5,#6_
- [ ] **[#8](https://github.com/cihadacar/taskhub/issues/8) — Session 6: Docker & CI/CD** (multi-stage image, GH Actions) · _needs #7_
- [ ] **[#9](https://github.com/cihadacar/taskhub/issues/9) — Session 7: K8s & GitOps** (kind, Flux, overlays, rollback) · _needs #8_
- [ ] **[#10](https://github.com/cihadacar/taskhub/issues/10) — Session 8: Observability** (logs/metrics/traces/alerts, /ship) · _needs #9_

### Checkpoints
- ⛳ After #3 · After #4 · After #6 · After #7 · After #8 · After #9 · After #10 — review gate before proceeding (see plan.md).
