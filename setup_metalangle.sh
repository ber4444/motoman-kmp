#!/bin/bash
set -e

mkdir -p third_party/MetalANGLE
cd third_party/MetalANGLE

# Download device framework
curl -sL "https://github.com/kakashidinho/metalangle/releases/download/gles3-0.0.7/MetalANGLE.framework.ios.zip" -o device.zip
unzip -q device.zip -d device/

# Download simulator framework
curl -sL "https://github.com/kakashidinho/metalangle/releases/download/gles3-0.0.7/MetalANGLE.framework.ios.simulator.zip" -o sim.zip
unzip -q sim.zip -d sim/

# Create XCFramework
xcodebuild -create-xcframework \
    -framework device/MetalANGLE.framework \
    -framework sim/MetalANGLE.framework \
    -output MetalANGLE.xcframework

# Create SPM Package wrapper
cat << 'EOF' > Package.swift
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
EOF

rm -rf device sim device.zip sim.zip
echo "MetalANGLE XCFramework and local SPM package created successfully!"
