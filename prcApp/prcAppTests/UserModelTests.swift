import XCTest
@testable import prcApp
import Shared

@MainActor
final class UserModelTests: XCTestCase {

    func testLoadsExistingUser() async {
        let expected = User(id: UserId(value: "1"), name: "Alice", email: "alice@example.com")
        let model = makeModel(users: ["1": expected])

        await model.load(id: "1")

        XCTAssertFalse(model.loading)
        XCTAssertEqual(model.user?.id.value, "1")
        XCTAssertEqual(model.user?.name, "Alice")
        XCTAssertEqual(model.user?.email, "alice@example.com")
        XCTAssertNil(model.error)
    }

    func testEmitsErrorWhenUserNotFound() async {
        let model = makeModel(users: [:])

        await model.load(id: "missing")

        XCTAssertFalse(model.loading)
        XCTAssertNil(model.user)
        XCTAssertEqual(model.error, "User 'missing' not found")
    }

    func testClearsPreviousErrorOnSuccessfulReload() async {
        let expected = User(id: UserId(value: "1"), name: "Alice", email: "alice@example.com")
        let model = makeModel(users: ["1": expected])

        await model.load(id: "missing")
        XCTAssertEqual(model.error, "User 'missing' not found")

        await model.load(id: "1")

        XCTAssertEqual(model.user?.id.value, "1")
        XCTAssertNil(model.error)
    }

    func testPassesUserIdToUseCase() async {
        let repository = FakeUserRepository(users: [:])
        let model = UserModel(getUser: GetUserUseCase(userRepository: repository))

        await model.load(id: "42")

        XCTAssertEqual(repository.calls, ["42"])
    }

    private func makeModel(users: [String: User]) -> UserModel {
        let repository = FakeUserRepository(users: users)
        return UserModel(getUser: GetUserUseCase(userRepository: repository))
    }
}

private final class FakeUserRepository: UserRepository {
    private let users: [String: User]
    var calls: [String] = []

    init(users: [String: User]) {
        self.users = users
    }

    func findById(id: UserId) async throws -> User? {
        calls.append(id.value)
        return users[id.value]
    }
}
