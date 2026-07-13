package intent

import com.singularity_universe.axon.Intent

class LoginIntent(
    val data: LoginData,
    parent: Intent<*>? = null,
) : MyAppIntent<LoginIntent.LoginResult>(parent) {
    data class LoginData(
        val username: String,
        val password: String,
    )

    sealed class LoginResult {
        data class LoginSuccess(val token: String) : LoginResult()
        data object LoginBlocked : LoginResult()
        data object UnableToProcess : LoginResult()
    }
}