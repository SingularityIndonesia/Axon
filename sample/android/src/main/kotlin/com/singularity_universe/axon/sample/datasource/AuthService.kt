package com.singularity_universe.axon.sample.datasource

import com.singularity_universe.axon.Inject
import kotlinx.coroutines.delay

class AuthService @Inject constructor() {

    suspend fun login(username: String, password: String): String? {
        delay(800) // simulate network
        if (username == "admin" && password == "password") {
            return "jwt.$username.signed"
        }
        return null
    }
}
