# Visual parity references

Captured on the same phone (Samsung Galaxy Z Fold 3, Adreno 660, Android 15).

- `original-device-galaxyzfold3.jpg` — the original 2013 libGDX game, from the prebuilt
  APK on the `pre-kmp-upstream-history` branch (`motoman-android/motoman-android.apk`),
  installed with `adb install --bypass-low-target-sdk-block` (it targets SDK 17).
- `port-device-galaxyzfold3.jpg` — the current KMP port on the same device.
- `original-vs-port-device.jpg` — the two stacked (original on top).

The 3D scene matches: cracked asphalt, yellow/black chevron barriers, the bike and rider,
lamp posts, the distant skyline billboards and hills, and the soft half-resolution look.
The HUD differs (the original has a minimap, on-screen D-pad, strength meter and FPS; the
port shows gear and speed) and is out of scope.

The deterministic desktop equivalent of this frame is the golden asset at
`motoman/src/desktopTest/resources/golden/start-line-desktop.png`, checked by
`GoldenSceneTest`.

Note: the original's libGDX/LWJGL 2 **desktop** build cannot run on this Apple Silicon
machine (its 2013 natives are x86-only and there is no x86 JDK installed), so the original
was captured on the phone, which is a better oracle anyway — same GPU and screen as the port.
