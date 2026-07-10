package resolver

import com.singularity_universe.axon.Resolve
import com.singularity_universe.axon.Resolver
import intent.MyAppIntent.LoginIntent
import intent.MyAppIntent.LoginIntent.LoginResult

@Resolve(LoginIntent::class)
class LoginResolver : Resolver<LoginIntent, LoginResult> {
    override suspend fun resolve(intent: LoginIntent): LoginResult {
        return LoginResult(token = "jwt.token.abc123")
    }
}
