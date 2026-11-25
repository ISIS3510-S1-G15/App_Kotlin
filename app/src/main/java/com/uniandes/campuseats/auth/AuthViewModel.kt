package com.uniandes.campuseats.auth

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
    private val connectivityObserver: ConnectivityMonitor,
    /** Limpieza unica de datos de usuario (outbox, perfil, caches...). Se usa en TODOS los caminos de logout. */
    private val purgeUserData: suspend (flushFirst: Boolean) -> Unit = {}
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
        if (!connectivityObserver.online.value) {
            error = NO_CONNECTION
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
        if (!connectivityObserver.online.value) {
            error = NO_CONNECTION
            return
        }
        submit { repository.register(name, email, password) }
    }

    // Para Eventual Connectivity. Muestra una alerta de que hay datos que no se han sincronizado antes de cerrar sesion si los hay
    fun attemptLogout() {
        viewModelScope.launch {
            // Los eventos de analitica no cuentan: el usuario no "pierde" nada si no se envian
            val outboxCount = localStore.pendingCountExcludingAnalytics()

            val profileRecord = localStore.loadProfileRecord()
            val hasPendingProfile = profileRecord?.pendingSync == true

            val isOnline = connectivityObserver.online.value

            if (!isOnline && (outboxCount > 0 || hasPendingProfile)) {
                _showLogoutWarning.value = true
            } else {
                confirmLogout()
            }
        }
    }
    /** Cierra sesion: envia lo pendiente (si hay red), limpia los datos del usuario y borra el token. */
    fun confirmLogout() {
        _showLogoutWarning.value = false
        viewModelScope.launch {
            purgeUserData(true)
            repository.logout()
            user = null
        }
    }

    /**
     * El servidor rechazo el token (401) mientras sincronizaba. Se cierra la sesion SIN borrar la cola de
     * salida: al volver a entrar con la misma cuenta los cambios pendientes se envian.
     */
    fun onSessionExpired() {
        if (user == null) return
        repository.logout()
        user = null
        error = "Your session expired. Sign in again to sync your pending changes."
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Something went wrong. Try again."
            } finally {
                loading = false
            }
        }
    }

    companion object {
        private const val NO_CONNECTION = "No internet connection. Connect to the internet to sign in."

        fun factory(repository: AuthRepository,
                    localStore: LocalStore,
                    connectivityObserver: ConnectivityMonitor,
                    purgeUserData: suspend (Boolean) -> Unit = {}): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(repository, localStore, connectivityObserver, purgeUserData) }
        }
    }
}
