package com.singularity_universe.axon.exception

import com.singularity_universe.axon.Intent

/**
 * Thrown when [com.singularity_universe.axon.Axon.proceed] is called with an [com.singularity_universe.axon.Intent] that has no registered [com.singularity_universe.axon.Resolver].
 *
 * @param intent the unhandled intent.
 */
class NoHandlerException(val intent: Intent<*>) : Exception(
    "No resolver registered for ${intent::class.simpleName}"
)
