import UIKit
import GLKit
import Motoman
import CoreMotion

class GameViewController: GLKViewController {
    var host: IosGameHost?
    private var context: EAGLContext?
    private let motionManager = CMMotionManager()

    override func viewDidLoad() {
        super.viewDidLoad()
        
        guard let context = EAGLContext(api: .openGLES2) else {
            fatalError("Failed to create OpenGLES 2.0 context")
        }
        self.context = context
        EAGLContext.setCurrent(context)
        
        let view = self.view as! GLKView
        view.context = context
        view.drawableDepthFormat = .format24 // REQUIRED
        
        preferredFramesPerSecond = 60
        
        host = IosGameHost(debugGl: true)
        
        // Pass pixels, not points
        let scale = UIScreen.main.scale
        let width = Int32(view.bounds.width * scale)
        let height = Int32(view.bounds.height * scale)
        host?.create(widthPx: width, heightPx: height)
        
        startMotionUpdates()
    }
    
    private func startMotionUpdates() {
        if motionManager.isDeviceMotionAvailable {
            motionManager.deviceMotionUpdateInterval = 1.0 / 60.0
            motionManager.startDeviceMotionUpdates(to: .main) { [weak self] (motion, error) in
                guard let motion = motion else { return }
                // In landscape, we read the gravity along the Y axis of the device for steering
                let y = Float(motion.gravity.y)
                // Match Android's normalization
                let steer = min(max(y / 5.0, -1.0), 1.0)
                self?.host?.setTilt(steer: steer)
            }
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        let view = self.view as! GLKView
        let scale = UIScreen.main.scale
        let width = Int32(view.bounds.width * scale)
        let height = Int32(view.bounds.height * scale)
        host?.resize(widthPx: width, heightPx: height)
    }

    func glkViewControllerUpdate(_ controller: GLKViewController) {
        host?.render(dtSeconds: Float(timeSinceLastUpdate))
    }

    deinit {
        host?.dispose()
        if EAGLContext.current() == context {
            EAGLContext.setCurrent(nil)
        }
    }
}
