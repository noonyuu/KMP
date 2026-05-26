package com.noonyuu.prc.infra.repository

import com.noonyuu.prc.domain.entity.User
import com.noonyuu.prc.domain.entity.UserId
import com.noonyuu.prc.domain.repository.UserRepository
import com.noonyuu.prc.infra.api.UserApi
import com.noonyuu.prc.infra.mapper.toEntity

class UserRepositoryImpl(
    private val userApi: UserApi,
) : UserRepository {
    override suspend fun findById(id: UserId): User? =
        userApi.fetch(id.value)?.toEntity()
}
