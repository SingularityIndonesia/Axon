package com.singularity_universe.axon.ksp

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class AxonSymbolProcessorTest {

    // --- helpers ---

    private fun compile(vararg sources: SourceFile): JvmCompilationResult {
        return KotlinCompilation().apply {
            this.sources = sources.toList()
            configureKsp {
                symbolProcessorProviders += AxonSymbolProcessorProvider()
            }
            inheritClassPath = true
        }.compile()
    }

    private fun JvmCompilationResult.generatedAxonRegistration(): String? =
        sourcesGeneratedBySymbolProcessor
            .firstOrNull { it.name == "AxonRegistration.kt" }
            ?.readText()

    // --- intent / resolver source stubs ---

    private val simpleIntent = SourceFile.kotlin(
        "SimpleIntent.kt", """
        import com.singularity_universe.axon.Intent
        class SimpleIntent : Intent<String>()
        """.trimIndent()
    )

    // --- tests ---

    @Test
    fun `no resolver classes produces no generated file`() {
        val result = compile(SourceFile.kotlin("Empty.kt", "class Empty"))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `no-arg resolver generates lazy val and registerResolver`() {
        val result = compile(
            simpleIntent,
            SourceFile.kotlin("SimpleResolver.kt", """
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(SimpleIntent::class)
                class SimpleResolver : Resolver<SimpleIntent, String> {
                    override suspend fun resolve(intent: SimpleIntent): String = "ok"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.generatedAxonRegistration()
        assertNotNull(generated, "AxonRegistration.kt should be generated")
        assertTrue(generated.contains("fun Axon.init()"))
        assertTrue(generated.contains("val simpleResolver = lazy { SimpleResolver() }"))
        assertTrue(generated.contains("registerResolver(SimpleIntent::class, simpleResolver)"))
    }

    @Test
    fun `resolver with injected dependency generates full lazy chain`() {
        val result = compile(
            simpleIntent,
            SourceFile.kotlin("AuthService.kt", """
                import com.singularity_universe.axon.Inject
                class AuthService @Inject constructor()
            """.trimIndent()),
            SourceFile.kotlin("SimpleResolver.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(SimpleIntent::class)
                class SimpleResolver @Inject constructor(
                    private val auth: AuthService
                ) : Resolver<SimpleIntent, String> {
                    override suspend fun resolve(intent: SimpleIntent): String = "ok"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.generatedAxonRegistration()
        assertNotNull(generated)
        assertTrue(generated.contains("val authService = lazy { AuthService() }"))
        assertTrue(generated.contains("val simpleResolver = lazy { SimpleResolver(authService.value) }"))
        assertTrue(generated.contains("registerResolver(SimpleIntent::class, simpleResolver)"))
    }

    @Test
    fun `shared dependency appears as a single lazy val`() {
        val result = compile(
            SourceFile.kotlin("Intents.kt", """
                import com.singularity_universe.axon.Intent
                class IntentA : Intent<String>()
                class IntentB : Intent<String>()
            """.trimIndent()),
            SourceFile.kotlin("SharedService.kt", """
                import com.singularity_universe.axon.Inject
                class SharedService @Inject constructor()
            """.trimIndent()),
            SourceFile.kotlin("ResolverA.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(IntentA::class)
                class ResolverA @Inject constructor(val s: SharedService) : Resolver<IntentA, String> {
                    override suspend fun resolve(intent: IntentA): String = "a"
                }
            """.trimIndent()),
            SourceFile.kotlin("ResolverB.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(IntentB::class)
                class ResolverB @Inject constructor(val s: SharedService) : Resolver<IntentB, String> {
                    override suspend fun resolve(intent: IntentB): String = "b"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.generatedAxonRegistration()!!
        val occurrences = "val sharedService = lazy".toRegex().findAll(generated).count()
        assertEquals(1, occurrences, "SharedService should be declared exactly once")
    }

    @Test
    fun `no-arg class without @Inject is injectable`() {
        val result = compile(
            simpleIntent,
            SourceFile.kotlin("NoArgService.kt", "class NoArgService"),
            SourceFile.kotlin("SimpleResolver.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(SimpleIntent::class)
                class SimpleResolver @Inject constructor(
                    private val service: NoArgService
                ) : Resolver<SimpleIntent, String> {
                    override suspend fun resolve(intent: SimpleIntent): String = "ok"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.generatedAxonRegistration()
        assertNotNull(generated)
        assertTrue(generated.contains("val noArgService = lazy { NoArgService() }"))
    }

    @Test
    fun `topological order — dependency declared before dependent`() {
        val result = compile(
            simpleIntent,
            SourceFile.kotlin("Level1.kt", """
                import com.singularity_universe.axon.Inject
                class Level1 @Inject constructor()
            """.trimIndent()),
            SourceFile.kotlin("Level2.kt", """
                import com.singularity_universe.axon.Inject
                class Level2 @Inject constructor(val l1: Level1)
            """.trimIndent()),
            SourceFile.kotlin("Level3.kt", """
                import com.singularity_universe.axon.Inject
                class Level3 @Inject constructor(val l2: Level2)
            """.trimIndent()),
            SourceFile.kotlin("SimpleResolver.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(SimpleIntent::class)
                class SimpleResolver @Inject constructor(val l3: Level3) : Resolver<SimpleIntent, String> {
                    override suspend fun resolve(intent: SimpleIntent): String = "ok"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.generatedAxonRegistration()!!
        val l1Pos = generated.indexOf("val level1 =")
        val l2Pos = generated.indexOf("val level2 =")
        val l3Pos = generated.indexOf("val level3 =")
        val resolverPos = generated.indexOf("val simpleResolver =")
        assertTrue(l1Pos < l2Pos, "Level1 must be declared before Level2")
        assertTrue(l2Pos < l3Pos, "Level2 must be declared before Level3")
        assertTrue(l3Pos < resolverPos, "Level3 must be declared before SimpleResolver")
    }

    @Test
    fun `multiple resolvers are all registered`() {
        val result = compile(
            SourceFile.kotlin("Intents.kt", """
                import com.singularity_universe.axon.Intent
                class IntentA : Intent<String>()
                class IntentB : Intent<String>()
                class IntentC : Intent<String>()
            """.trimIndent()),
            SourceFile.kotlin("Resolvers.kt", """
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(IntentA::class)
                class ResolverA : Resolver<IntentA, String> {
                    override suspend fun resolve(intent: IntentA): String = "a"
                }
                @Resolve(IntentB::class)
                class ResolverB : Resolver<IntentB, String> {
                    override suspend fun resolve(intent: IntentB): String = "b"
                }
                @Resolve(IntentC::class)
                class ResolverC : Resolver<IntentC, String> {
                    override suspend fun resolve(intent: IntentC): String = "c"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.generatedAxonRegistration()!!
        assertTrue(generated.contains("registerResolver(IntentA::class, resolverA)"))
        assertTrue(generated.contains("registerResolver(IntentB::class, resolverB)"))
        assertTrue(generated.contains("registerResolver(IntentC::class, resolverC)"))
    }

    @Test
    fun `missing @Inject on class with constructor params emits compile error`() {
        val result = compile(
            simpleIntent,
            SourceFile.kotlin("BadService.kt", """
                class BadService(val x: String)
            """.trimIndent()),
            SourceFile.kotlin("SimpleResolver.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(SimpleIntent::class)
                class SimpleResolver @Inject constructor(
                    private val service: BadService
                ) : Resolver<SimpleIntent, String> {
                    override suspend fun resolve(intent: SimpleIntent): String = "ok"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("must have an @Inject constructor"))
    }

    @Test
    fun `@Bind resolves interface to concrete implementation`() {
        val result = compile(
            simpleIntent,
            SourceFile.kotlin("Repository.kt", """
                import com.singularity_universe.axon.Bind
                import com.singularity_universe.axon.Inject
                interface Repository
                @Bind(Repository::class)
                class LocalRepository @Inject constructor() : Repository
            """.trimIndent()),
            SourceFile.kotlin("SimpleResolver.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(SimpleIntent::class)
                class SimpleResolver @Inject constructor(
                    private val repo: Repository
                ) : Resolver<SimpleIntent, String> {
                    override suspend fun resolve(intent: SimpleIntent): String = "ok"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.generatedAxonRegistration()
        assertNotNull(generated)
        assertTrue(generated.contains("val localRepository = lazy { LocalRepository() }"))
        assertTrue(generated.contains("val simpleResolver = lazy { SimpleResolver(localRepository.value) }"))
    }

    @Test
    fun `@Bind implementation shared across multiple dependents`() {
        val result = compile(
            SourceFile.kotlin("Intents.kt", """
                import com.singularity_universe.axon.Intent
                class IntentA : Intent<String>()
                class IntentB : Intent<String>()
            """.trimIndent()),
            SourceFile.kotlin("Repository.kt", """
                import com.singularity_universe.axon.Bind
                import com.singularity_universe.axon.Inject
                interface Repository
                @Bind(Repository::class)
                class LocalRepository @Inject constructor() : Repository
            """.trimIndent()),
            SourceFile.kotlin("ResolverA.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(IntentA::class)
                class ResolverA @Inject constructor(val repo: Repository) : Resolver<IntentA, String> {
                    override suspend fun resolve(intent: IntentA): String = "a"
                }
            """.trimIndent()),
            SourceFile.kotlin("ResolverB.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(IntentB::class)
                class ResolverB @Inject constructor(val repo: Repository) : Resolver<IntentB, String> {
                    override suspend fun resolve(intent: IntentB): String = "b"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.generatedAxonRegistration()!!
        val occurrences = "val localRepository = lazy".toRegex().findAll(generated).count()
        assertEquals(1, occurrences, "LocalRepository should be declared exactly once even when bound interface is used by multiple resolvers")
    }

    @Test
    fun `circular dependency emits compile error`() {
        val result = compile(
            simpleIntent,
            SourceFile.kotlin("ServiceA.kt", """
                import com.singularity_universe.axon.Inject
                class ServiceA @Inject constructor(val b: ServiceB)
            """.trimIndent()),
            SourceFile.kotlin("ServiceB.kt", """
                import com.singularity_universe.axon.Inject
                class ServiceB @Inject constructor(val a: ServiceA)
            """.trimIndent()),
            SourceFile.kotlin("SimpleResolver.kt", """
                import com.singularity_universe.axon.Inject
                import com.singularity_universe.axon.Resolve
                import com.singularity_universe.axon.Resolver
                @Resolve(SimpleIntent::class)
                class SimpleResolver @Inject constructor(
                    private val a: ServiceA
                ) : Resolver<SimpleIntent, String> {
                    override suspend fun resolve(intent: SimpleIntent): String = "ok"
                }
            """.trimIndent())
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("Circular dependency detected"))
    }
}
