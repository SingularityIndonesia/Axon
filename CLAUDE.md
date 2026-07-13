# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run all tests
./gradlew test

# Run only core tests (JVM target)
./gradlew :core:jvmTest

# Run only KSP processor tests
./gradlew :ksp:test

# Run the CLI sample
./gradlew :sample:cli:run

# Run a single test by name
./gradlew :ksp:test --tests "com.singularity_universe.axon.ksp.AxonSymbolProcessorTest.circular dependency emits compile error"

# Publish to Maven Central (requires AXON_SONATYPE_USERNAME and AXON_SONATYPE_PASSWORD)
./gradlew publishAllPublicationsToCentralPortal
```

## Modules

| Module | Purpose |
|--------|---------|
| `:core` | KMP library — `Intent`, `Axon`, `Resolver`, annotations, exceptions |
| `:ksp` | KSP annotation processor — generates `Axon.init()`, validates contracts |
| `:sample:cli` | JVM CLI sample demonstrating the full framework |

## Architecture

Axon organizes applications around business operations, each expressed as a compile-time contract:

```
Intent → Resolver → Result
```

### Core (`core/src/commonMain/`)

- **`Intent<R>`** — abstract base class for all intents. Constructor param `parent: Intent<*>?` (required, no default) tracks the chain of operations that led to this intent. `parent = null` is valid for root/standalone intents.
- **`Resolver<I, R>`** — interface with a single `suspend fun resolve(intent: I): R`. Must never throw — all failures go in the result type.
- **`Axon`** — routes intents to resolvers via `dispatch(intent)`. Resolvers are registered lazily. Catches resolver exceptions, emits a fatal log, and re-throws as `ResolverException`.
- **`@Resolve(IntentClass::class)`** — marks a resolver class. Picked up by KSP to auto-generate `Axon.init()`.
- **`@Inject`** — marks the constructor used for dependency injection.
- **`@Bind(Interface::class)`** — on a concrete class; tells KSP to inject this class wherever the interface is required.

### KSP Processor (`ksp/src/main/`)

`AxonSymbolProcessor` runs at compile time and:
1. Collects all `@Resolve`-annotated classes
2. Builds a dependency graph from `@Inject` constructors (resolving `@Bind` interfaces)
3. Detects circular dependencies — compile error
4. Detects missing `@Inject` — compile error
5. Warns if an Intent type is a `data class` (breaks parent chain via `copy()`)
6. Errors if an Intent type has a default value on its `parent` constructor param (parent must be explicitly passed)
7. Topologically sorts the graph and generates `AxonRegistration.kt` containing `fun Axon.init()` with lazy vals

Generated file location: `sample/cli/build/generated/ksp/main/kotlin/com/singularity_universe/axon/generated/AxonRegistration.kt`

### KSP Tests

Tests in `:ksp` use `kctfork` (kotlin-compile-testing fork) to compile source stubs in-memory and assert on generated code and error/warning messages. See `AxonSymbolProcessorTest` for patterns.

## Intent Rules

- Declare intents as `class`, never `data class` — KSP warns if violated
- `parent` must have **no default value** — KSP errors if violated
- Intents are commands with identity, not value objects — `equals`/`hashCode` are by reference

## Versioning & Release

Versions are set in `core/build.gradle.kts` and `ksp/build.gradle.kts` (`version = "..."`). Also update `README.md` artifact references and `CHANGELOG.md`.

Release steps:
1. Bump version in both `build.gradle.kts` files and `README.md`
2. Update `CHANGELOG.md`
3. Commit and push
4. `./gradlew publishAllPublicationsToCentralPortal`
5. `git tag 1.0.0-alphaX && git push origin 1.0.0-alphaX`
6. `gh release create 1.0.0-alphaX ...`

Tags use no `v` prefix (e.g. `1.0.0-alpha5`, not `v1.0.0-alpha5`).
