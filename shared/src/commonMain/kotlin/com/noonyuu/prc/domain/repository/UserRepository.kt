package com.noonyuu.prc.domain.repository

import com.noonyuu.prc.domain.entity.User
import com.noonyuu.prc.domain.entity.UserId

interface UserRepository {
    suspend fun findById(id: UserId): User?
}
