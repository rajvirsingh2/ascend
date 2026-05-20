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
            when(val res=interestsRepository.getMyInterests()){
                is Result.Success ->{
                    val (configured,_) = res.data
                    _destination.value=if(!configured){
                        SplashDestination.InterestsOnboarding
                    } else{
                        SplashDestination.Dashboard
                    }
                }

                is Result.Error ->{
                    _destination.value= SplashDestination.Dashboard
                }

                is Result.Loading ->{/*TODO*/}
            }
        }
    }
}