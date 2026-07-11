# Axon KSP Processor

Compile-time annotation processor for the Axon framework. Resolves the dependency graph from
`@Resolve` and `@Inject` annotations and generates `Axon.init()` — zero runtime reflection.

---

## What it generates

Given:

```kotlin
class LocalDatabase @Inject constructor()

class AuthenticationService @Inject constructor(
    private val api: AuthWebApi,
    private val db: LocalDatabase
)

@Resolve(LoginIntent::class)
class LoginResolver @Inject constructor(
    private val authService: AuthenticationService
) : Resolver<LoginIntent, LoginResult> { ... }

@Resolve(LogoutIntent::class)
class LogoutResolver @Inject constructor(
    private val db: LocalDatabase
) : Resolver<LogoutIntent, LogoutResult> { ... }
```

The processor generates:

```kotlin
// build/generated/ksp/.../AxonRegistration.kt
fun Axon.init() {
    val authWebApi = lazy { AuthWebApi() }
    val localDatabase = lazy { LocalDatabase() }                                        // shared singleton
    val authenticationService = lazy { AuthenticationService(authWebApi.value, localDatabase.value) }
    val loginResolver = lazy { LoginResolver(authenticationService.value) }
    val logoutResolver = lazy { LogoutResolver(localDatabase.value) }                   // reuses localDatabase

    registerResolver(LoginIntent::class, loginResolver)
    registerResolver(LogoutIntent::class, logoutResolver)
}
```

Key properties:
- **Topological order** — dependencies always declared before their dependents
- **Lazy singletons** — each class instantiated at most once, on first use
- **Shared dependencies** — one `lazy val` per type regardless of how many resolvers use it

---

## Compile-time guarantees

| Violation | Error |
|-----------|-------|
| Dependency missing `@Inject` and has constructor params | `AuthWebApi must have an @Inject constructor. Required by: LoginResolver → AuthenticationService → AuthWebApi` |
| Circular dependency | `Circular dependency detected: A → B → A` |

Both errors point to the exact file and line number.

---

## Rules

- `@Inject` must be on the **primary constructor**
- Classes with a **no-arg constructor** are injectable without `@Inject`
- Each `@Resolve` class must implement `Resolver<I, R>`
- Resolvers **must be stateless** — all context needed to process a request must be carried by
  the Intent, all context the caller needs afterward by the Result. Internal mutable state in a
  resolver is a design error. Dependencies injected into a resolver should be stateless services,
  not stateful holders.

---

## Setup

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation("com.singularity_universe.axon:core:1.0.0")
    ksp("com.singularity_universe.axon:ksp:1.0.0")
}
```
