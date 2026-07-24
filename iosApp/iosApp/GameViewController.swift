import UIKit
import MetalANGLE
import Motoman

/// A GLKViewController-equivalent that owns the MetalANGLE context,
/// drives a CADisplayLink render loop, and initializes the game engine.
class GameViewController: UIViewController {
    var host: IosGameHost!
    private var mglContext: MGLContext!
    private var displayLink: CADisplayLink?
    private var lastTime: CFTimeInterval = 0
    private var lastDrawableSize: CGSize = .zero

    override func loadView() {
        host = IosGameHost(debugGl: true)
        let glView = MilkyWayGLView(frame: UIScreen.main.bounds, host: host)
        self.view = glView
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        let glView = self.view as! MilkyWayGLView
        let mglLayer = glView.layer as! MGLLayer

        // Create GL context and bind it to the layer before creating GL resources.
        mglContext = MGLContext(api: MGLRenderingAPI(rawValue: 2)) // GLES2
        let success = MGLContext.setCurrent(mglContext, for: mglLayer)
        if !success {
            fatalError("Failed to set MGLContext current in viewDidLoad")
        }

        // Initialize game engine with MetalANGLE's actual drawable size.
        let drawableSize = drawablePixelSize(for: mglLayer)
        let width = Int32(drawableSize.width)
        let height = Int32(drawableSize.height)
        NSLog("GameViewController: host.create width=\(width) height=\(height)")
        host.create(widthPx: width, heightPx: height)
        lastDrawableSize = drawableSize

        startRendering()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        resizeHostIfNeeded()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        stopRendering()
    }

    private func startRendering() {
        guard displayLink == nil else { return }
        lastTime = CACurrentMediaTime()
        displayLink = CADisplayLink(target: self, selector: #selector(onFrame))
        displayLink?.add(to: .main, forMode: .common)
    }

    private func stopRendering() {
        displayLink?.invalidate()
        displayLink = nil
    }

    private var frameCount = 0

    @objc private func onFrame() {
        if frameCount == 0 {
            NSLog("GameViewController: first onFrame")
        }
        frameCount += 1

        let now = CACurrentMediaTime()
        let dt = now - lastTime
        lastTime = now

        let mglLayer = self.view.layer as! MGLLayer
        MGLContext.setCurrent(mglContext, for: mglLayer)
        resizeHostIfNeeded()
        
        host.render(dtSeconds: Float(dt))
        
        mglContext.present(mglLayer)
    }

    private func resizeHostIfNeeded() {
        guard host != nil else { return }

        let mglLayer = view.layer as! MGLLayer
        let drawableSize = drawablePixelSize(for: mglLayer)
        let width = Int32(drawableSize.width)
        let height = Int32(drawableSize.height)
        guard width > 0, height > 0 else { return }

        guard drawableSize != lastDrawableSize else { return }

        NSLog("GameViewController: host.resize width=\(width) height=\(height)")
        host.resize(widthPx: width, heightPx: height)
        lastDrawableSize = drawableSize
    }

    private func drawablePixelSize(for layer: MGLLayer) -> CGSize {
        updateDrawableScale()

        let size = layer.drawableSize
        if size.width > 0, size.height > 0 {
            return CGSize(width: size.width.rounded(), height: size.height.rounded())
        }

        let scale = view.window?.screen.scale ?? UIScreen.main.scale
        return CGSize(
            width: (view.bounds.width * scale).rounded(),
            height: (view.bounds.height * scale).rounded()
        )
    }

    private func updateDrawableScale() {
        let scale = view.window?.screen.scale ?? UIScreen.main.scale
        view.contentScaleFactor = scale
        view.layer.contentsScale = scale
    }

    deinit {
        stopRendering()
    }
}

/// A UIView backed by MGLLayer for MetalANGLE rendering.
class MilkyWayGLView: UIView {
    private weak var host: IosGameHost?
    private var steeringTouch: UITouch?
    private var steeringStartX: CGFloat = 0

    override class var layerClass: AnyClass { return MGLLayer.self }

    init(frame: CGRect, host: IosGameHost) {
        self.host = host
        super.init(frame: frame)
        contentScaleFactor = UIScreen.main.scale
        layer.contentsScale = UIScreen.main.scale
        isMultipleTouchEnabled = false
        isUserInteractionEnabled = true
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) not supported") }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard steeringTouch == nil, let touch = touches.first else { return }

        steeringTouch = touch
        steeringStartX = touch.location(in: self).x
        host?.setTilt(steer: 0)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = steeringTouch, touches.contains(touch) else { return }

        let fullLock = max(bounds.width / 4, 1)
        let dx = touch.location(in: self).x - steeringStartX
        let steer = Float(max(-1, min(1, dx / fullLock)))
        host?.setTilt(steer: steer)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = steeringTouch, touches.contains(touch) else { return }
        resetSteeringTouch()
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let touch = steeringTouch, touches.contains(touch) else { return }
        resetSteeringTouch()
    }

    private func resetSteeringTouch() {
        steeringTouch = nil
        host?.setTilt(steer: 0)
    }
}
