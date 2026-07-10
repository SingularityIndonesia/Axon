package com.singularity_universe.axon.ext

import com.singularity_universe.axon.Axon
import com.singularity_universe.axon.Intent
import com.singularity_universe.axon.Resolver

/**
 * Registers a [Resolver] using a reified type parameter instead of a [KClass].
 *
 * Shorthand for manual registration:
 * ```
 * axon.registerResolver<LoginIntent, LoginResult>(LoginResolver())
 * ```
 */
inline fun <reified I : Intent<R>, R> Axon.registerResolver(resolver: Resolver<I, R>) {
    registerResolver(I::class, resolver)
}
