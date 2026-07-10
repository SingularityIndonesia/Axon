import com.singularity_universe.axon.Axon
import com.singularity_universe.axon.exception.NoHandlerException
import com.singularity_universe.axon.exception.ResolverException
import com.singularity_universe.axon.untils.Log
import intent.MyAppIntent.DeleteAccountIntent
import intent.MyAppIntent.LoginIntent
import intent.MyAppIntent.LogoutIntent
import kotlinx.coroutines.runBlocking
import resolver.BuggyDeleteAccountResolver
import resolver.LoginResolver

fun main() = runBlocking {
    val axon = Axon()
    initAxon(axon)

    // Happy path — resolver returns result directly
    val loginResult = axon.dispatch(LoginIntent(username = "steve", password = "secret"))
    println("login: $loginResult")

    // No handler registered — NoHandlerException
    try {
        val logoutResult = axon.dispatch(LogoutIntent(userId = "user-123"))
        println("logout: $logoutResult")
    } catch (e: NoHandlerException) {
        Log.error("No handler: ${e.message}")
    }

    // Resolver throws — ResolverException
    try {
        val deleteResult = axon.dispatch(DeleteAccountIntent(userId = "user-123"))
        println("delete: $deleteResult")
    } catch (e: ResolverException) {
        Log.error("Resolver bug caught by caller: ${e.message}")
    }
}

private fun initAxon(axon: Axon) {
    axon.registerResolver(LoginIntent::class, LoginResolver())
    axon.registerResolver(DeleteAccountIntent::class, BuggyDeleteAccountResolver())
}
