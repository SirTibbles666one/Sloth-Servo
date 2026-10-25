package com.sloth.servo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * The controls, drawn rather than typed.
 *
 * <p>A text glyph carries its own bearings, weight and baseline, so a row of them never
 * lines up. These share one stroke width, one cap and one optical size.</p>
 */
final class Icons extends View {

    enum Kind { BACK, FORWARD, RELOAD, MENU, SEARCH, CLOSE }

    private final Kind kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private Icons(Context context, Kind kind) {
        super(context);
        this.kind = kind;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Theme.INK);
        setWillNotDraw(false);
    }

    static Icons of(Context context, Kind kind, float sizeDp, int colour) {
        Icons icon = new Icons(context, kind);
        paint(icon, colour);
        int size = Theme.px(context, sizeDp);
        icon.setLayoutParams(new android.widget.LinearLayout.LayoutParams(size, size));
        return icon;
    }

    private static void paint(Icons icon, int colour) {
        icon.paint.setColor(colour);
    }

    /** Recolour in place, for controls whose meaning changes with the page. */
    void tint(int colour) {
        paint.setColor(colour);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) * 0.5f;
        paint.setStrokeWidth(Math.max(2f, r * 0.17f));
        path.reset();

        switch (kind) {
            case BACK:
                path.moveTo(cx + r * 0.26f, cy - r * 0.44f);
                path.lineTo(cx - r * 0.30f, cy);
                path.lineTo(cx + r * 0.26f, cy + r * 0.44f);
                break;
            case FORWARD:
                path.moveTo(cx - r * 0.26f, cy - r * 0.44f);
                path.lineTo(cx + r * 0.30f, cy);
                path.lineTo(cx - r * 0.26f, cy + r * 0.44f);
                break;
            case RELOAD:
                path.addArc(cx - r * 0.62f, cy - r * 0.62f, cx + r * 0.62f, cy + r * 0.62f, 40f, 280f);
                path.moveTo(cx + r * 0.10f, cy - r * 0.70f);
                path.lineTo(cx + r * 0.52f, cy - r * 0.44f);
                path.lineTo(cx + r * 0.16f, cy - r * 0.20f);
                break;
            case MENU:
                for (int i = -1; i <= 1; i++) {
                    float y = cy + i * r * 0.40f;
                    path.moveTo(cx - r * 0.58f, y);
                    path.lineTo(cx + r * 0.58f, y);
                }
                break;
            case SEARCH:
                path.addCircle(cx - r * 0.10f, cy - r * 0.10f, r * 0.42f, Path.Direction.CW);
                path.moveTo(cx + r * 0.20f, cy + r * 0.20f);
                path.lineTo(cx + r * 0.52f, cy + r * 0.52f);
                break;
            case CLOSE:
                path.moveTo(cx - r * 0.40f, cy - r * 0.40f);
                path.lineTo(cx + r * 0.40f, cy + r * 0.40f);
                path.moveTo(cx + r * 0.40f, cy - r * 0.40f);
                path.lineTo(cx - r * 0.40f, cy + r * 0.40f);
                break;
            default:
                break;
        }
        canvas.drawPath(path, paint);
    }
}
