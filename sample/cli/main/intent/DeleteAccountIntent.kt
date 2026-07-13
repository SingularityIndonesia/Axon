package intent

import com.singularity_universe.axon.Intent

class DeleteAccountIntent(
    val userId: String,
    parent: Intent<*>?,
) : MyAppIntent<DeleteAccountIntent.DeleteAccountResult>(parent) {
    data class DeleteAccountResult(val success: Boolean)
}