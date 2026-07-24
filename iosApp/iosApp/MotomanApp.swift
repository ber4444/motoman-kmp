import SwiftUI

@main
struct MotomanApp: App {
    var body: some Scene {
        WindowGroup {
            GameView()
                .ignoresSafeArea()
        }
    }
}
