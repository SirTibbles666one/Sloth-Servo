package com.sloth.servo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Renders the concept screen on the JVM.
 *
 * <p>The phone is the only real renderer, but a concept that cannot be looked at before it
 * is installed is a concept nobody can argue with. This draws the activity to a bitmap in
 * the same native graphics mode the browser's harness uses, so the picture is of the actual
 * view tree rather than a mock.</p>
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = 34)
public class ConceptRenderTest {

    @Test
    public void rendersTheConcept() throws Exception {
        // the JVM has no arm64 library to load, so the card is told what the phone shows
        Engine.preview("rustc 0.1.0 / aarch64 / aarch64-linux-android",
                "SLOTH SERVO (11 chars, from Rust)");

        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        View root = activity.getWindow().getDecorView();
        int w = 1233;
        int h = 2673;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        ShadowLooper.idleMainLooper();

        File closed = snap(root, w, h, "servo-01-closed.png");
        assertTrue("the closed state rendered to " + closed, closed.length() > 10_000);

        // the sheet, as the menu opens it
        View menu = find(root, "Open the dashboard");
        assertNotNull("the menu control exists", menu);
        menu.performClick();
        ShadowLooper.idleMainLooper();
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        File open = snap(root, w, h, "servo-02-open.png");
        assertTrue("the open state rendered to " + open, open.length() > 10_000);
    }
    private File snap(View root, int w, int h, String name) throws Exception {
        Bitmap shot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(shot));
        File out = new File("/opt/data/cache/scratch/shots/" + name);
        out.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(out)) {
            shot.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        return out;
    }

    private View find(View root, String description) {
        if (description.contentEquals(String.valueOf(root.getContentDescription()))) {
            return root;
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View hit = find(group.getChildAt(i), description);
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }
}
