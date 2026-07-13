package intent

import com.singularity_universe.axon.Intent

sealed class MyAppIntent<out R>(parent: Intent<*>? = null) : Intent<R>(parent) {

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

    class LogoutIntent(
        val userId: String,
        parent: Intent<*>? = null,
    ) : MyAppIntent<LogoutIntent.LogoutResult>(parent) {
        data class LogoutResult(val success: Boolean)
    }

    class DeleteAccountIntent(
        val userId: String,
        parent: Intent<*>? = null,
    ) : MyAppIntent<DeleteAccountIntent.DeleteAccountResult>(parent) {
        data class DeleteAccountResult(val success: Boolean)
    }
}
