package resolver

import com.singularity_universe.axon.Intent
import com.singularity_universe.axon.Resolver
import intent.MyAppIntent.LoginIntent
import intent.MyAppIntent.LoginIntent.LoginResult

class LoginResolver : Resolver<LoginIntent, LoginResult> {
    override suspend fun resolve(intent: LoginIntent, emit: suspend (Intent<LoginResult>) -> Unit) {
        emit(intent.copy(result = LoginResult(token = "authenticating...")))
        emit(intent.copy(result = LoginResult(token = "jwt.token.abc123")))
    }
}