package com.singularity_universe.axon

/**
 * Marks a constructor as injectable by the Axon KSP processor.
 *
 * A class with an [@Inject][Inject]-annotated constructor will have its dependencies
 * resolved and wired automatically at build time — no runtime reflection required.
 *
 * Each class may only have one [@Inject][Inject] constructor.
 *
 * ```
 * class AuthRepository @Inject constructor(
 *     private val db: Database
 * )
 * ```
 *
 * Dependencies are instantiated as **lazy singletons** within the scope of [Axon.init] —
 * a shared dependency used by multiple resolvers is created only once.
 */
@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject
