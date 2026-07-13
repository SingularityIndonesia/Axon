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
import com.google.devtools.ksp.symbol.Modifier

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

        // Warn if any Intent type used in @Resolve is a data class
        resolverClasses.forEach { clazz ->
            val intentDeclaration = clazz.resolveIntentType().declaration as? KSClassDeclaration
            if (intentDeclaration != null && Modifier.DATA in intentDeclaration.modifiers) {
                logger.warn(
                    "[Axon] ${intentDeclaration.simpleName.asString()} is a data class. " +
                    "It is not recommended to use a data class as an Intent — " +
                    "copy() produces a new instance, which breaks the parent chain and loses the original intent's identity.",
                    intentDeclaration
                )
            }
        }

        // Build bind map: interface qualified name → concrete implementation
        val bindMap: Map<String, KSClassDeclaration> = resolver
            .getSymbolsWithAnnotation("com.singularity_universe.axon.Bind")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { impl ->
                val annotation = impl.annotations.first { it.shortName.asString() == "Bind" }
                val interfaceType = annotation.arguments.first().value as? KSType ?: return@mapNotNull null
                val interfaceKey = interfaceType.declaration.qualifiedName?.asString() ?: return@mapNotNull null
                interfaceKey to impl
            }
            .toMap()

        val graph = buildDependencyGraph(resolverClasses, bindMap)
        val sharedKeys = findSharedNodes(graph)
        val sorted = topologicalSort(graph)

        // Log graph
        graph.values.forEach { node ->
            val key = node.declaration.qualifiedName?.asString()
            val sharedTag = if (key in sharedKeys) " [shared]" else ""
            if (node.params.isEmpty()) {
                logger.info("Axon [graph]: ${node.declaration.simpleName.asString()}$sharedTag — no dependencies")
            } else {
                val depNames = node.params.joinToString { it.simpleName.asString() }
                logger.info("Axon [graph]: ${node.declaration.simpleName.asString()}$sharedTag — depends on [$depNames]")
            }
        }
        logger.info("Axon [sorted]: ${sorted.joinToString(" → ") { it.declaration.simpleName.asString() }}")

        val resolverKeys = resolverClasses.mapNotNull { it.qualifiedName?.asString() }.toSet()
        val sourceFiles = resolverClasses.mapNotNull { it.containingFile }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, *sourceFiles.toTypedArray()),
            packageName = "com.singularity_universe.axon.generated",
            fileName = "AxonRegistration"
        )

        file.bufferedWriter().use { out ->
            out.write("package com.singularity_universe.axon.generated\n\n")
            out.write("import com.singularity_universe.axon.Axon\n")

            // Imports — all classes in the graph + intent classes for resolvers
            val imports = mutableSetOf<String>()
            sorted.forEach { node -> imports.add(node.declaration.qualifiedName!!.asString()) }
            resolverClasses.forEach { clazz ->
                imports.add(clazz.resolveIntentType().declaration.qualifiedName!!.asString())
            }
            imports.forEach { out.write("import $it\n") }

            out.write("\nfun Axon.init() {\n")

            // Lazy val for every node in topological order
            sorted.forEach { node ->
                val varName = node.declaration.simpleName.asString().toVarName()
                val className = node.declaration.simpleName.asString()
                if (node.params.isEmpty()) {
                    out.write("    val $varName = lazy { $className() }\n")
                } else {
                    val args = node.params.joinToString(", ") { dep ->
                        "${dep.simpleName.asString().toVarName()}.value"
                    }
                    out.write("    val $varName = lazy { $className($args) }\n")
                }
            }

            out.write("\n")

            // registerResolver for @Resolve nodes only
            resolverClasses.forEach { clazz ->
                val intentName = clazz.resolveIntentType().declaration.simpleName.asString()
                val varName = clazz.simpleName.asString().toVarName()
                out.write("    registerResolver($intentName::class, $varName)\n")
            }

            out.write("}\n")
        }

        logger.info("Axon: generated init() for ${resolverClasses.size} resolver(s)")
        return emptyList()
    }

    /**
     * Builds the full dependency graph by recursively collecting all @Inject
     * dependencies reachable from the given [resolvers]. Interface parameters
     * are resolved to their concrete implementations via [bindMap].
     */
    private fun buildDependencyGraph(
        resolvers: List<KSClassDeclaration>,
        bindMap: Map<String, KSClassDeclaration>
    ): Map<String, DependencyNode> {
        val graph = mutableMapOf<String, DependencyNode>()
        val inStack = mutableSetOf<String>()   // currently being traversed
        val path = mutableListOf<String>()      // current traversal path for error reporting
        resolvers.forEach { collectNode(it, graph, inStack, path, bindMap) }
        return graph
    }

    /**
     * Returns all nodes sorted in topological order — each node appears after
     * all of its dependencies, making sequential instantiation safe.
     */
    private fun topologicalSort(graph: Map<String, DependencyNode>): List<DependencyNode> {
        val visited = mutableSetOf<String>()
        val sorted = mutableListOf<DependencyNode>()

        fun visit(key: String) {
            if (key in visited) return
            visited.add(key)
            val node = graph[key] ?: return
            node.params.forEach { dep -> dep.qualifiedName?.asString()?.let { visit(it) } }
            sorted.add(node)
        }

        graph.keys.forEach { visit(it) }
        return sorted
    }

    /**
     * Returns the set of qualified names depended upon by more than one node —
     * these are shared singletons and must appear as a single lazy val in generated code.
     */
    private fun findSharedNodes(graph: Map<String, DependencyNode>): Set<String> {
        val refCount = mutableMapOf<String, Int>()
        graph.values.forEach { node ->
            node.params.forEach { dep ->
                val key = dep.qualifiedName?.asString() ?: return@forEach
                refCount[key] = (refCount[key] ?: 0) + 1
            }
        }
        return refCount.filter { it.value > 1 }.keys.toSet()
    }

    /**
     * Recursively collects [clazz] and all its transitive @Inject dependencies
     * into [graph]. Already-visited nodes are skipped — shared deps are collected once.
     *
     * [inStack] tracks nodes currently being traversed in the active DFS path.
     * [path] records simple names along that path for readable cycle error messages.
     * If a node is encountered while already in [inStack], a circular dependency is reported.
     *
     * Interface parameters are resolved to their concrete implementations via [bindMap].
     */
    private fun collectNode(
        clazz: KSClassDeclaration,
        graph: MutableMap<String, DependencyNode>,
        inStack: MutableSet<String>,
        path: MutableList<String>,
        bindMap: Map<String, KSClassDeclaration>
    ) {
        val key = clazz.qualifiedName?.asString() ?: return
        val simpleName = clazz.simpleName.asString()

        if (key in inStack) {
            val cycleStart = path.indexOf(simpleName)
            val cyclePath = (if (cycleStart >= 0) path.drop(cycleStart) else path) + simpleName
            logger.error(
                "Circular dependency detected: ${cyclePath.joinToString(" → ")}",
                clazz
            )
            return
        }

        if (key in graph) return // already fully processed — shared dependency

        inStack.add(key)
        path.add(simpleName)

        val constructor = clazz.findInjectConstructor() ?: run {
            val context = if (path.isNotEmpty()) " Required by: ${path.joinToString(" → ")}" else ""
            logger.error("$simpleName must have an @Inject constructor.$context", clazz)
            inStack.remove(key)
            path.removeLast()
            return
        }

        val params = constructor.parameters.map { param ->
            val declared = param.type.resolve().declaration as KSClassDeclaration
            val declaredKey = declared.qualifiedName?.asString() ?: return@map declared
            // Resolve interface to its @Bind implementation, if one exists
            bindMap[declaredKey] ?: declared
        }

        graph[key] = DependencyNode(clazz, constructor, params)
        params.forEach { dep -> collectNode(dep, graph, inStack, path, bindMap) }

        inStack.remove(key)
        path.removeLast()
    }

    // Returns the @Inject-annotated constructor, or the primary constructor if it has
    // no parameters (implicitly injectable — @Inject is optional for no-arg constructors).
    private fun KSClassDeclaration.findInjectConstructor(): KSFunctionDeclaration? =
        primaryConstructor?.takeIf { constructor ->
            constructor.annotations.any { it.shortName.asString() == "Inject" }
                || constructor.parameters.isEmpty()
        }

    private fun KSClassDeclaration.resolveIntentType(): KSType =
        annotations
            .first { it.shortName.asString() == "Resolve" }
            .arguments
            .first()
            .value as KSType

    // "AuthWebApi" → "authWebApi"
    private fun String.toVarName(): String =
        replaceFirstChar { it.lowercase() }
}
