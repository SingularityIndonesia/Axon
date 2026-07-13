# FAQ

## Is Axon a dependency injection library?

No.

Dependency injection is a supporting feature, not Axon's purpose.

Axon is designed around a simple principle:

> **A business application is a collection of business operations. Each operation naturally follows Intent → Process → Result.**

Every operation forms a compile-time contract consisting of:

```
Intent
   │
   ▼
Resolver
   │
   ▼
Result
```

The KSP processor validates that these pieces always agree. Dependency injection simply exists to construct the object graph required to execute those contracts.

Features such as constructor injection, lazy initialization, interface bindings, and compile-time validation exist because they make business operations easier to implement—not because dependency injection is Axon's primary concern.

---

## Why does Axon validate Intent, Resolver, and Result together?

Business operations should be impossible to wire incorrectly.

An `Intent` declares the type of `Result` it produces.

A `Resolver` declares both the `Intent` it handles and the `Result` it returns.

The `@Resolve` annotation declares which intent the resolver implements.

These three declarations describe the same business contract from different perspectives. Axon validates that they remain consistent during compilation.

For example, this is valid:

```kotlin
class LoginIntent(...) : Intent<LoginResult>()

@Resolve(LoginIntent::class)
class LoginResolver : Resolver<LoginIntent, LoginResult>
```

Changing any part of that contract without updating the others causes a compile-time error.

Rather than relying on runtime testing or convention, Axon ensures the structure of every business operation is correct before the application runs.

---

## Why does Intent have a `parent`?

Every intent carries an optional reference to the intent that spawned it.

The idea is that a business application is not a flat collection of isolated operations — it is a living sequence of causally connected actions. When one operation triggers another, the second is a consequence of the first. The `parent` reference makes that relationship explicit and permanent.

```
AppStartIntent (parent = null)
    └─ GoToDashboardIntent (parent = AppStartIntent)
        └─ GoToProductListIntent (parent = GoToDashboardIntent)
            └─ AddToCartIntent (parent = GoToProductListIntent)
```

Walking the chain backward from any intent tells the complete story of how the system arrived at that operation.

`parent = null` is always valid. Intents triggered from outside any running operation — push notifications, system events, application entry points — are natural roots with no prior cause.

The deeper vision is that the application itself starts as an intent. The UI, background work, and side effects all run within intent scopes. A dashboard does not simply open — it opens *because* of an intent, and everything that happens while it is open happens *within* that intent's scope. This makes the full history of any system state reconstructable from the chain.

> Automatic parent propagation — where Axon injects `parent` from the current dispatch context without the caller setting it manually — is a planned feature.

---

## Why is it not recommended to declare an Intent as a `data class`?

This is a philosophical consequence of what an intent represents.

A `data class` models a **value**. Two instances with the same fields are considered equal and interchangeable. Kotlin generates structural `equals()`, `hashCode()`, and `copy()` accordingly.

But an intent is not a value. It is a **command** — a unique, irreversible act of will directed at the system. Two login attempts with the same credentials are two distinct operations, not the same operation expressed twice. Treating them as equal is semantically wrong.

The more concrete problem is `copy()`. Calling `copy()` on an intent produces a new instance with different identity. If the original intent had a parent, the copy does not — the history chain is silently severed.

```kotlin
val original = LoginIntent(data = loginData, parent = previousIntent)
val copy = original.copy(data = loginData) // parent is gone
```

Axon's intent model carries a `parent` reference precisely to preserve the chain of operations that led to the current one. A `data class` undermines this by making it easy — and syntactically normal — to produce orphaned intents.

For this reason, Axon's KSP processor emits a compile-time warning whenever a `data class` is used as an intent type in a `@Resolve` annotation.

Declare intents as regular classes:

```kotlin
class LoginIntent(
    val data: LoginData,
    parent: Intent<*>? = null
) : Intent<LoginIntent.Result>(parent)
```

---

## Why are nested result classes recommended?

They are not required.

Axon only requires that an intent declares the type of result it produces.

However, placing the result beside its intent makes the business contract immediately discoverable. Opening an intent shows both:

- what the operation requests, and
- every outcome it may produce.

For simple operations, a standalone result class may be perfectly appropriate.

For operations with multiple outcomes, a nested sealed class often communicates the contract more clearly.

The goal is not a particular class structure—the goal is making the business operation obvious to someone reading the code.

---

## Why must resolvers be stateless?

A resolver represents the processing step of a business operation.

Its responsibility is simply to transform an `Intent` into a `Result`.

Any information required to perform the work belongs in the `Intent`.

Any information the caller needs afterward belongs in the `Result`.

Keeping resolvers stateless makes them deterministic, reusable, naturally thread-safe, and easy to test.

If a resolver requires mutable internal state, that state usually belongs elsewhere in the application rather than inside the processing layer.

---

## Why don't resolvers throw exceptions?

Business failures are business outcomes.

Invalid credentials, insufficient permissions, unavailable inventory, or validation failures are expected possibilities and should be represented explicitly by the result type.

Exceptions represent programming or infrastructure failures that violate the resolver contract.

For that reason, Axon distinguishes between:

- **Business outcomes** → returned as `Result`
- **Programming errors** → surfaced as `ResolverException`

This keeps business logic explicit while ensuring genuine bugs are never silently treated as normal outcomes.

---

## Why doesn't Axon support factory-scoped resolvers?

Factory lifecycles exist to manage stateful objects.

Resolvers are intentionally stateless.

Since a resolver contains only processing logic, creating and destroying resolver instances provides little practical benefit while introducing additional lifecycle complexity.

Instead, resolver instances are created lazily the first time they are needed and reused afterward.

If a resolver appears to require factory semantics, it is often a sign that mutable state has found its way into the processing layer.

---

## Why doesn't Axon have session scope?

Because a session is context—not object lifetime.

Information such as user identity, authentication, filters, locale, or workflow state belongs either:

- in the `Intent`, or
- in data retained by the caller between dispatches.

Axon intentionally separates business processing from application state management.

Resolvers process requests.

Callers own state.

Keeping those responsibilities separate produces simpler and more predictable systems.

---

## Isn't keeping every resolver alive wasteful?

Resolvers are lightweight objects.

They contain processing logic and references to their dependencies.

They do not maintain per-request state, worker threads, caches, or network connections simply by existing.

Creating and destroying such objects repeatedly generally costs more than keeping them available.

Expensive resources should instead be managed explicitly by the components that own those resources—not by the resolver lifecycle.

---

## What about memory pressure on mobile devices?

Modern operating systems already manage memory more effectively than application-level object lifecycles.

When memory becomes scarce, the operating system can reclaim unused pages or terminate background processes.

Axon therefore avoids introducing additional object lifecycle management solely for memory optimization.

Resolvers are created lazily and reused.

Memory management remains the operating system's responsibility.

---

## What happens if multiple screens dispatch the same Intent simultaneously?

Each dispatch is completely independent.

`dispatch` is a suspending function that returns its result directly to the calling coroutine.

Multiple callers can dispatch the same intent concurrently through the same stateless resolver without interfering with one another.

This is similar to multiple HTTP requests being handled simultaneously by the same server endpoint.

Cancellation is equally straightforward.

Cancelling the caller's coroutine automatically cancels the corresponding dispatch operation.