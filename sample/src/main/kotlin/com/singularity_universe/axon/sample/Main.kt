package com.singularity_universe.axon.sample

import com.singularity_universe.axon.Axon
import com.singularity_universe.axon.Intent
import com.singularity_universe.axon.exception.NoHandlerException
import com.singularity_universe.axon.Resolver
import com.singularity_universe.axon.ext.registerResolver
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking

// --- Domain ---

data class LoginResult(val token: String)

data class LoginIntent(
    val username: String,
    val password: String,
    override val result: LoginResult? = null
) : Intent<LoginResult>()

class LoginResolver : Resolver<LoginIntent, LoginResult> {
    override suspend fun resolve(intent: LoginIntent, emit: suspend (Intent<LoginResult>) -> Unit) {
        emit(intent.copy(result = LoginResult(token = "authenticating...")))
        emit(intent.copy(result = LoginResult(token = "jwt.token.abc123")))
    }
}

// --- No handler demo ---

data class LogoutResult(val success: Boolean)

data class LogoutIntent(
    override val result: LogoutResult? = null
) : Intent<LogoutResult>()

// --- Main ---

fun main() = runBlocking {
    val axon = Axon()
    axon.registerResolver<LoginIntent, LoginResult>(LoginResolver())

    println("=== proceed: LoginIntent ===")
    axon.proceed(LoginIntent(username = "steve", password = "secret"))
        .catch { e ->
            if (e is NoHandlerException) println("No handler: ${e.message}")
            else throw e
        }
        .collect { intent ->
            println("result: ${intent.result}")
        }

    println()
    println("=== proceed: LogoutIntent (no resolver) ===")
    axon.proceed(LogoutIntent())
        .catch { e ->
            if (e is NoHandlerException) println("No handler: ${e.message}")
            else throw e
        }
        .collect { intent ->
            println("result: ${intent.result}")
        }
}
