package com.noonyuu.prc.usecase

import com.noonyuu.prc.domain.entity.User
import com.noonyuu.prc.domain.entity.UserId
import com.noonyuu.prc.domain.repository.UserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetUserUseCaseTest {

    @Test
    fun returnsUserWhenRepositoryFindsOne() = runTest {
        val expected = User(UserId("42"), "Alice", "alice@example.com")
        val repository = FakeUserRepository(users = mapOf(UserId("42") to expected))
        val useCase = GetUserUseCase(repository)

        val actual = useCase(UserId("42"))

        assertEquals(expected, actual)
    }

    @Test
    fun returnsNullWhenRepositoryFindsNone() = runTest {
        val repository = FakeUserRepository(users = emptyMap())
        val useCase = GetUserUseCase(repository)

        val actual = useCase(UserId("missing"))

        assertNull(actual)
    }

    @Test
    fun passesUserIdThroughToRepository() = runTest {
        val repository = FakeUserRepository(users = emptyMap())
        val useCase = GetUserUseCase(repository)

        useCase(UserId("99"))

        assertEquals(listOf(UserId("99")), repository.calls)
    }
}

private class FakeUserRepository(
    private val users: Map<UserId, User>,
) : UserRepository {
    val calls = mutableListOf<UserId>()

    override suspend fun findById(id: UserId): User? {
        calls += id
        return users[id]
    }
}
