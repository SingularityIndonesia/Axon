package com.singularity_universe.axon.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.singularity_universe.axon.Axon
import com.singularity_universe.axon.generated.init
import com.singularity_universe.axon.sample.intent.AppIntent
import com.singularity_universe.axon.sample.intent.LoginIntent
import com.singularity_universe.axon.sample.intent.LoginIntent.LoginResult
import com.singularity_universe.axon.sample.intent.LogoutIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val axon = Axon().apply { init() }

    // Root intent — anchors the parent chain for all operations in this session
    private val appIntent = AppIntent(parent = null)

    private val _state = MutableStateFlow<ScreenState>(ScreenState.Login())
    val state: StateFlow<ScreenState> = _state

    fun login(username: String, password: String) {
        _state.value = ScreenState.Login(loading = true)
        viewModelScope.launch {
            val intent = LoginIntent(
                data = LoginIntent.LoginData(username, password),
                parent = appIntent,
            )
            when (val result = axon.dispatch(intent)) {
                is LoginResult.Success ->
                    _state.value = ScreenState.Dashboard(
                        token = result.token,
                        loginIntent = intent,
                    )
                is LoginResult.InvalidCredentials ->
                    _state.value = ScreenState.Login(error = "Invalid username or password.")
                is LoginResult.UnableToProcess ->
                    _state.value = ScreenState.Login(error = "Unable to process. Try again.")
            }
        }
    }

    fun logout(loginIntent: LoginIntent) {
        viewModelScope.launch {
            // Parent = loginIntent — logout is a direct consequence of the login session
            val intent = LogoutIntent(parent = loginIntent)
            axon.dispatch(intent)
            _state.value = ScreenState.Login()
        }
    }
}

sealed class ScreenState {
    data class Login(
        val loading: Boolean = false,
        val error: String? = null,
    ) : ScreenState()

    data class Dashboard(
        val token: String,
        val loginIntent: LoginIntent,
    ) : ScreenState()
}
