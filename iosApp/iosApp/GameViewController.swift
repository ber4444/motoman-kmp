import UIKit
import GLKit
import Motoman
import CoreMotion

class GameViewController: UIViewController, GLKViewDelegate {
    var host: IosGameHost?
    private var context: EAGLContext?
    private let motionManager = CMMotionManager()
    private var displayLink: CADisplayLink?

    override func viewDidLoad() {
        super.viewDidLoad()
        NSLog("GameViewController: viewDidLoad bounds=\\(self.view.bounds)")
        
        guard let context = EAGLContext(api: .openGLES2) else {
            fatalError("Failed to create OpenGLES 2.0 context")
        }
        self.context = context
        EAGLContext.setCurrent(context)
        
        let glkView = GLKView(frame: self.view.bounds, context: context)
        glkView.drawableDepthFormat = .format24
        glkView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        glkView.delegate = self
        self.view.addSubview(glkView)
        
        host = IosGameHost(debugGl: true)
        
        let scale = UIScreen.main.scale
        let width = Int32(self.view.bounds.width * scale)
        let height = Int32(self.view.bounds.height * scale)
        host?.create(widthPx: width, heightPx: height)
        
        startMotionUpdates()
        
        displayLink = CADisplayLink(target: self, selector: #selector(renderLoop))
        displayLink?.add(to: .main, forMode: .default)
    }
    
    private var lastSize: CGSize = .zero

    @objc private func renderLoop() {
        if let glkView = self.view.subviews.first(where: { $0 is GLKView }) as? GLKView {
            if glkView.bounds.size != lastSize && glkView.bounds.width > 0 && glkView.bounds.height > 0 {
                lastSize = glkView.bounds.size
                let scale = UIScreen.main.scale
                let width = Int32(glkView.bounds.width * scale)
                let height = Int32(glkView.bounds.height * scale)
                host?.resize(widthPx: width, heightPx: height)
            }
            glkView.display()
        }
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
        NSLog("GameViewController: viewDidLayoutSubviews bounds=\\(self.view.bounds)")
        if let glkView = self.view.subviews.first(where: { $0 is GLKView }) as? GLKView {
            glkView.frame = self.view.bounds
            let scale = UIScreen.main.scale
            let width = Int32(glkView.bounds.width * scale)
            let height = Int32(glkView.bounds.height * scale)
            host?.resize(widthPx: width, heightPx: height)
        }
    }

    func glkView(_ view: GLKView, drawIn rect: CGRect) {
        host?.render(dtSeconds: 1.0 / 60.0)
    }

    deinit {
        host?.dispose()
        if EAGLContext.current() == context {
            EAGLContext.setCurrent(nil)
        }
    }
}
