package com.singularity_universe.axon

import kotlinx.datetime.Clock

/**
 * Represents a user intention within the Axon framework.
 *
 * Every business operation begins as an [Intent] — a declarative object describing
 * what the user wants to accomplish. Intents are processed by a Process, which
 * produces a [result] and returns it back through the intent.
 *
 * Subclass this to define domain-specific intents:
 * ```
 * data class LoginIntent(
 *     val username: String,
 *     val password: String
 * ) : Intent<LoginResult>()
 * ```
 *
 * @param R covariant type of the result produced after this intent is processed.
 */
abstract class Intent<out R> {

    /**
     * The timestamp (epoch milliseconds) at which this intent was created.
     */
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()

    /**
     * The result produced after this intent has been processed.
     * Null if the intent has not yet been processed.
     */
    open val result: R? = null

    /**
     * The parent intent that spawned this intent, or null if this is a top-level intent.
     *
     * A child intent is aware that a parent exists but does not know its specific type.
     */
    open val parent: Intent<*>? = null
}
