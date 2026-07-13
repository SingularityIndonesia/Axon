package intent

import com.singularity_universe.axon.Intent

class LogoutIntent(
    val userId: String,
    parent: Intent<*>?,
) : MyAppIntent<LogoutIntent.LogoutResult>(parent) {
    data class LogoutResult(val success: Boolean)
}