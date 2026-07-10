package com.singularity_universe.axon.exception

import com.singularity_universe.axon.Intent

/**
 * Thrown by [com.singularity_universe.axon.Axon] when a [com.singularity_universe.axon.Resolver]
 * violates its contract by throwing an exception.
 *
 * ## This should never happen.
 *
 * Resolvers must never throw exceptions. All outcomes — including errors — must be expressed
 * through the intent's result type. A resolver that throws is a bug, not a handled condition.
 *
 * This exception is not the original exception thrown by the resolver. Axon intercepts the
 * original, logs it as a fatal violation, and re-throws this controlled exception so that
 * callers receive a typed, Axon-owned signal rather than an arbitrary internal exception
 * bleeding across layer boundaries.
 *
 * @param intent the intent that was being processed when the violation occurred.
 * @param cause the original exception thrown by the resolver.
 */
class ResolverException(
    val intent: Intent<*>,
    cause: Throwable
) : Exception(
    "Resolver for ${intent::class.simpleName} threw an unexpected exception. " +
    "Resolvers must not throw — express errors through the result type instead.",
    cause
)
