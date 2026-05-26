import Shared
import SwiftUI

struct UserView: View {
    @State private var model = UserModel()
    @State private var input = "1"

    var body: some View {
        VStack(spacing: 12) {
            TextField("User ID", text: $input)
                .textFieldStyle(.roundedBorder)
                .frame(maxWidth: 200)

            Button("Load") {
                Task { await model.load(id: input) }
            }

            if model.loading {
                ProgressView()
            } else if let error = model.error {
                Text(error).foregroundStyle(.red)
            } else if let user = model.user {
                VStack(alignment: .leading, spacing: 4) {
                    Text("ID: \(user.id.value)")
                    Text("Name: \(user.name)")
                    Text("Email: \(user.email)")
                }
            }
        }
        .padding()
    }
}

#Preview {
    UserView()
}
