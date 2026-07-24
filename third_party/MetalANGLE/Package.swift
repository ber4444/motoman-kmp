// swift-tools-version:5.5
import PackageDescription

let package = Package(
    name: "MetalANGLE",
    platforms: [.iOS(.v14)],
    products: [
        .library(name: "MetalANGLE", targets: ["MetalANGLE"])
    ],
    targets: [
        .binaryTarget(
            name: "MetalANGLE",
            path: "MetalANGLE.xcframework"
        )
    ]
)
