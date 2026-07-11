import com.singularity_universe.axon.Axon
import com.singularity_universe.axon.exception.NoHandlerException
import com.singularity_universe.axon.exception.ResolverException
import com.singularity_universe.axon.untils.Log
import datasource.AuthWebApi
import datasource.AuthenticationService
import datasource.LocalDatabase
import intent.MyAppIntent.DeleteAccountIntent
import intent.MyAppIntent.LoginIntent
import intent.MyAppIntent.LogoutIntent
import kotlinx.coroutines.runBlocking
import resolver.BuggyDeleteAccountResolver
import resolver.LoginResolver

fun main() = runBlocking {
    val axon = Axon()

    // Manual wiring — will be replaced by axon.init() once KSP handles @Inject deps (item 5-6)
    val db = LocalDatabase()
    val authWebApi = AuthWebApi()
    val authService = AuthenticationService(authWebApi, db)
    axon.registerResolver(LoginIntent::class, lazy { LoginResolver(authService) })
    axon.registerResolver(DeleteAccountIntent::class, lazy { BuggyDeleteAccountResolver() })

    // Happy path — resolver returns result directly
    when (val loginResult = axon.dispatch(LoginIntent(username = "steve", password = "secret"))) {
        is LoginIntent.LoginResult.LoginSuccess -> println("login success: ${loginResult.token}")
        is LoginIntent.LoginResult.LoginBlocked -> println("login blocked")
        is LoginIntent.LoginResult.UnableToProcess -> println("unable to process")
    }

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
