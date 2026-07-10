package intent

import com.singularity_universe.axon.Intent

sealed class MyAppIntent<out R> : Intent<R>() {

    data class LoginIntent(
        val username: String,
        val password: String
    ) : MyAppIntent<LoginIntent.LoginResult>() {
        data class LoginResult(val token: String)
    }

    data class LogoutIntent(
        val userId: String
    ) : MyAppIntent<LogoutIntent.LogoutResult>() {
        data class LogoutResult(val success: Boolean)
    }

    data class DeleteAccountIntent(
        val userId: String
    ) : MyAppIntent<DeleteAccountIntent.DeleteAccountResult>() {
        data class DeleteAccountResult(val success: Boolean)
    }
}
