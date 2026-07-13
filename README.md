# Axon

> **At its core, every business application should be Intent → Process → Result.**

Axon is the backbone for business applications built around this philosophy.

It places business processes at the center of the application, treating UI, databases, networks, files, and external services as equal I/O participants rather than architectural foundations.

Most application frameworks are naturally UI-centric — tutorials start with screens, examples are organized around navigation, and business logic tends to gravitate toward wherever the framework provides the most convenience. This is a practical consequence of how those tools evolved, not a flaw in the people who use them. Axon simply starts from a different premise: the application is the process, not the interface. UI is one of many I/O channels — a way to deliver an Intent and render a Result, no different in principle from a network request or a scheduled job. When that distinction is clear in the architecture, testing becomes straightforward, platform changes become manageable, and business logic stays where it belongs.

---

## Core Concepts

### Intent
An `Intent` is a pure, immutable declarative object that describes what the user wants to accomplish. It carries only its input data, a `createdAt` timestamp, and an optional `parent` intent — never the result.

The result type is declared as a sealed class nested inside the intent — making all possible outcomes explicit and exhaustively handled at the call site.

```kotlin
sealed class MyAppIntent<out R> : Intent<R>() {

    data class LoginIntent(
        val username: String,
        val password: String
    ) : MyAppIntent<LoginIntent.LoginResult>() {
        sealed class LoginResult {
            data class LoginSuccess(val token: String) : LoginResult()
            data object LoginBlocked : LoginResult()
            data object UnableToProcess : LoginResult()
        }
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

Resolvers **must be stateless**. A resolver is a pure transformation node — like a neuron in a
neural network, it has no memory between invocations. All context needed to process a request
must be carried by the Intent; all context the caller needs afterward must be carried by the
Result. Internal mutable state in a resolver is a design error.

This is not merely a guideline — it is a direct consequence of the Intent → Process → Result
philosophy. State is context. Context belongs on the signal, not in the node.

Resolvers **must never throw exceptions**. All outcomes — including errors — must be expressed through the result type.

```kotlin
@Resolve(LoginIntent::class)
class LoginResolver @Inject constructor(
    private val authService: AuthenticationService
) : Resolver<LoginIntent, LoginResult> {
    override suspend fun resolve(intent: LoginIntent): LoginResult {
        val token = authService.login(intent.username, intent.password)
        return LoginSuccess(token = token)
    }
}
```

### Dependency Injection
Axon includes a built-in compile-time DI system powered by KSP. Annotate constructors with `@Inject` and the processor resolves the full dependency graph automatically — no runtime reflection, no manual wiring.

```kotlin
class LocalDatabase @Inject constructor()

class AuthenticationService @Inject constructor(
    private val api: AuthWebApi,
    private val db: LocalDatabase
)
```

To depend on an interface rather than a concrete class, use `@Bind` on the implementation:

```kotlin
interface DatabaseRepository

@Bind(DatabaseRepository::class)
class LocalDatabase @Inject constructor() : DatabaseRepository

class AuthenticationService @Inject constructor(
    private val db: DatabaseRepository  // LocalDatabase is injected
)
```

Rules:
- Classes with a no-arg constructor are injectable without `@Inject`
- Shared dependencies are instantiated once and reused across all resolvers
- Circular dependencies are detected at **compile time** with a clear error:
  ```
  Circular dependency detected: A → B → A
  ```
- Missing `@Inject` on a class with constructor params is a **compile error**, with full context:
  ```
  AuthWebApi must have an @Inject constructor. Required by: LoginResolver → AuthenticationService → AuthWebApi
  ```

### Axon
`Axon` is the central dispatcher. It is designed to live as a singleton managed by your DI framework — not a Kotlin `object`, so it remains testable and replaceable.

With KSP, the generated `init()` wires the full dependency graph automatically:

```kotlin
val axon = Axon()
axon.init() // generated — resolvers and all their dependencies wired as lazy singletons
```

Without KSP, resolvers can be registered manually:

```kotlin
val axon = Axon()
axon.registerResolver(LoginIntent::class, lazy { LoginResolver(authService) })
```

Resolver instances are created **lazily** — only on the first `dispatch` call for that intent type, not at startup.

A custom `AxonLogger` can be injected to forward fatal resolver errors to your platform's logging infrastructure (e.g. Crashlytics, Timber, Sentry):

```kotlin
val axon = Axon(logger = MyLogger())
```

---

## Dispatching Intents

`dispatch` is a suspending function that returns the result directly. Cancellation is handled organically — cancelling the caller's scope cancels the resolver's coroutine.

Callers must handle the following exceptions:

- `NoHandlerException` — no resolver is registered for this intent type
- `ResolverException` — the resolver violated its contract by throwing an exception (this is always a bug)

```kotlin
when (val result = axon.dispatch(LoginIntent(username = "steve", password = "secret"))) {
    is LoginSuccess      -> println("token: ${result.token}")
    is LoginBlocked      -> println("account blocked")
    is UnableToProcess   -> println("try again later")
}
```

---

## Modules

| Module | Description |
|--------|-------------|
| `core` | `Intent`, `Resolver`, `@Resolve`, `@Inject`, `Axon` — the runtime |
| `ksp`  | KSP annotation processor — resolves dependency graph, generates `Axon.init()` |

## Artifacts
```
com.singularity-universe.axon:core:1.0.0
com.singularity-universe.axon:ksp:1.0.0
```

## License
[Apache 2.0](LICENSE)

---

## FAQ
Common questions about Axon's design philosophy, scope, and trade-offs — including why Axon
is not a DI library, why factory-scoped resolvers are not supported, and how session state
and memory management are handled.

→ [Read the FAQ](FAQ.md)
