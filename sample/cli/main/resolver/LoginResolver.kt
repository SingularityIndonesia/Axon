package resolver

import com.singularity_universe.axon.Inject
import com.singularity_universe.axon.Resolve
import com.singularity_universe.axon.Resolver
import datasource.AuthenticationService
import intent.LoginIntent
import intent.LoginIntent.LoginResult
import intent.LoginIntent.LoginResult.LoginSuccess

@Resolve(LoginIntent::class)
class LoginResolver @Inject constructor(
    private val authService: AuthenticationService
) : Resolver<LoginIntent, LoginResult> {
    override suspend fun resolve(intent: LoginIntent): LoginResult {
        val token = authService.login(intent.data.username, intent.data.password)
        return LoginSuccess(token = token)
    }
}
