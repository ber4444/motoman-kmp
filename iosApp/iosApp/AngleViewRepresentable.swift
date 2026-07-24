import SwiftUI
import MetalANGLE
import Motoman
import CoreMotion

struct AngleViewRepresentable: UIViewRepresentable {
    let host: IosGameHost

    func makeUIView(context: Context) -> MilkyWayGLView {
        let view = MilkyWayGLView(frame: .zero, host: host)
        return view
    }

    func updateUIView(_ uiView: MilkyWayGLView, context: Context) {
        // No updates needed
    }
}

class MilkyWayGLView: UIView {
    private var mglContext: MGLContext!
    private var displayLink: CADisplayLink?
    private let host: IosGameHost
    private let motionManager = CMMotionManager()
    private var lastTime: CFTimeInterval = 0

    init(frame: CGRect, host: IosGameHost) {
        self.host = host
        super.init(frame: frame)
        contentScaleFactor = UIScreen.main.scale
        layer.contentsScale = UIScreen.main.scale
        setup()
    }
    
    required init?(coder: NSCoder) { fatalError("init(coder:) not supported") }

    override class var layerClass: AnyClass { return MGLLayer.self }

    private func setup() {
        mglContext = MGLContext(api: MGLRenderingAPI(rawValue: 2)) // GLES2
        let mglLayer = self.layer as! MGLLayer
        MGLContext.setCurrent(mglContext, for: mglLayer)
        
        // Pass pixels, not points
        let scale = UIScreen.main.scale
        let width = Int32(bounds.width * scale)
        let height = Int32(bounds.height * scale)
        host.create(widthPx: width, heightPx: height)
        
        startMotionUpdates()
        startRendering()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let scale = UIScreen.main.scale
        let width = Int32(bounds.width * scale)
        let height = Int32(bounds.height * scale)
        host.resize(widthPx: width, heightPx: height)
    }

    private func startRendering() {
        lastTime = CACurrentMediaTime()
        displayLink = CADisplayLink(target: self, selector: #selector(onFrame))
        displayLink?.add(to: .main, forMode: .common)
    }

    @objc private func onFrame() {
        let now = CACurrentMediaTime()
        let dt = now - lastTime
        lastTime = now

        let mglLayer = self.layer as! MGLLayer
        MGLContext.setCurrent(mglContext, for: mglLayer)
        
        host.render(dtSeconds: Float(dt))
        
        mglContext.present(mglLayer)
    }

    private func startMotionUpdates() {
        if motionManager.isDeviceMotionAvailable {
            motionManager.deviceMotionUpdateInterval = 1.0 / 60.0
            motionManager.startDeviceMotionUpdates(to: .main) { [weak self] (motion, error) in
                guard let motion = motion else { return }
                let y = Float(motion.gravity.y)
                let steer = min(max(y / 5.0, -1.0), 1.0)
                self?.host.setTilt(steer: steer)
            }
        }
    }
    
    deinit {
        displayLink?.invalidate()
        host.dispose()
    }
}
