package com.example.spendly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

public class PieChartView extends View {

    private Paint paintShopping;
    private Paint paintFood;
    private Paint paintText;
    private RectF rectF;
    private RectF innerRectF;

    private float shoppingPercentage = 60f; // 60%
    private float foodPercentage = 40f; // 40%
    private int totalExpense = 1000000;

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Paint for Shopping segment
        paintShopping = new Paint();
        paintShopping.setColor(ContextCompat.getColor(getContext(), R.color.pink_primary));
        paintShopping.setAntiAlias(true);

        // Paint for Food segment
        paintFood = new Paint();
        paintFood.setColor(ContextCompat.getColor(getContext(), R.color.orange_primary));
        paintFood.setAntiAlias(true);

        // Paint for center text
        paintText = new Paint();
        paintText.setColor(ContextCompat.getColor(getContext(), R.color.black));
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();
        innerRectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2 - 20;
        int centerX = width / 2;
        int centerY = height / 2;

        // Set up the rectangle for the pie chart
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // Draw Shopping segment (starts from top, 60%)
        float shoppingAngle = (shoppingPercentage / 100f) * 360f;
        canvas.drawArc(rectF, -90f, shoppingAngle, true, paintShopping);

        // Draw Food segment (40%)
        float foodAngle = (foodPercentage / 100f) * 360f;
        canvas.drawArc(rectF, -90f + shoppingAngle, foodAngle, true, paintFood);

        // Draw inner circle (donut hole)
        int innerRadius = radius - 60;
        innerRectF.set(centerX - innerRadius, centerY - innerRadius,
                centerX + innerRadius, centerY + innerRadius);

        Paint innerPaint = new Paint();
        innerPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        innerPaint.setAntiAlias(true);
        canvas.drawCircle(centerX, centerY, innerRadius, innerPaint);

        // Draw center text
        paintText.setTextSize(48f);
        paintText.setColor(ContextCompat.getColor(getContext(), R.color.black));
        canvas.drawText("Expenses", centerX, centerY - 10, paintText);

        paintText.setTextSize(56f);
        canvas.drawText("Rp" + formatNumber(totalExpense), centerX, centerY + 40, paintText);
    }

    private String formatNumber(int number) {
        return String.format("%,d", number).replace(",", ".");
    }

    public void setData(float shoppingPercentage, float foodPercentage, int totalExpense) {
        this.shoppingPercentage = shoppingPercentage;
        this.foodPercentage = foodPercentage;
        this.totalExpense = totalExpense;
        invalidate(); // Redraw the view
    }
}
