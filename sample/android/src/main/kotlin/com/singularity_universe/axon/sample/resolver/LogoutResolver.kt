package com.singularity_universe.axon.sample.resolver

import com.singularity_universe.axon.Resolve
import com.singularity_universe.axon.Resolver
import com.singularity_universe.axon.sample.intent.LogoutIntent
import com.singularity_universe.axon.sample.intent.LogoutIntent.LogoutResult

@Resolve(LogoutIntent::class)
class LogoutResolver : Resolver<LogoutIntent, LogoutResult> {

    override suspend fun resolve(intent: LogoutIntent): LogoutResult {
        return LogoutResult(success = true)
    }
}
