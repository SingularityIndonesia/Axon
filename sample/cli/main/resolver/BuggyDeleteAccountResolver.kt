package intent

import com.singularity_universe.axon.Intent
import com.singularity_universe.axon.Resolver
import intent.MyAppIntent.DeleteAccountIntent
import intent.MyAppIntent.DeleteAccountIntent.DeleteAccountResult

class BuggyDeleteAccountResolver : Resolver<DeleteAccountIntent, DeleteAccountResult> {
    override suspend fun resolve(
        intent: DeleteAccountIntent,
        emit: suspend (Intent<DeleteAccountResult>) -> Unit
    ) {
        throw IllegalStateException("oops — resolver bug!")
    }
}