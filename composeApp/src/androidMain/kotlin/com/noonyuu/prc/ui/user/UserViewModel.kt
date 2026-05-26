package com.noonyuu.prc.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noonyuu.prc.AppContainer
import com.noonyuu.prc.domain.entity.UserId
import com.noonyuu.prc.usecase.GetUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val getUser: GetUserUseCase = AppContainer.getUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UserUiState())
    val state: StateFlow<UserUiState> = _state.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val user = getUser(UserId(id))
            _state.value = if (user != null) {
                UserUiState(user = user)
            } else {
                UserUiState(error = "User '$id' not found")
            }
        }
    }
}
