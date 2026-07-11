package datasource

import com.singularity_universe.axon.Inject

class AuthWebApi @Inject constructor() {
    suspend fun login(username: String, password: String): String {
        // Simulate network call
        return "jwt.${username}.${password}.signed"
    }
}
