package uk.brimstone.simpletype;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public final class BrimstoneMarkView extends View {
    private static final int PURPLE = Color.rgb(87, 20, 112);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BrimstoneMarkView(Context context) {
        super(context);
        init();
    }

    public BrimstoneMarkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setContentDescription("Brimstone logo");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(new RectF(left, top, left + size, top + size),
                size * 0.20f, size * 0.20f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, size * 0.025f));
        paint.setColor(PURPLE);
        canvas.drawCircle(cx, cy, size * 0.24f, paint);

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(2f, size * 0.018f));
        float inner = size * 0.025f;
        float outer = size * 0.215f;
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0 - 90.0);
            float x1 = cx + (float) Math.cos(angle) * inner;
            float y1 = cy + (float) Math.sin(angle) * inner;
            float x2 = cx + (float) Math.cos(angle) * outer;
            float y2 = cy + (float) Math.sin(angle) * outer;
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, size * 0.035f, paint);
    }
}
