import Foundation
import Shared

@Observable
final class UserModel {
    var loading = false
    var user: User? = nil
    var error: String? = nil

    private let getUser: GetUserUseCase

    init(getUser: GetUserUseCase = AppContainer.shared.getUserUseCase) {
        self.getUser = getUser
    }

    func load(id: String) async {
        loading = true
        error = nil
        defer { loading = false }

        do {
            let result = try await getUser.invoke(id: UserId(value: id))
            if let result {
                user = result
            } else {
                user = nil
                error = "User '\(id)' not found"
            }
        } catch {
            self.error = error.localizedDescription
        }
    }
}
