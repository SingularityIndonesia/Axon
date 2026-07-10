package com.singularity_universe.axon

/**
 * Handles the processing of a specific [Intent] type.
 *
 * A [Resolver] receives an intent and emits one or more resolved states back to the caller
 * by invoking [emit]. The final emit should carry the completed [result][Intent.resolve].
 *
 * @param I the specific [Intent] type this resolver handles.
 * @param R the result type produced by processing the intent.
 */
interface Resolver<I : Intent<R>, R> {

    /**
     * Processes the given [intent] and emits resolved states via [emit].
     *
     * May be called multiple times to provide intermediate feedback before the final result.
     * Cancellation is handled automatically — if the caller detaches, this coroutine is cancelled.
     *
     * @param intent the intent to process.
     * @param emit suspending function to emit an [Intent] state back to the caller.
     */
    suspend fun resolve(intent: I, emit: suspend (Intent<R>) -> Unit)
}
