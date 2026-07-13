package resolver

import com.singularity_universe.axon.Inject
import com.singularity_universe.axon.Resolve
import com.singularity_universe.axon.Resolver
import datasource.LocalDatabase
import intent.LogoutIntent
import intent.LogoutIntent.LogoutResult

@Resolve(LogoutIntent::class)
class LogoutResolver @Inject constructor(
    private val db: LocalDatabase
) : Resolver<LogoutIntent, LogoutResult> {
    override suspend fun resolve(intent: LogoutIntent): LogoutResult {
        db.saveToken("") // clear token on logout
        return LogoutResult(success = true)
    }
}
