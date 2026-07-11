package datasource

import com.singularity_universe.axon.Inject

class AuthenticationService @Inject constructor(
    private val api: AuthWebApi,
    private val db: LocalDatabase
) {
    suspend fun login(username: String, password: String): String {
        val token = api.login(username, password)
        db.saveToken(token)
        return token
    }
}
