# Sloth Servo — concept 0.1

A separate app, beside Sloth, built from source instead of patched. Same idea of a browser
surface — one bar that grows into a sheet — on a stack we own end to end, so that a real
Rust engine can be dropped into it without fighting a decompiled app.

**This is not a browser yet.** Nothing in this build fetches, parses or renders the web, and
the page area says so on screen rather than showing a WebView and hoping nobody looks.

## What is actually in it

- `com.sloth.servo` — a second package, so it installs alongside `com.sloth.browser`.
  Same signing key, `versionCode 1`, `0.1-concept`.
- **The shell**: a bar (back, forward, address field, reload, menu), and a sheet that the bar
  drags out — three resting sizes (shut, cards, open), snap on release, back closes it.
  Written as a normal view tree, not a patch over one.
- **The Rust spine**: `rust/bridge` is a `cdylib` crate built for `arm64-v8a` and `x86_64`
  with the NDK, packaged as `libsloth_servo_bridge.so`. It exports three JNI entry points and
  returns a real build report and a real echo round trip. Panics are caught at the boundary,
  because unwinding into the JVM aborts the process.
- **The honesty rule**: `Engine` catches `UnsatisfiedLinkError` and the card says
  `RUST BRIDGE MISSING` instead of crashing. On the phone the string comes from Rust.

## What is next, in order

1. **Link `verso`/`servo` into `rust/bridge`** and expose a surface handle. That is the whole
   project: everything else here is scaffolding for it.
2. Hand that surface to `MainActivity.page()`, which is a `FrameLayout` holding an empty-state
   notice today.
3. Tabs, history, settings — only once there is a page to have them about.

The blockers to know about before starting step 1: Servo's Android support is experimental,
it needs a specific NDK and Rust nightly-era toolchain, and the build is measured in tens of
minutes and multiple gigabytes. That is why this concept stops at the spine.

## Building

```sh
# Rust, both ABIs, into the app's jniLibs
export PATH=$HOME/.cargo/bin:$PATH
cd rust/bridge
cargo build --release --target aarch64-linux-android
cargo build --release --target x86_64-linux-android
mkdir -p ../../app/src/main/jniLibs/{arm64-v8a,x86_64}
cp target/aarch64-linux-android/release/libsloth_servo_bridge.so ../../app/src/main/jniLibs/arm64-v8a/
cp target/x86_64-linux-android/release/libsloth_servo_bridge.so ../../app/src/main/jniLibs/x86_64/

# the app
cd ../..
./gradlew assembleRelease
apksigner sign --ks <keystore> --out SlothServo.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

Linkers live in `.cargo/config.toml`; they point at the NDK r27c clang wrappers for API 26.
AGP 8.13, `compileSdk 36`, `minSdk 26`.

## Pictures

`app/src/test/java/com/sloth/servo/ConceptRenderTest.java` renders the closed and open states
on the JVM with Robolectric in native graphics mode, into `shots/servo-01-closed.png` and
`shots/servo-02-open.png`. The JVM cannot load an arm64 library, so the renderer uses
`Engine.preview(...)` to show the phone's state — that hook is the only test seam in the app.
