# Axon

Software architecture has given us many valuable patterns—dependency injection, Clean Architecture, MVVM, MVI, modularization, and more. They help us build systems that are easier to organize, test, and maintain.

As applications evolve, however, it is easy for the architecture itself to become the primary focus. Teams invest increasing effort in refining how software is structured, while the business operations it exists to perform become distributed across layers and abstractions.

Sometimes, the architecture intended to clarify the system ends up obscuring it.

Axon starts from a different premise: architecture should first express what the software is before it describes how the software is built.

---

> **At its core, a business application is a collection of business operations. Each operation naturally follows Intent → Process → Result.**

Axon is the backbone for applications built around this philosophy.

Rather than organizing software around UI, repositories, services, or infrastructure, Axon organizes it around business operations.

UI, databases, networks, files, caches, and external services become equal I/O participants—not architectural foundations.

---

# A Business Operation

Every business operation in Axon is expressed as a compile-time contract.

```
Intent
   │
   ▼
Resolver
   │
   ▼
Result
```

The contract consists of three parts:

- **Intent** — what is requested.
- **Resolver** — how it is processed.
- **Result** — every possible outcome.

These three components are permanently linked together and validated at compile time.

---

## Intent

An `Intent` is a pure immutable object describing what the caller wants to accomplish.

It contains only the information required to perform the operation.

```kotlin
data class LoginIntent(
    val username: String,
    val password: String
) : Intent<LoginIntent.Result>() {

    sealed class Result {
        data class LoginSuccess(val token: String) : Result()
        data object LoginBlocked : Result()
        data object UnableToProcess : Result()
    }
}
```

The nested `Result` is a recommended convention—not a requirement.

Its purpose is simply to make the business contract immediately discoverable. Opening an intent should make it obvious what operation it represents and what outcomes are possible.

---

## Resolver

A resolver implements exactly one business contract.

```kotlin
@Resolve(LoginIntent::class)
class LoginResolver @Inject constructor(
    private val authService: AuthenticationService
) : Resolver<LoginIntent, LoginIntent.Result> {

    override suspend fun resolve(
        intent: LoginIntent
    ): LoginIntent.Result {

        val token = authService.login(
            intent.username,
            intent.password
        )

        return LoginSuccess(token)
    }
}
```

The `@Resolve` annotation and `Resolver<I, R>` interface are validated together.

Axon verifies at compile time that:

- the resolver handles the annotated intent,
- the resolver returns the result declared by that intent,
- every intent has a matching resolver,
- no resolver accidentally implements the wrong business contract.

If any part of the contract becomes inconsistent, compilation fails.

Business operations remain self-documenting and impossible to wire incorrectly.

Resolvers must also be:

- stateless,
- deterministic,
- exception-free.

Expected failures belong in the result type.

Thrown exceptions indicate programming errors and are surfaced as `ResolverException`.

---

## Dependency Injection

Axon includes a compile-time dependency injection system powered by KSP.

```kotlin
class AuthenticationService @Inject constructor(
    private val api: AuthApi,
    private val database: Database
)
```

Features include:

- constructor injection
- interface binding
- lazy singleton creation
- compile-time dependency validation
- circular dependency detection
- zero runtime reflection

---

## Axon

`Axon` dispatches intents to their corresponding resolvers.

```kotlin
val result = axon.dispatch(
    LoginIntent(
        username = "steve",
        password = "secret"
    )
)
```

The result is returned directly.

Business outcomes are represented by the result type.

Only infrastructure failures throw exceptions.

Resolvers are instantiated lazily.

With KSP:

```kotlin
val axon = Axon()
axon.init()
```

Without KSP:

```kotlin
axon.registerResolver(
    LoginIntent::class,
    lazy { LoginResolver(service) }
)
```

---

## Modules

| Module | Purpose |
|---------|---------|
| core | Runtime |
| ksp | Compile-time code generation |

```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version <ksp-version>
}

dependencies {
    implementation("com.singularity-universe.axon:core:1.0.0-alpha2")
    ksp("com.singularity-universe.axon:ksp:1.0.0-alpha2")
}
```

---

## Artifacts

```
com.singularity-universe.axon:core:1.0.0-alpha2
com.singularity-universe.axon:ksp:1.0.0-alpha2
```

---

## FAQ

→ [FAQ.md](FAQ.md)