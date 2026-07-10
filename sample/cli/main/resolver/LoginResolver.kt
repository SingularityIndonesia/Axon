package resolver

import com.singularity_universe.axon.Handler
import com.singularity_universe.axon.Resolver
import intent.MyAppIntent.LoginIntent
import intent.MyAppIntent.LoginIntent.LoginResult

@Resolver(LoginIntent::class)
class LoginResolver : Handler<LoginIntent, LoginResult> {
    override suspend fun resolve(intent: LoginIntent): LoginResult {
        return LoginResult(token = "jwt.token.abc123")
    }
}
