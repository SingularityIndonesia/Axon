package com.singularity_universe.axon.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Represents a single node in the dependency graph.
 *
 * @param declaration the class to be instantiated.
 * @param constructor the @Inject constructor used to instantiate it.
 * @param params the direct dependencies (constructor parameters) in order.
 */
data class DependencyNode(
    val declaration: KSClassDeclaration,
    val constructor: KSFunctionDeclaration,
    val params: List<KSClassDeclaration>
)

class AxonSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val resolverClasses = resolver
            .getSymbolsWithAnnotation("com.singularity_universe.axon.Resolve")
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        if (resolverClasses.isEmpty()) return emptyList()

        // Build the full dependency graph starting from all @Resolve classes
        val graph = buildDependencyGraph(resolverClasses)

        // Log the graph for verification
        graph.values.forEach { node ->
            if (node.params.isEmpty()) {
                logger.info("Axon [graph]: ${node.declaration.simpleName.asString()} — no dependencies")
            } else {
                val depNames = node.params.joinToString { it.simpleName.asString() }
                logger.info("Axon [graph]: ${node.declaration.simpleName.asString()} — depends on [$depNames]")
            }
        }

        val sourceFiles = resolverClasses.mapNotNull { it.containingFile }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, *sourceFiles.toTypedArray()),
            packageName = "com.singularity_universe.axon.generated",
            fileName = "AxonRegistration"
        )

        // Only generate registration for resolvers whose full dependency chain is resolved.
        // Resolvers with constructor params are skipped until item 5-6 (topological sort + full generation).
        val wiringReady = resolverClasses.filter { clazz ->
            graph[clazz.qualifiedName?.asString()]?.params?.isEmpty() == true
        }

        file.bufferedWriter().use { out ->
            out.write("package com.singularity_universe.axon.generated\n\n")
            out.write("import com.singularity_universe.axon.Axon\n")

            wiringReady.forEach { clazz ->
                val intentType = clazz.resolveIntentType()
                out.write("import ${clazz.qualifiedName!!.asString()}\n")
                out.write("import ${intentType.declaration.qualifiedName!!.asString()}\n")
            }

            out.write("\nfun Axon.init() {\n")
            wiringReady.forEach { clazz ->
                val intentType = clazz.resolveIntentType()
                val intentName = intentType.declaration.simpleName.asString()
                val resolverName = clazz.simpleName.asString()
                out.write("    registerResolver($intentName::class, lazy { $resolverName() })\n")
            }
            out.write("}\n")
        }

        logger.info("Axon: generated init() for ${resolverClasses.size} resolver(s)")
        return emptyList()
    }

    /**
     * Builds the full dependency graph by recursively collecting all @Inject
     * dependencies reachable from the given [resolvers].
     *
     * The graph is keyed by fully qualified class name to ensure each class
     * appears as a single node regardless of how many resolvers depend on it.
     */
    private fun buildDependencyGraph(
        resolvers: List<KSClassDeclaration>
    ): Map<String, DependencyNode> {
        val graph = mutableMapOf<String, DependencyNode>()
        resolvers.forEach { collectNode(it, graph) }
        return graph
    }

    /**
     * Recursively collects [clazz] and all its transitive @Inject dependencies
     * into [graph]. Already-visited nodes are skipped to avoid duplicate work.
     */
    private fun collectNode(
        clazz: KSClassDeclaration,
        graph: MutableMap<String, DependencyNode>
    ) {
        val key = clazz.qualifiedName?.asString() ?: return
        if (key in graph) return // already visited — shared dependency, collect once

        val constructor = clazz.findInjectConstructor() ?: run {
            logger.error(
                "${clazz.simpleName.asString()} must have an @Inject constructor.",
                clazz
            )
            return
        }

        val params = constructor.parameters.map { param ->
            param.type.resolve().declaration as KSClassDeclaration
        }

        graph[key] = DependencyNode(clazz, constructor, params)

        // Recurse into each dependency
        params.forEach { dep -> collectNode(dep, graph) }
    }

    // Returns the @Inject-annotated primary constructor, or null if none exists.
    private fun KSClassDeclaration.findInjectConstructor(): KSFunctionDeclaration? =
        primaryConstructor?.takeIf { constructor ->
            constructor.annotations.any { it.shortName.asString() == "Inject" }
        }

    private fun KSClassDeclaration.resolveIntentType(): KSType =
        annotations
            .first { it.shortName.asString() == "Resolve" }
            .arguments
            .first()
            .value as KSType
}
