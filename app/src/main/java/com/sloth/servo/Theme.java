package com.sloth.servo;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

/**
 * The palette and the metrics, in one place.
 *
 * <p>Sloth's ten palettes exist because the browser had to look like something. This app
 * starts with the one that reads best on a phone at night - moss - and keeps the same
 * relationships: a deep ground, a surface one step up, one accent used sparingly.</p>
 */
final class Theme {

    static final int BG = 0xFF0E1B16;
    static final int SURFACE = 0xFF15271F;
    static final int CARD = 0xFF1B322A;
    static final int INK = 0xFFF2F6F1;
    static final int DIM = 0xB3E6EFE6;
    static final int MUTED = 0x73C3D4C6;
    static final int ACCENT = 0xFF9FD36B;
    static final int AMBER = 0xFFE2A05A;

    private Theme() {
    }

    static int px(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static int sp(Context context, float sp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                context.getResources().getDisplayMetrics()));
    }

    static GradientDrawable round(Context context, float radiusDp, int fill, int stroke, int strokeWidthDp) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(px(context, radiusDp));
        d.setColor(fill);
        if (strokeWidthDp > 0) {
            d.setStroke(Math.max(1, px(context, strokeWidthDp)), stroke);
        }
        return d;
    }

    static GradientDrawable round(Context context, float radiusDp, int fill, int stroke) {
        return round(context, radiusDp, fill, stroke, 1);
    }

    /** A circle: the corner radius is larger than any view that will use it. */
    static GradientDrawable circle(int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        d.setStroke(1, stroke);
        return d;
    }

    /** The accent at a given alpha, for hairlines and quiet fills. */
    static int accentAt(int alpha) {
        return (alpha << 24) | (ACCENT & 0x00FFFFFF);
    }
}
