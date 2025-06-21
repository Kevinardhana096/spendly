package com.example.spendly.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CurvedBottomNavigationView extends BottomNavigationView {

    private Path path;
    private Paint paint;

    public CurvedBottomNavigationView(Context context) {
        super(context);
        init();
    }

    public CurvedBottomNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CurvedBottomNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        path = new Path();
        paint = new Paint();
        paint.setAntiAlias(true);
        setBackgroundColor(Color.TRANSPARENT);
        setElevation(8f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float fabRadius = 35f;
        float curveRadius = 50f;

        path.reset();

        // Start from left
        path.moveTo(0f, 30f);

        // Top left corner
        path.quadTo(0f, 0f, 30f, 0f);

        // Line to curve start
        path.lineTo(width / 2 - curveRadius, 0f);

        // Curve for FAB
        path.quadTo(width / 2 - fabRadius, 0f, width / 2 - fabRadius, fabRadius);

        RectF rectF = new RectF(
                width / 2 - fabRadius,
                -fabRadius,
                width / 2 + fabRadius,
                fabRadius
        );
        path.arcTo(rectF, 180f, -180f, false);

        path.quadTo(width / 2 + fabRadius, 0f, width / 2 + curveRadius, 0f);

        // Line to top right corner
        path.lineTo(width - 30f, 0f);

        // Top right corner
        path.quadTo(width, 0f, width, 30f);

        // Right side
        path.lineTo(width, height);

        // Bottom
        path.lineTo(0f, height);

        // Close path
        path.close();

        // Draw background
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);

        super.onDraw(canvas);
    }
}