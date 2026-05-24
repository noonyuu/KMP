import Shared
import SwiftUI

struct ContentView: View {
    @State private var showContent: Bool

    init(showContent: Bool = false) {
        _showContent = State(initialValue: showContent)
    }

    var body: some View {
        VStack {
            Button("Click me!") {
                withAnimation {
                    showContent.toggle()
                }
            }

            if showContent {
                VStack(spacing: 16) {
                    Image(systemName: "swift")
                        .font(.system(size: 200))
                        .foregroundStyle(.tint)
                    Text("SwiftUI: \(Greeting().greet())")
                }
                .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding()
    }
}

#Preview("Initial") {
    ContentView()
}

#Preview("Content shown") {
    ContentView(showContent: true)
}
