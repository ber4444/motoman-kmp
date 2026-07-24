import SwiftUI
import Motoman

struct GameView: View {
    let host = IosGameHost(debugGl: true)

    var body: some View {
        ZStack {
            AngleViewRepresentable(host: host)
                .ignoresSafeArea()
            
            HudViewRepresentable(host: host)
                .ignoresSafeArea()
        }
    }
}

struct HudViewRepresentable: UIViewControllerRepresentable {
    let host: IosGameHost
    
    func makeUIViewController(context: Context) -> UIViewController {
        let vc = MainViewControllerKt.HudViewController(host: host)
        vc.view.backgroundColor = .clear // ensure the Compose HUD background is transparent!
        return vc
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // No updates needed
    }
}
