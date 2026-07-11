# TODO

## ContextWrapper
Execution context should wrap around intent execution, not live explicitly inside the Intent.

When the same Intent type is dispatched by multiple instances concurrently, listeners may need
a context to correctly identify which execution produced which result.

A `ContextWrapper` is the proposed solution — context surrounds the execution scope organically
rather than being carried as a field on the Intent itself.

---

## DI — @Inject support (KSP processor)

Build a lightweight DI system inside the KSP processor. No runtime reflection — everything
is resolved and wired at compile time via the dependency graph.

### Phase 1: Basic @Inject support

- [x] 1. Add `@Inject` annotation to `:core`
- [x] 2. KSP: read `@Inject` constructor from `@Resolve` classes
- [x] 3. KSP: recursively collect all `@Inject` dependencies (build DAG)
- [x] 4. KSP: detect shared dependencies — one type = one lazy val, not duplicated
- [x] 5. KSP: topological sort DAG → correct generation order
- [x] 6. KSP: generate full lazy val chain + registerResolver in `init()`
- [x] 7. KSP: compile error if a dependency has no `@Inject` constructor
- [x] 8. KSP: compile error if circular dependency is detected

### Phase 2: Edge cases

- [x] 9.  Handle no-arg constructor without `@Inject` (treat as injectable)
- [x] 10. Clear error messages — name the exact class that has the problem

### Out of scope (for now)
- Interface binding (`@Bind`)
- Scopes beyond singleton
- Qualifiers (`@Named`)
- Optional dependencies
