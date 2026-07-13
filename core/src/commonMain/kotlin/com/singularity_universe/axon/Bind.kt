package com.singularity_universe.axon

import kotlin.reflect.KClass

/**
 * Binds a concrete class to an interface for dependency injection.
 *
 * When the Axon KSP processor encounters a constructor parameter of type [to],
 * it injects this class instead. This allows resolvers and services to depend on
 * abstractions rather than concrete implementations.
 *
 * ```
 * interface DatabaseRepository
 *
 * @Bind(DatabaseRepository::class)
 * class LocalDatabase @Inject constructor() : DatabaseRepository
 *
 * class AuthService @Inject constructor(
 *     private val db: DatabaseRepository  // LocalDatabase is injected here
 * )
 * ```
 *
 * Only one binding per interface is allowed. The annotated class must satisfy
 * the normal [@Inject][Inject] rules — it must have an [@Inject][Inject] constructor
 * or a no-arg constructor.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Bind(val to: KClass<*>)
