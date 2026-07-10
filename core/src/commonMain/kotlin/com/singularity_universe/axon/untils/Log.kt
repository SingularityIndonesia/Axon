package com.singularity_universe.axon.untils


object Log {
    fun error(message: String) {
        error("", message)
    }

    fun error(context: String, message: String) {
        val context = context.takeIf { it.isNotBlank() }?.let { "[$it] " }.orEmpty()
        println("\u001B[31mError: $context$message\u001B[0m")
    }

    fun fatalError(message: String) {
        fatalError("", message)
    }

    fun fatalError(context: String, message: String) {
        val context = context.takeIf { it.isNotBlank() }?.let { "[$it] " }.orEmpty()
        println("\u001B[31mFatal: $context$message\u001B[0m")
    }
}