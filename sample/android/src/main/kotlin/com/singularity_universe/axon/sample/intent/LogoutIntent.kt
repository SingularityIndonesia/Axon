package com.singularity_universe.axon.sample.intent

import com.singularity_universe.axon.Intent

class LogoutIntent(
    parent: Intent<*>?,
) : Intent<LogoutIntent.LogoutResult>(parent) {

    data class LogoutResult(val success: Boolean)
}
