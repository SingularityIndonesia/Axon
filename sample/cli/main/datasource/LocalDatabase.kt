package datasource

import com.singularity_universe.axon.Inject

class LocalDatabase @Inject constructor() {
    private var token: String? = null

    fun saveToken(token: String) {
        this.token = token
    }

    fun getToken(): String? = token
}
