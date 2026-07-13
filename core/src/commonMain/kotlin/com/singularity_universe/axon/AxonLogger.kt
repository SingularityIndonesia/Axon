package com.singularity_universe.axon

import com.singularity_universe.axon.utils.Log

/**
 * Logger interface for [Axon] internal events.
 *
 * ## Why this exists
 *
 * Axon enforces a strict architectural rule: **exceptions must not cross layer boundaries.**
 * A [Resolver] operates at the domain/process layer and must express all outcomes — including
 * errors — through the intent's result type. Throwing an exception from a [Resolver] is a
 * contract violation that breaks this rule, as the exception would propagate into the caller's
 * layer which has no business knowing about internal resolver failures.
 *
 * However, Kotlin provides no compile-time mechanism to enforce that a function will never throw.
 * There is no equivalent of Java's checked exceptions. This means a developer *can* — whether
 * intentionally or by mistake — throw an exception inside a [Resolver], and there is no way to
 * prevent this at the language level.
 *
 * To protect the application from crashing at runtime due to such violations, [Axon] wraps
 * all [Resolver] executions in a `runCatching` block. If an exception escapes the resolver,
 * it is caught by Axon and **never forwarded to the caller**. Instead, it is reported here as
 * a fatal internal error.
 *
 * When a resolver violation occurs, [Axon] always emits a hardcoded fatal log that cannot
 * be excluded, filtered, or overridden — regardless of the [AxonLogger] implementation.
 * [AxonLogger] is then called as an additional hook, allowing the application to forward
 * the violation to its own logging infrastructure (e.g. Crashlytics, Timber, Sentry).
 * After both logs, [Axon] throws a [com.singularity_universe.axon.exception.ResolverException]
 * so the caller is explicitly notified rather than left in an unknown state.
 *
 * Inject a custom implementation into [Axon] to integrate with your platform's logging system.
 */
interface AxonLogger {

    /**
     * Called when a [Resolver] throws an unexpected exception.
     *
     * This indicates a contract violation in the resolver implementation — resolvers must not
     * throw exceptions. Errors should be expressed through the intent's result type instead.
     *
     * @param intent the intent that was being processed when the exception occurred.
     * @param exception the exception thrown by the resolver.
     */
    fun onResolverException(intent: Intent<*>, exception: Throwable)

    companion object {
        /**
         * Default logger that prints to standard output.
         */
        val Default: AxonLogger = object : AxonLogger {
            override fun onResolverException(intent: Intent<*>, exception: Throwable) {
                Log.fatalError(
                    "Axon",
                    "${intent::class.simpleName} resolver threw an exception: ${exception.message}"
                )
            }
        }
    }
}
