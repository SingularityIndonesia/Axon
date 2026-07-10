package com.singularity_universe.axon

import com.singularity_universe.axon.exception.DuplicateResolverException
import com.singularity_universe.axon.exception.NoHandlerException
import com.singularity_universe.axon.exception.ResolverException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KClass

/**
 * The central flow controller of the Axon framework.
 *
 * [Axon] routes incoming [Intent]s to their registered [Resolver]s and streams
 * resolved states back to the caller as a [Flow].
 *
 * Resolvers are registered via [registerResolver]:
 * ```
 * val axon = Axon()
 * axon.registerResolver(LoginIntent::class, LoginResolver())
 * ```
 *
 * Intents are dispatched via [dispatch]:
 * ```
 * axon.dispatch(LoginIntent(username, password))
 *     .catch { e -> if (e is NoHandlerException) { ... } }
 *     .collect { intent -> ... }  // optional — fire-and-forget intents may not need a collector
 * ```
 *
 * @param logger the [AxonLogger] used to report fatal resolver errors. Defaults to [AxonLogger.Default].
 */
class Axon(private val logger: AxonLogger = AxonLogger.Default) {

    private val resolvers = mutableMapOf<KClass<*>, Resolver<*, *>>()

    /**
     * Registers a [Resolver] for the given [intentClass].
     *
     * Each [Intent] type may only have one resolver. Registering a second resolver
     * for the same type throws [DuplicateResolverException] to prevent silent overwrites.
     *
     * @param intentClass the [KClass] of the [Intent] this resolver handles.
     * @param resolver the resolver to register.
     * @throws DuplicateResolverException if a resolver for [intentClass] is already registered.
     */
    fun <I : Intent<R>, R> registerResolver(intentClass: KClass<I>, resolver: Resolver<I, R>) {
        if (resolvers.containsKey(intentClass)) throw DuplicateResolverException(intentClass)
        resolvers[intentClass] = resolver
    }

    /**
     * Dispatches the given [intent] to its registered [Resolver] and returns a [Flow]
     * of resolved [Intent] states.
     *
     * The [Flow] emits each time the [Resolver] calls [emit][Resolver.resolve], allowing
     * intermediate feedback before the final result.
     *
     * Cancellation is handled organically — cancelling the collector's scope will cancel
     * the resolver's coroutine.
     *
     * **Callers must always attach a `.catch {}` to handle the following exceptions:**
     *
     * @param intent the intent to dispatch.
     * @throws NoHandlerException if no resolver is registered for this intent type.
     * @throws ResolverException if the resolver violates its contract by throwing an exception.
     * This indicates a bug in the resolver — resolvers must never throw. A hardcoded fatal log
     * is always emitted before this exception reaches the caller.
     */
    fun <R> dispatch(intent: Intent<R>): Flow<Intent<R>> = flow {
        @Suppress("UNCHECKED_CAST")
        val resolver = resolvers[intent::class] as? Resolver<Intent<R>, R>
            ?: throw NoHandlerException(intent)

        runCatching {
            resolver.resolve(intent) { emit(it) }
        }.onFailure { exception ->
            // Always re-throw CancellationException — it must never be intercepted.
            // Swallowing it would silently break coroutine cancellation.
            if (exception is kotlinx.coroutines.CancellationException) throw exception

            // This log is hardcoded and cannot be excluded or filtered.
            // A resolver that throws is always a fatal contract violation.
            println("\u001B[31m[Axon] *** FATAL *** ${intent::class.simpleName} resolver threw an exception. This must never happen. Cause: ${exception.message}\u001B[0m")
            logger.onResolverException(intent, exception)
            throw ResolverException(intent, exception)
        }
    }
}
