package com.singularity_universe.axon.sample.intent

import com.singularity_universe.axon.Intent

class LoginIntent(
    val data: LoginData,
    parent: Intent<*>?,
) : Intent<LoginIntent.LoginResult>(parent) {

    data class LoginData(
        val username: String,
        val password: String,
    )

    sealed class LoginResult {
        data class Success(val token: String) : LoginResult()
        data object InvalidCredentials : LoginResult()
        data object UnableToProcess : LoginResult()
    }
}
