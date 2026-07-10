package com.singularity_universe.axon

import com.singularity_universe.axon.exception.DuplicateResolverException
import com.singularity_universe.axon.exception.NoHandlerException
import com.singularity_universe.axon.exception.ResolverException
import com.singularity_universe.axon.untils.Log
import kotlin.reflect.KClass

/**
 * The central flow controller of the Axon framework.
 *
 * [Axon] routes incoming [Intent]s to their registered [Handler]s and returns the result.
 *
 * Handlers are registered via [registerResolver]:
 * ```
 * val axon = Axon()
 * axon.registerResolver(LoginIntent::class, LoginResolver())
 * ```
 *
 * With the KSP annotation processor, registration is generated automatically from [@Resolver][Resolver].
 *
 * Intents are dispatched via [dispatch]:
 * ```
 * val result = axon.dispatch(LoginIntent(username, password))
 * ```
 *
 * @param logger the [AxonLogger] used to report fatal resolver errors. Defaults to [AxonLogger.Default].
 */
class Axon(private val logger: AxonLogger = AxonLogger.Default) {

    private val handlers = mutableMapOf<KClass<*>, Handler<*, *>>()

    /**
     * Registers a [Handler] for the given [intentClass].
     *
     * Each [Intent] type may only have one handler. Registering a second handler
     * for the same type throws [DuplicateResolverException] to prevent silent overwrites.
     *
     * ```
     * axon.registerResolver(LoginIntent::class, LoginResolver())
     * ```
     *
     * @param intentClass the [KClass] of the [Intent] this handler processes.
     * @param handler the [Handler] instance that processes the intent.
     * @throws DuplicateResolverException if a handler for [intentClass] is already registered.
     */
    fun <I : Intent<R>, R> registerResolver(intentClass: KClass<I>, handler: Handler<I, R>) {
        if (handlers.containsKey(intentClass)) throw DuplicateResolverException(intentClass)
        handlers[intentClass] = handler
    }

    /**
     * Dispatches the given [intent] to its registered [Handler] and returns the result.
     *
     * **Callers must handle the following exceptions:**
     *
     * @param intent the intent to dispatch.
     * @return the result produced by the handler.
     * @throws NoHandlerException if no handler is registered for this intent type.
     * @throws ResolverException if the handler violates its contract by throwing an exception.
     * This indicates a bug in the resolver — resolvers must never throw. A hardcoded fatal log
     * is always emitted before this exception reaches the caller.
     */
    suspend fun <R> dispatch(intent: Intent<R>): R {
        @Suppress("UNCHECKED_CAST")
        val handler = handlers[intent::class] as? Handler<Intent<R>, R>
            ?: throw NoHandlerException(intent)

        return runCatching {
            handler.resolve(intent)
        }.getOrElse { exception ->
            // Always re-throw CancellationException — it must never be intercepted.
            // Swallowing it would silently break coroutine cancellation.
            if (exception is kotlinx.coroutines.CancellationException) throw exception

            // This log is hardcoded and cannot be excluded or filtered.
            // A resolver that throws is always a fatal contract violation.
            Log.fatalError(
                "Axon",
                "${intent::class.simpleName} resolver threw an exception. This must never happen. Cause: ${exception.message}"
            )
            logger.onResolverException(intent, exception)
            throw ResolverException(intent, exception)
        }
    }
}
