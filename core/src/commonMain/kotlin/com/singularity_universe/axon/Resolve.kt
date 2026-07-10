package com.singularity_universe.axon

import kotlin.reflect.KClass

/**
 * Marks a class as the resolver for a specific [Intent] type.
 *
 * A class annotated with [Resolve] must implement [Resolver] with the matching intent and result types:
 * ```
 * @Resolve(LoginIntent::class)
 * class LoginResolver : Resolver<LoginIntent, LoginResult> {
 *     override suspend fun resolve(intent: LoginIntent): LoginResult {
 *         return LoginResult(token = "jwt.token.abc123")
 *     }
 * }
 * ```
 *
 * This annotation is used by the KSP annotation processor to auto-generate resolver registration
 * with [Axon]. Without KSP, resolvers must be registered manually via [Axon.registerResolver].
 *
 * @param intentClass the [Intent] subclass this resolver handles.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Resolve(val intentClass: KClass<out Intent<*>>)
