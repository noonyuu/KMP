package com.noonyuu.prc.usecase

import com.noonyuu.prc.domain.entity.User
import com.noonyuu.prc.domain.entity.UserId
import com.noonyuu.prc.domain.repository.UserRepository

class GetUserUseCase(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(id: UserId): User? =
        userRepository.findById(id)
}
