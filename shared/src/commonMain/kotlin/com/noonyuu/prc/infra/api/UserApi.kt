package com.noonyuu.prc.infra.api

import com.noonyuu.prc.infra.dto.UserDto

interface UserApi {
    suspend fun fetch(id: String): UserDto?
}

class InMemoryUserApi : UserApi {
    private val users = mapOf(
        "1" to UserDto(id = "1", name = "Alice", email = "alice@example.com"),
        "2" to UserDto(id = "2", name = "Bob", email = "bob@example.com"),
    )

    override suspend fun fetch(id: String): UserDto? = users[id]
}
