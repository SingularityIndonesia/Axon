package com.singularity_universe.axon

import kotlinx.datetime.Clock

/**
 * Represents a user intention within the Axon framework.
 *
 * Every business operation begins as an [Intent] — a declarative object describing
 * what the user wants to accomplish. The type parameter [R] contracts the result type
 * that this intent will produce when processed, without carrying the result value itself.
 *
 * The intent is immutable and its identity is preserved throughout its lifetime.
 * Results are delivered separately via [Resolved].
 *
 * @param R covariant type of the result this intent is expected to produce.
 */
abstract class Intent<out R> {

    /**
     * The timestamp (epoch milliseconds) at which this intent was created.
     */
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()

    /**
     * The parent intent that spawned this intent, or null if this is a top-level intent.
     *
     * A child intent is aware that a parent exists but does not know its specific type.
     */
    open val parent: Intent<*>? = null
}
