package com.sloth.servo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Sloth Servo, concept build.
 *
 * <p>Two screens' worth of idea in one window. The bar is the handle: drag it down and the
 * sheet comes out, exactly as the browser does, because the point of this app is to find
 * out whether the same surface can be built in a tree we own outright. The sheet is where
 * the engine reports itself - the Rust build string and a JNI round trip, both of which
 * are real, and the empty page area, which is honest about what is not built yet.</p>
 */
public class MainActivity extends Activity {

    private FrameLayout root;
    private LinearLayout column;
    private FrameLayout sheet;
    private FrameLayout pageArea;
    private View bar;

    private int peekPx;
    private int fullPx;
    private float downY;
    private int startH;
    private boolean dragging;
    private final int slop;

    public MainActivity() {
        slop = 0; // replaced in onCreate: the value depends on the device's density
    }

    private int touchSlop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Theme.BG);
        getWindow().setNavigationBarColor(Theme.BG);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0B1512);

        pageArea = page();
        root.addView(pageArea, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));

        bar = bar();
        column.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0));

        sheet = sheet();
        column.addView(sheet, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0));

        setContentView(root);

        peekPx = Theme.px(this, 320);
        // measured after the first layout: the sheet may fill the window but never more
        root.post(new Runnable() {
            @Override
            public void run() {
                fullPx = root.getHeight() - bar.getHeight() - statusBar();
                showSheet(0);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (sheet.getLayoutParams().height > 0) {
            showSheet(0);
            return;
        }
        super.onBackPressed();
    }

    private int statusBar() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : Theme.px(this, 24);
    }

    // ------------------------------------------------------------------ page area

    private FrameLayout page() {
        FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(0xFFF4F6F3);

        LinearLayout notice = new LinearLayout(this);
        notice.setOrientation(LinearLayout.VERTICAL);
        notice.setGravity(Gravity.CENTER_HORIZONTAL);
        notice.setPadding(Theme.px(this, 28), 0, Theme.px(this, 28), 0);

        TextView chip = text("SLOTH SERVO · 0.1 · CONCEPT", 10f, 0xFF7C8B83, true);
        chip.setLetterSpacing(0.16f);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(0, 0, 0, Theme.px(this, 12));

        TextView title = text("Nothing is rendering yet", 18f, 0xFF1F2A24, true);
        title.setGravity(Gravity.CENTER);
        TextView body = text("This build proves the shell and the Rust bridge. The page area stays "
                + "empty until the engine is linked: no web view is pretending to be Servo.", 13f,
                0xFF4A5A52, false);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, Theme.px(this, 10), 0, 0);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        body.setLineSpacing(Theme.px(this, 3), 1f);

        notice.addView(chip);
        notice.addView(title);
        notice.addView(body, bodyLp);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        lp.topMargin = Theme.px(this, 80);
        page.addView(notice, lp);
        return page;
    }

    // ------------------------------------------------------------------------ bar

    private View bar() {
        final LinearLayout barRow = new LinearLayout(this) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return onBarTouch(event, this);
            }
        };
        barRow.setOrientation(LinearLayout.HORIZONTAL);
        barRow.setGravity(Gravity.CENTER_VERTICAL);
        barRow.setBackgroundColor(Theme.BG);
        barRow.setPadding(Theme.px(this, 6), Theme.px(this, 6), Theme.px(this, 6),
                Theme.px(this, 8));

        barRow.addView(chrome(Icons.Kind.BACK, "Back", Theme.MUTED));
        barRow.addView(chrome(Icons.Kind.FORWARD, "Forward", Theme.MUTED));
        barRow.addView(field(), new LinearLayout.LayoutParams(0,
                Theme.px(this, 40), 1f));
        barRow.addView(chrome(Icons.Kind.RELOAD, "Reload", Theme.INK));
        barRow.addView(chrome(Icons.Kind.MENU, "Open the dashboard", Theme.INK), menuLp());
        return barRow;
    }

    private LinearLayout.LayoutParams menuLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                Theme.px(this, 30), Theme.px(this, 30));
        lp.leftMargin = Theme.px(this, 10);
        lp.rightMargin = Theme.px(this, 4);
        return lp;
    }

    private View chrome(Icons.Kind kind, String label, int colour) {
        final Icons icon = Icons.of(this, kind, 30, colour);
        icon.setContentDescription(label);
        icon.setClickable(true);
        icon.setFocusable(true);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) icon.getLayoutParams();
        lp.leftMargin = Theme.px(this, 2);
        lp.rightMargin = Theme.px(this, 2);
        if (kind == Icons.Kind.MENU) {
            icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSheet(sheet.getLayoutParams().height > 0 ? 0 : peekPx);
                }
            });
        }
        return icon;
    }

    /** The address field: a search slot in this build, because nothing can be navigated to. */
    private View field() {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(Theme.px(this, 12), 0, Theme.px(this, 10), 0);
        pill.setBackground(Theme.round(this, 20, Theme.SURFACE, Theme.accentAt(0x2E)));

        pill.addView(Icons.of(this, Icons.Kind.SEARCH, 15, Theme.MUTED),
                new LinearLayout.LayoutParams(Theme.px(this, 15), Theme.px(this, 15)));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        input.setTextColor(Theme.INK);
        input.setHintTextColor(Theme.MUTED);
        input.setHint("Search or open");
        input.setBackground(null);
        input.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.leftMargin = Theme.px(this, 9);
        pill.addView(input, lp);
        return pill;
    }

    /**
     * The bar is the handle. A drag is claimed only once it is unmistakably vertical, so a
     * tap in the field still types - the same rule the browser uses, written down where a
     * reader can argue with it.
     */
    private boolean onBarTouch(MotionEvent event, View target) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getY();
                startH = sheet.getLayoutParams().height;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dy = event.getY() - downY;
                if (!dragging && Math.abs(dy) > touchSlop) {
                    dragging = true;
                    startH = Math.max(0, startH + Math.round(dy));
                    downY = event.getY();
                    dy = 0;
                }
                if (dragging) {
                    showSheet(startH + Math.round(dy));
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    snap();
                    dragging = false;
                }
                return true;
            default:
                return true;
        }
    }

    /** Three places to land: shut, the row of cards, and open. */
    private void snap() {
        int h = sheet.getLayoutParams().height;
        int[] seats = {0, peekPx, fullPx};
        int best = seats[0];
        for (int seat : seats) {
            if (Math.abs(seat - h) < Math.abs(best - h)) {
                best = seat;
            }
        }
        showSheet(best);
    }

    private void showSheet(int height) {
        int h = Math.max(0, Math.min(height, fullPx));
        ViewGroup.LayoutParams lp = sheet.getLayoutParams();
        lp.height = h;
        sheet.setLayoutParams(lp);
        sheet.setVisibility(h > 0 ? View.VISIBLE : View.GONE);

        int barH = Theme.px(this, 58) + statusBar();
        ViewGroup.LayoutParams barLp = bar.getLayoutParams();
        barLp.height = barH;
        bar.setLayoutParams(barLp);

        // the page dims a little as the sheet comes over it, so the sheet reads as a surface
        // in front rather than a hole in the page
        float over = Math.min(1f, h / (float) Math.max(1, peekPx));
        pageArea.setAlpha(1f - 0.18f * over);
    }

    // ---------------------------------------------------------------------- sheet

    private FrameLayout sheet() {
        FrameLayout holder = new FrameLayout(this);
        holder.setBackground(Theme.round(this, 26, Theme.SURFACE, Theme.accentAt(0x22)));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setPadding(Theme.px(this, 16), Theme.px(this, 14), Theme.px(this, 16),
                Theme.px(this, 16));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(header());
        content.addView(engineCard());
        content.addView(roadmap());
        content.addView(footNote());
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        holder.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return holder;
    }

    private View header() {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, 0, 0, Theme.px(this, 12));

        TextView word = text("Sloth", 22f, Theme.INK, true);
        TextView sub = text("SERVO · CONCEPT", 10.5f, Theme.MUTED, true);
        sub.setLetterSpacing(0.14f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.leftMargin = Theme.px(this, 8);
        subLp.bottomMargin = Theme.px(this, 3);

        LinearLayout mark = new LinearLayout(this);
        mark.setOrientation(LinearLayout.HORIZONTAL);
        mark.setGravity(Gravity.CENTER_VERTICAL);
        mark.addView(word);
        mark.addView(sub, subLp);
        head.addView(mark, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout close = new LinearLayout(this);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Theme.round(this, 999, 0, Theme.accentAt(0x33)));
        close.setContentDescription("Close the dashboard");
        close.setClickable(true);
        Icons cross = Icons.of(this, Icons.Kind.CLOSE, 13, Theme.INK);
        cross.setClickable(false);
        close.addView(cross, new LinearLayout.LayoutParams(Theme.px(this, 13),
                Theme.px(this, 13)));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSheet(0);
            }
        });
        head.addView(close, new LinearLayout.LayoutParams(Theme.px(this, 44),
                Theme.px(this, 44)));
        return head;
    }

    private View engineCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Theme.px(this, 14), Theme.px(this, 14), Theme.px(this, 14),
                Theme.px(this, 14));
        card.setBackground(Theme.round(this, 18, Theme.CARD, Theme.accentAt(0x1F)));

        boolean linked = Engine.linkedForDisplay();
        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(this);
        dot.setBackground(Theme.circle(linked ? Theme.ACCENT : Theme.AMBER, 0));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(Theme.px(this, 8),
                Theme.px(this, 8));
        dotLp.rightMargin = Theme.px(this, 8);
        status.addView(dot, dotLp);
        status.addView(text(linked ? "RUST BRIDGE LIVE" : "RUST BRIDGE MISSING", 10.5f,
                linked ? Theme.ACCENT : Theme.AMBER, true));
        card.addView(status);

        TextView build = text(Engine.describe(), 12.5f, Theme.DIM, false);
        build.setPadding(0, Theme.px(this, 10), 0, 0);
        build.setTextIsSelectable(true);
        card.addView(build);

        String echo = Engine.echo("sloth servo");
        TextView round = text(echo == null
                ? "The bridge did not answer: no JNI round trip in this build"
                : "JNI round trip: " + echo, 12f, Theme.MUTED, false);
        round.setPadding(0, Theme.px(this, 8), 0, 0);
        round.setTextIsSelectable(true);
        card.addView(round);
        return card;
    }

    private View roadmap() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Theme.px(this, 14), Theme.px(this, 14), Theme.px(this, 14),
                Theme.px(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Theme.px(this, 12);
        card.setLayoutParams(lp);
        card.setBackground(Theme.round(this, 18, Theme.CARD, Theme.accentAt(0x1F)));

        card.addView(label("WHAT IS BUILT, AND WHAT IS NOT"));
        card.addView(step("1", "The shell: bar, sheet, drag, palette", true));
        card.addView(step("2", "The Rust spine: JNI, build report, echo", Engine.linked()));
        card.addView(step("3", "The engine: verso surface handed to the page area", false));
        card.addView(step("4", "Tabs, history, settings - once there is a page", false));
        return card;
    }

    private View step(String number, String what, boolean done) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Theme.px(this, 7), 0, 0);

        TextView mark = text(number, 11f, done ? Theme.ACCENT : Theme.MUTED, true);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(Theme.px(this, 18),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        row.addView(mark, markLp);
        row.addView(text(what, 12.5f, done ? Theme.INK : Theme.DIM, false));
        return row;
    }

    private View footNote() {
        TextView note = text("A concept build, not a browser. Nothing here renders the web, and "
                + "nothing here pretends to.", 11.5f, Theme.MUTED, false);
        note.setLineSpacing(Theme.px(this, 2), 1f);
        note.setPadding(0, Theme.px(this, 14), 0, 0);
        return note;
    }

    // -------------------------------------------------------------------- helpers

    private TextView label(String what) {
        TextView view = text(what, 10f, Theme.MUTED, true);
        view.setLetterSpacing(0.14f);
        return view;
    }

    private TextView text(String what, float sizeSp, int colour, boolean bold) {
        TextView view = new TextView(this);
        view.setText(what);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(colour);
        if (bold) {
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
        }
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }
}
