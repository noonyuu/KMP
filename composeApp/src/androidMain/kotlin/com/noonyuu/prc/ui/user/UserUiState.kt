package com.noonyuu.prc.ui.user

import com.noonyuu.prc.domain.entity.User

data class UserUiState(
    val loading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
)
