package intent

import com.singularity_universe.axon.Intent

sealed class MyAppIntent<out R> : Intent<R>() {

    data class LoginIntent(
        val username: String,
        val password: String,
        override val result: LoginResult? = null
    ) : MyAppIntent<LoginIntent.LoginResult>() {
        data class LoginResult(val token: String)
    }

    data class LogoutIntent(
        override val result: LogoutResult? = null
    ) : MyAppIntent<LogoutIntent.LogoutResult>() {
        data class LogoutResult(val success: Boolean)
    }

    data class DeleteAccountIntent(
        override val result: DeleteAccountResult? = null
    ) : MyAppIntent<DeleteAccountIntent.DeleteAccountResult>() {
        data class DeleteAccountResult(val success: Boolean)
    }
}
