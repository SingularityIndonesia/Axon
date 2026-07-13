# Changelog

## [1.0.0-alpha4] - 2026-07-13

### Changed
- `Intent` is no longer a value object. `parent` is now a constructor parameter on the `Intent` base class, permanently bound at construction time and inherited by all subclasses — no more `abstract val parent` override required.
- All Intent subclasses should be declared as regular `class`, not `data class`. Intents are commands with identity, not interchangeable values.

### Added
- Compile-time warning via KSP when an Intent type referenced in `@Resolve` is declared as a `data class`. Using `data class` for an Intent is discouraged because `copy()` produces a new instance with different identity, silently severing the parent chain.

### Fixed
- Pre-existing inconsistency in the CLI sample where `LoginIntent` was called with flat `username`/`password` parameters instead of the `LoginData` wrapper.

---

## [1.0.0-alpha3] - 2026-07-12

### Added
- `@Bind` annotation for interface-to-implementation binding in the KSP dependency graph.
- Shared dependency detection — a dependency used by multiple resolvers is declared as a single lazy val in the generated `init()`.

### Changed
- Renamed `utils` package (previously `untils`).

### Docs
- README rewritten with cleaner structure and expanded philosophy section.
- FAQ expanded with additional entries covering DI scope, session scope, memory pressure, and concurrent dispatch.

---

## [1.0.0-alpha2] - 2026-07-11

### Added
- KSP processor generates full lazy dependency wiring from `@Inject` constructor graph.
- Compile-time circular dependency detection.
- No-arg classes are implicitly injectable — `@Inject` is optional when the constructor has no parameters.
- Clear error messages when `@Inject` is missing on a class with constructor parameters.

### Changed
- `Handler` renamed to `Resolver`, `@Resolver` renamed to `@Resolve`.
- Resolver instantiation is now lazy — resolvers are created on first use and reused afterward.

---

## [1.0.0-alpha1]

Initial release.

### Added
- `Intent<R>` base class — declarative command object that contracts its result type.
- `Resolver<I, R>` interface — stateless processor for a single business contract.
- `@Resolve` annotation — links a resolver to its intent at compile time.
- `Axon.dispatch()` — suspending function that routes an intent to its resolver and returns the result directly.
- `ResolverException` — surfaced when a resolver violates its contract by throwing.
- KSP processor generates `Axon.init()` — wires all resolvers automatically, zero runtime reflection.
- Duplicate resolver detection at compile time.
