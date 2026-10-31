package com.uniandes.campuseats.auth

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.uniandes.campuseats.data.local.LocalStore
import com.uniandes.campuseats.net.ConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    private val localStore: LocalStore,
    private val connectivityObserver: ConnectivityMonitor
) : ViewModel() {

    var user by mutableStateOf<AuthUser?>(null)
        private set
    var restoring by mutableStateOf(true)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private val _showLogoutWarning = MutableStateFlow(false)
    val showLogoutWarning: StateFlow<Boolean> = _showLogoutWarning.asStateFlow()

    init {
        viewModelScope.launch {
            user = repository.restoreSession()
            restoring = false
        }
    }

    fun login(email: String, password: String) {
        val problem = AuthValidation.validate(null, email, password)
        if (problem != null) {
            error = problem
            return
        }
        submit { repository.login(email, password) }
    }

    fun register(name: String, email: String, password: String) {
        val problem = AuthValidation.validate(name, email, password)
        if (problem != null) {
            error = problem
            return
        }
        submit { repository.register(name, email, password) }
    }

    // Para Eventual Connectivity. Muestra una alerta de que hay datos que no se han sincronizado antes de cerrar sesion si los hay
    fun attemptLogout() {
        viewModelScope.launch {
            val outboxCount = localStore.count()

            val profileRecord = localStore.loadProfileRecord()
            val hasPendingProfile = profileRecord?.pendingSync == true

            val isOnline = connectivityObserver.online.value

            Log.d("TEST_LOGOUT", "Internet: $isOnline | Outbox: $outboxCount | Perfil Pendiente: $hasPendingProfile")

            if (!isOnline && (outboxCount > 0 || hasPendingProfile)) {
                _showLogoutWarning.value = true
            } else {
                confirmLogout()
            }
        }
    }
    fun confirmLogout() {
        _showLogoutWarning.value = false
        repository.logout()
        user = null
    }

    fun cancelLogout() {
        _showLogoutWarning.value = false
    }

    fun clearError() {
        error = null
    }

    private fun submit(action: suspend () -> AuthUser) {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                user = action()
            } catch (e: AuthException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    companion object {
        fun factory(repository: AuthRepository,
                    localStore: LocalStore,
                    connectivityObserver: ConnectivityMonitor): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(repository, localStore, connectivityObserver) }
        }
    }
}
