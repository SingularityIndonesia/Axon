package resolver

import com.singularity_universe.axon.Inject
import com.singularity_universe.axon.Resolve
import com.singularity_universe.axon.Resolver
import intent.MyAppIntent.DeleteAccountIntent
import intent.MyAppIntent.DeleteAccountIntent.DeleteAccountResult

@Resolve(DeleteAccountIntent::class)
class BuggyDeleteAccountResolver @Inject constructor() : Resolver<DeleteAccountIntent, DeleteAccountResult> {
    override suspend fun resolve(intent: DeleteAccountIntent): DeleteAccountResult {
        throw IllegalStateException("oops — resolver bug!")
    }
}
