import UIKit
import Motoman

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)
        let gameViewController = GameViewController()
        let _ = gameViewController.view // Force viewDidLoad
        let rootViewController = MainViewControllerKt.MainViewController(
            gameViewController: gameViewController,
            host: gameViewController.host!
        )
        window.rootViewController = rootViewController
        self.window = window
        window.makeKeyAndVisible()
        return true
    }

}
