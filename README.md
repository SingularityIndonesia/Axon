# Axon

> **At its core, every business application should be Intent → Process → Result.**

Axon is the backbone for business applications built around this philosophy.

It places business processes at the center of the application, treating UI, databases, networks, files, and external services as equal I/O participants rather than architectural foundations.

---

## Core Concepts

### Intent
An `Intent` is a declarative object that carries a user's intention. It knows its result (once processed) and its parent intent (if spawned by another intent), but nothing about how it will be handled.

```kotlin
data class LoginIntent(
    val username: String,
    val password: String,
    override val result: LoginResult? = null
) : Intent<LoginResult>()
```

### Resolver
A `Resolver` handles a specific `Intent` type. It receives the intent and emits one or more resolved states back to the caller — enabling intermediate feedback before the final result.

```kotlin
class LoginResolver : Resolver<LoginIntent, LoginResult> {
    override suspend fun resolve(intent: LoginIntent, emit: suspend (Intent<LoginResult>) -> Unit) {
        emit(intent.copy(result = LoginResult(token = "authenticating...")))
        emit(intent.copy(result = LoginResult(token = "jwt.token.abc123")))
    }
}
```

### Axon
`Axon` is the central flow controller. Resolvers are registered to it, and intents are dispatched through it. It is designed to be used as a singleton via dependency injection.

```kotlin
val axon = Axon()
axon.registerResolver(LoginIntent::class, LoginResolver())
```

---

## Dispatching Intents

`dispatch` returns a `Flow<Intent<R>>`. The flow emits each time the resolver calls `emit`, and is automatically cancelled when the collector's scope is cancelled.

```kotlin
axon.dispatch(LoginIntent(username = "steve", password = "secret"))
    .catch { e ->
        if (e is NoHandlerException) { /* no resolver registered */ }
    }
    .collect { intent ->
        println(intent.result)
    }
```

Not all intents need a collector — fire-and-forget intents can be dispatched without `collect`.

---

## Group
`com.singularity_universe`

## License
[Apache 2.0](LICENSE)
