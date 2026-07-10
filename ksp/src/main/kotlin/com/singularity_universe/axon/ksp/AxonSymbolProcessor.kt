package com.singularity_universe.axon.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

class AxonSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver
            .getSymbolsWithAnnotation("com.singularity_universe.axon.Resolve")
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        if (annotated.isEmpty()) return emptyList()

        val sourceFiles = annotated.mapNotNull { it.containingFile }

        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = false, *sourceFiles.toTypedArray()),
            packageName = "com.singularity_universe.axon.generated",
            fileName = "AxonRegistration"
        )

        file.bufferedWriter().use { out ->
            out.write("package com.singularity_universe.axon.generated\n\n")
            out.write("import com.singularity_universe.axon.Axon\n")

            annotated.forEach { clazz ->
                val intentType = clazz.resolveIntentType()
                out.write("import ${clazz.qualifiedName!!.asString()}\n")
                out.write("import ${intentType.declaration.qualifiedName!!.asString()}\n")
            }

            out.write("\nfun Axon.init() {\n")
            annotated.forEach { clazz ->
                val intentType = clazz.resolveIntentType()
                val intentName = intentType.declaration.simpleName.asString()
                val resolverName = clazz.simpleName.asString()
                out.write("    registerResolver($intentName::class, $resolverName())\n")
            }
            out.write("}\n")
        }

        logger.info("Axon: generated registerAll() for ${annotated.size} resolver(s)")
        return emptyList()
    }

    private fun KSClassDeclaration.resolveIntentType(): KSType =
        annotations
            .first { it.shortName.asString() == "Resolve" }
            .arguments
            .first()
            .value as KSType
}
