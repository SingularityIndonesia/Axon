# Axon

> **At its core, every business application should be Intent → Process → Result.**

Axon is the backbone for business applications built around this philosophy.

It places business processes at the center of the application, treating UI, databases, networks, files, and external services as equal I/O participants rather than architectural foundations.

---

## Core Concepts

### Intent
An `Intent` is a pure, immutable declarative object that describes what the user wants to accomplish. It carries only its input data, a `createdAt` timestamp, and an optional `parent` intent — never the result.

Intents are best organized as a sealed class hierarchy per domain:

```kotlin
sealed class MyAppIntent<out R> : Intent<R>() {

    data class LoginIntent(
        val username: String,
        val password: String
    ) : MyAppIntent<LoginIntent.LoginResult>() {
        data class LoginResult(val token: String)
    }

    data class LogoutIntent(
        val userId: String
    ) : MyAppIntent<LogoutIntent.LogoutResult>() {
        data class LogoutResult(val success: Boolean)
    }
}
```

### Resolver
A `Resolver` handles a specific `Intent` type and returns a result directly. It is always paired with the `@Resolve` annotation, which declares which intent it handles and enables auto-registration via the KSP annotation processor.

Resolvers **must never throw exceptions**. All outcomes — including errors — must be expressed through the result type.

```kotlin
@Resolve(LoginIntent::class)
class LoginResolver : Resolver<LoginIntent, LoginResult> {
    override suspend fun resolve(intent: LoginIntent): LoginResult {
        return LoginResult(token = "jwt.token.abc123")
    }
}
```

### Axon
`Axon` is the central flow controller. Resolvers are registered to it, and intents are dispatched through it. It is designed to be used as a singleton via dependency injection.

```kotlin
val axon = Axon()
axon.registerResolver(LoginIntent::class, LoginResolver())
```

A custom `AxonLogger` can be injected to forward fatal resolver errors to your platform's logging infrastructure (e.g. Crashlytics, Timber, Sentry):

```kotlin
val axon = Axon(logger = MyLogger())
```

---

## Dispatching Intents

`dispatch` is a suspending function that returns the result directly. Cancellation is handled organically — cancelling the caller's scope cancels the resolver's coroutine.

Callers must handle the following exceptions:

- `NoHandlerException` — no resolver is registered for this intent type
- `ResolverException` — the resolver violated its contract by throwing an exception (this is a bug and should never happen)

```kotlin
try {
    val result = axon.dispatch(LoginIntent(username = "steve", password = "secret"))
    println(result.token)
} catch (e: NoHandlerException) {
    // no resolver registered
} catch (e: ResolverException) {
    // resolver bug — hide loading, report to logger
}
```

---

## Artifact
```
com.singularity_universe.axon:core:1.0.0
```

## License
[Apache 2.0](LICENSE)
