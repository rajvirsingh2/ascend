package com.ascend.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.remote.api.PhysiqueApiService
import com.ascend.app.data.repository.InterestsRepository
import com.ascend.app.data.repository.UserRepository
import com.ascend.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject


sealed class SplashDestination {
    data object InterestsOnboarding: SplashDestination()
    data object Login : SplashDestination()
    data object Dashboard : SplashDestination()
    data object PhysiqueSetup : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    private val interestsRepository: InterestsRepository
) : ViewModel() {

    private val _destination= MutableStateFlow<SplashDestination?>(null)
    val destination=_destination.asStateFlow()

    init {
        route()
    }

    private fun route(){
        viewModelScope.launch {
            val token=tokenDataStore.accessToken.first()
            if(token.isNullOrBlank()){
                _destination.value= SplashDestination.Login
                return@launch
            }
            
            // Fast path: bypass network if interests already configured locally
            val locallyConfigured = tokenDataStore.interestsConfigured.first()
            if(locallyConfigured == true) {
                _destination.value = SplashDestination.Dashboard
                return@launch
            }
            
            // Slow path: fetch from network with 3s timeout
            try {
                withTimeout(3000) {
                    when(val res=interestsRepository.getMyInterests()){
                        is Result.Success ->{
                            val (configured,_) = res.data
                            tokenDataStore.setInterestsConfigured(configured)
                            _destination.value=if(!configured){
                                SplashDestination.InterestsOnboarding
                            } else{
                                SplashDestination.Dashboard
                            }
                        }
                        is Result.Error ->{
                            if (res.message.contains("404")) {
                                _destination.value = SplashDestination.PhysiqueSetup
                            } else {
                                _destination.value = SplashDestination.Dashboard
                            }
                        }
                        is Result.Loading ->{/*TODO*/}
                    }
                }
            } catch (e: TimeoutCancellationException) {
                // If the backend takes too long (e.g. cold start), don't freeze the splash screen.
                // Just fall back to the dashboard.
                _destination.value = SplashDestination.Dashboard
            }
        }
    }
}