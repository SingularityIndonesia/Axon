package com.singularity_universe.axon

/**
 * The processing contract for a specific [Intent] type.
 *
 * A [Handler] receives an intent and returns a result directly. It is always used together
 * with the [@Resolver][Resolver] annotation, which binds the handler to its target intent class
 * and enables auto-registration via the KSP annotation processor.
 *
 * ```
 * @Resolver(LoginIntent::class)
 * class LoginResolver : Handler<LoginIntent, LoginResult> {
 *     override suspend fun resolve(intent: LoginIntent): LoginResult {
 *         return LoginResult(token = "jwt.token.abc123")
 *     }
 * }
 * ```
 *
 * @param I the specific [Intent] type this handler processes.
 * @param R the result type returned after processing.
 */
interface Handler<I : Intent<R>, R> {

    /**
     * Processes the given [intent] and returns the result.
     *
     * Must not throw — express all error states through [R] instead.
     * Throwing from a handler is a fatal contract violation; [Axon] will catch it,
     * emit a fatal log, and re-throw it as a [com.singularity_universe.axon.exception.ResolverException].
     *
     * Cancellation is handled automatically — if the caller's scope is cancelled,
     * this coroutine is cancelled as well.
     *
     * @param intent the intent to process.
     * @return the result of processing the intent.
     */
    suspend fun resolve(intent: I): R
}
