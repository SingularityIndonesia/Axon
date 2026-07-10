package com.singularity_universe.axon

import kotlin.reflect.KClass

/**
 * Marks a class as the resolver for a specific [Intent] type.
 *
 * A class annotated with [Resolver] must declare a suspending function with the signature:
 * ```
 * suspend fun resolve(intent: I): R
 * ```
 * where `I` is the intent class specified in [intentClass], and `R` is the corresponding result type.
 *
 * This annotation is used by the KSP annotation processor to auto-generate resolver registration
 * with [Axon]. Without KSP, resolvers must be registered manually via [Axon.registerResolver].
 *
 * Example:
 * ```
 * @Resolver(LoginIntent::class)
 * class LoginResolver {
 *     suspend fun resolve(intent: LoginIntent): LoginIntent.LoginResult {
 *         return LoginIntent.LoginResult(token = "jwt.token.abc123")
 *     }
 * }
 * ```
 *
 * @param intentClass the [Intent] subclass this resolver handles.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Resolver(val intentClass: KClass<out Intent<*>>)
