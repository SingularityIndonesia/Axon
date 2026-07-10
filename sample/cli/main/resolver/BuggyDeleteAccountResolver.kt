package resolver

import com.singularity_universe.axon.Handler
import com.singularity_universe.axon.Resolver
import intent.MyAppIntent.DeleteAccountIntent
import intent.MyAppIntent.DeleteAccountIntent.DeleteAccountResult

@Resolver(DeleteAccountIntent::class)
class BuggyDeleteAccountResolver : Handler<DeleteAccountIntent, DeleteAccountResult> {
    override suspend fun resolve(intent: DeleteAccountIntent): DeleteAccountResult {
        throw IllegalStateException("oops — resolver bug!")
    }
}
