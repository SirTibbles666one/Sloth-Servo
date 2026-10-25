//! The Rust spine of Sloth Servo.
//!
//! This crate is deliberately small. It exists to prove the part of the concept that has
//! to be true before any engine work starts: that a Rust library, built for the phone's
//! ABI, loads inside the app, answers a Java call, and can report what it is. The engine
//! adapter on the Java side asks this crate for its build string; if the string arrives,
//! the bridge is real and Servo has somewhere to live.
//!
//! Nothing here pretends to be a browser. There is no HTTP, no HTML, no rendering: the
//! next step is to link `servo`/`verso` into this crate and hand the app a surface.

use std::ffi::c_void;
use std::panic::{catch_unwind, AssertUnwindSafe};

use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring};
use jni::JNIEnv;

/// Rust panics must never cross the JNI boundary: unwinding into the JVM aborts the
/// process, and the browser would die with no Java stack to explain it.
fn guarded<F: FnOnce() -> String>(fallback: &str, work: F) -> String {
    match catch_unwind(AssertUnwindSafe(work)) {
        Ok(value) => value,
        Err(_) => format!("{fallback} (the Rust side panicked)"),
    }
}

fn build_report() -> String {
    format!(
        "rustc {} / {} / {}",
        env!("CARGO_PKG_VERSION"),
        std::env::consts::ARCH,
        TARGET,
    )
}

/// The triple this library was compiled for, baked in at build time.
pub const TARGET: &str = if cfg!(target_arch = "aarch64") {
    "aarch64-linux-android"
} else if cfg!(target_arch = "x86_64") {
    "x86_64-linux-android"
} else {
    "unknown-android"
};

/// `Engine.nativeBuild()` - what the app shows on its first screen.
#[no_mangle]
pub extern "system" fn Java_com_sloth_servo_Engine_nativeBuild(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let text = guarded("bridge unavailable", build_report);
    match env.new_string(text) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// `Engine.nativeEcho(String)` - proof the bridge carries data both ways.
#[no_mangle]
pub extern "system" fn Java_com_sloth_servo_Engine_nativeEcho(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let text: String = env
        .get_string(&input)
        .map(|s| s.into())
        .unwrap_or_else(|_| String::new());
    let answer = guarded("echo failed", || {
        // a small piece of real work, so the round trip is not a constant
        let chars = text.chars().count();
        let upper = text.to_uppercase();
        format!("{upper} ({chars} chars, from Rust)")
    });
    match env.new_string(answer) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// `Engine.nativeReady()` - 1 when this library is the spine, not a stub.
#[no_mangle]
pub extern "system" fn Java_com_sloth_servo_Engine_nativeReady(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    1
}

/// Kept so the linker cannot drop the symbol table entry the JVM looks for.
#[no_mangle]
pub extern "system" fn Java_com_sloth_servo_Engine_nativeHandle(
    _env: JNIEnv,
    _class: JClass,
) -> *mut c_void {
    std::ptr::null_mut()
}
