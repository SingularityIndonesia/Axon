package com.singularity_universe.axon.sample

import com.singularity_universe.axon.Axon
import com.singularity_universe.axon.Intent
import com.singularity_universe.axon.Resolver
import com.singularity_universe.axon.exception.NoHandlerException
import com.singularity_universe.axon.exception.ResolverException
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

// --- ResolverException demo ---

data class DeleteAccountResult(val success: Boolean)

data class DeleteAccountIntent(
    override val result: DeleteAccountResult? = null
) : Intent<DeleteAccountResult>()

class BuggyDeleteAccountResolver : Resolver<DeleteAccountIntent, DeleteAccountResult> {
    override suspend fun resolve(intent: DeleteAccountIntent, emit: suspend (Intent<DeleteAccountResult>) -> Unit) {
        throw IllegalStateException("oops — resolver bug!")
    }
}

// --- Main ---

fun main() = runBlocking {
    val axon = Axon()
    axon.registerResolver(LoginIntent::class, LoginResolver())
    axon.registerResolver(DeleteAccountIntent::class, BuggyDeleteAccountResolver())

    println("=== dispatch: LoginIntent ===")
    axon.dispatch(LoginIntent(username = "steve", password = "secret"))
        .catch { e ->
            when (e) {
                is NoHandlerException -> println("No handler: ${e.message}")
                is ResolverException  -> println("Resolver bug: ${e.message}")
                else                  -> throw e
            }
        }
        .collect { intent -> println("result: ${intent.result}") }

    println()
    println("=== dispatch: LogoutIntent (no resolver) ===")
    axon.dispatch(LogoutIntent())
        .catch { e ->
            when (e) {
                is NoHandlerException -> println("No handler: ${e.message}")
                is ResolverException  -> println("Resolver bug: ${e.message}")
                else                  -> throw e
            }
        }
        .collect { intent -> println("result: ${intent.result}") }

    println()
    println("=== dispatch: DeleteAccountIntent (buggy resolver) ===")
    axon.dispatch(DeleteAccountIntent())
        .catch { e ->
            when (e) {
                is NoHandlerException -> println("No handler: ${e.message}")
                is ResolverException  -> println("Resolver bug caught by caller: ${e.message}")
                else                  -> throw e
            }
        }
        .collect { intent -> println("result: ${intent.result}") }
}
