package com.singularity_universe.axon.exception

import com.singularity_universe.axon.Intent
import kotlin.reflect.KClass

/**
 * Thrown when a [com.singularity_universe.axon.Resolver] is registered for an [Intent] type
 * that already has a registered resolver.
 *
 * @param intentClass the [Intent] type that already has a resolver registered.
 */
class DuplicateResolverException(intentClass: KClass<*>) : Exception(
    "A resolver for ${intentClass.simpleName} is already registered. Each Intent type may only have one resolver."
)
