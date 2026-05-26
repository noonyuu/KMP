package com.noonyuu.prc

import com.noonyuu.prc.infra.api.InMemoryUserApi
import com.noonyuu.prc.infra.api.UserApi
import com.noonyuu.prc.infra.repository.UserRepositoryImpl
import com.noonyuu.prc.usecase.GetUserUseCase

object AppContainer {
    private val userApi: UserApi = InMemoryUserApi()
    private val userRepository = UserRepositoryImpl(userApi)
    val getUserUseCase: GetUserUseCase = GetUserUseCase(userRepository)
}
