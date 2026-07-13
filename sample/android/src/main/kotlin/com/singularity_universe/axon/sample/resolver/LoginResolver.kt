package com.singularity_universe.axon.sample.resolver

import com.singularity_universe.axon.Inject
import com.singularity_universe.axon.Resolve
import com.singularity_universe.axon.Resolver
import com.singularity_universe.axon.sample.datasource.AuthService
import com.singularity_universe.axon.sample.intent.LoginIntent
import com.singularity_universe.axon.sample.intent.LoginIntent.LoginResult

@Resolve(LoginIntent::class)
class LoginResolver @Inject constructor(
    private val authService: AuthService,
) : Resolver<LoginIntent, LoginResult> {

    override suspend fun resolve(intent: LoginIntent): LoginResult {
        val token = authService.login(intent.data.username, intent.data.password)
            ?: return LoginResult.InvalidCredentials
        return LoginResult.Success(token)
    }
}
