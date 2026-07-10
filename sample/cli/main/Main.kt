import com.singularity_universe.axon.Axon
import com.singularity_universe.axon.exception.NoHandlerException
import com.singularity_universe.axon.exception.ResolverException
import com.singularity_universe.axon.untils.Log
import intent.BuggyDeleteAccountResolver
import intent.MyAppIntent.DeleteAccountIntent
import intent.MyAppIntent.LoginIntent
import intent.MyAppIntent.LogoutIntent
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import resolver.LoginResolver

fun main() = runBlocking {
    // Init
    val axon = Axon()
    initAxon(axon)

    axon.dispatch(LoginIntent(username = "steve", password = "secret"))
        .catch { e ->
            when (e) {
                is NoHandlerException -> Log.error("No handler: ${e.message}")
                is ResolverException -> Log.error("Resolver bug: ${e.message}")
                else -> throw e
            }
        }
        .collect { intent -> println("result: ${intent.result}") }

    axon.dispatch(LogoutIntent())
        .catch { e ->
            when (e) {
                is NoHandlerException -> Log.error("No handler: ${e.message}")
                is ResolverException -> Log.error("Resolver bug: ${e.message}")
                else -> throw e
            }
        }
        .collect { intent -> println("result: ${intent.result}") }

    axon.dispatch(DeleteAccountIntent())
        .catch { e ->
            when (e) {
                is NoHandlerException -> Log.error("No handler: ${e.message}")
                is ResolverException -> Log.error("Resolver bug caught by caller: ${e.message}")
                else -> throw e
            }
        }
        .collect { intent -> println("result: ${intent.result}") }
}

private fun initAxon(axon: Axon) {
    axon.registerResolver(LoginIntent::class, LoginResolver())
    axon.registerResolver(DeleteAccountIntent::class, BuggyDeleteAccountResolver())
}