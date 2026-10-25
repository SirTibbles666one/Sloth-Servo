package com.sloth.servo;

/**
 * The Java side of the Rust bridge.
 *
 * <p>Everything that is not a view belongs on the other side of this class: filtering,
 * storage, parsing, and - the point of the project - the engine surface itself. Today it
 * carries a build string and an echo, which is enough to prove the library loads on the
 * phone's ABI and that a call makes the round trip. The next step is a `verso` surface
 * handed to {@link PageArea}.</p>
 *
 * <p>A missing library must not crash the app: a concept build that cannot load its own
 * native code should say so on screen, in the same place the engine would appear.</p>
 */
public final class Engine {

    private static final String LIBRARY = "sloth_servo_bridge";
    private static Boolean linked;
    private static String previewBuild;
    private static String previewEcho;

    private Engine() {
    }

    static native String nativeBuild();

    static native String nativeEcho(String text);

    static native int nativeReady();

    /** Whether the Rust library loaded. Asked once; the answer cannot change at runtime. */
    public static synchronized boolean linked() {
        if (linked == null) {
            try {
                System.loadLibrary(LIBRARY);
                linked = nativeReady() == 1;
            } catch (Throwable t) {
                // UnsatisfiedLinkError, or a bad ABI, or a missing symbol: all the same
                // to the person looking at the screen
                linked = false;
            }
        }
        return linked;
    }

    /**
     * A picture of the phone's screen, for the JVM renderer.
     *
     * <p>A test JVM cannot load an arm64 library, so a render made there would show the
     * "bridge missing" branch and misrepresent the build. This is only for that: on the
     * phone nothing sets it.</p>
     */
    static void preview(String build, String echo) {
        previewBuild = build;
        previewEcho = echo;
    }

    /** Whether to show the bridge as live: the renderer cannot load the library, so it says. */
    static boolean linkedForDisplay() {
        return previewBuild != null || linked();
    }

    /** What the app shows as its engine line. */
    public static String describe() {
        if (previewBuild != null) {
            return previewBuild;
        }
        if (!linked()) {
            return "Rust library not loaded (no " + LIBRARY + " for this ABI)";
        }
        try {
            String build = nativeBuild();
            return build == null ? "Rust library loaded, but it would not say what it is" : build;
        } catch (Throwable t) {
            return "Rust library loaded, call failed: " + t.getClass().getSimpleName();
        }
    }

    /** One round trip through JNI, used by the concept screen to prove the bridge. */
    public static String echo(String text) {
        if (previewEcho != null) {
            return previewEcho;
        }
        if (!linked()) {
            return null;
        }
        try {
            return nativeEcho(text == null ? "" : text);
        } catch (Throwable t) {
            return null;
        }
    }
}
