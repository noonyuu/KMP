package com.noonyuu.prc.ui.user

import app.cash.turbine.test
import com.noonyuu.prc.domain.entity.User
import com.noonyuu.prc.domain.entity.UserId
import com.noonyuu.prc.domain.repository.UserRepository
import com.noonyuu.prc.usecase.GetUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emitsInitialThenLoadingThenSuccess() = runTest {
        val expected = User(UserId("1"), "Alice", "alice@example.com")
        val viewModel = UserViewModel(
            getUser = GetUserUseCase(FakeUserRepository(mapOf(UserId("1") to expected))),
        )

        viewModel.state.test {
            assertEquals(UserUiState(), awaitItem())

            viewModel.load("1")

            assertEquals(UserUiState(loading = true), awaitItem())
            assertEquals(UserUiState(user = expected), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emitsErrorWhenUserNotFound() = runTest {
        val viewModel = UserViewModel(
            getUser = GetUserUseCase(FakeUserRepository(emptyMap())),
        )

        viewModel.state.test {
            assertEquals(UserUiState(), awaitItem())

            viewModel.load("missing")

            assertEquals(UserUiState(loading = true), awaitItem())
            assertEquals(UserUiState(error = "User 'missing' not found"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearsPreviousErrorWhenReloading() = runTest {
        val expected = User(UserId("1"), "Alice", "alice@example.com")
        val viewModel = UserViewModel(
            getUser = GetUserUseCase(FakeUserRepository(mapOf(UserId("1") to expected))),
        )

        viewModel.state.test {
            assertEquals(UserUiState(), awaitItem())

            viewModel.load("missing")
            assertEquals(UserUiState(loading = true), awaitItem())
            assertEquals(UserUiState(error = "User 'missing' not found"), awaitItem())

            viewModel.load("1")
            assertEquals(UserUiState(loading = true), awaitItem())
            assertEquals(UserUiState(user = expected), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeUserRepository(
    private val users: Map<UserId, User>,
) : UserRepository {
    override suspend fun findById(id: UserId): User? {
        yield()
        return users[id]
    }
}
