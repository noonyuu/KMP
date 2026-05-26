package com.noonyuu.prc.infra.repository

import com.noonyuu.prc.domain.entity.User
import com.noonyuu.prc.domain.entity.UserId
import com.noonyuu.prc.infra.api.UserApi
import com.noonyuu.prc.infra.dto.UserDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserRepositoryImplTest {

    @Test
    fun mapsDtoFromApiToEntity() = runTest {
        val api = FakeUserApi(
            responses = mapOf(
                "1" to UserDto(id = "1", name = "Alice", email = "alice@example.com"),
            ),
        )
        val repository = UserRepositoryImpl(api)

        val actual = repository.findById(UserId("1"))

        assertEquals(
            User(id = UserId("1"), name = "Alice", email = "alice@example.com"),
            actual,
        )
    }

    @Test
    fun returnsNullWhenApiReturnsNull() = runTest {
        val api = FakeUserApi(responses = emptyMap())
        val repository = UserRepositoryImpl(api)

        val actual = repository.findById(UserId("missing"))

        assertNull(actual)
    }

    @Test
    fun passesUserIdValueAsStringToApi() = runTest {
        val api = FakeUserApi(responses = emptyMap())
        val repository = UserRepositoryImpl(api)

        repository.findById(UserId("42"))

        assertEquals(listOf("42"), api.calls)
    }
}

private class FakeUserApi(
    private val responses: Map<String, UserDto>,
) : UserApi {
    val calls = mutableListOf<String>()

    override suspend fun fetch(id: String): UserDto? {
        calls += id
        return responses[id]
    }
}
