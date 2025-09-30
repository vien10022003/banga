package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Explosion {
    private float x, y;
    private long startTime;
    private long duration; // ms
    private float maxRadius;

    public Explosion(float x, float y) {
        this(x, y, 60f, 400);
    }

    public Explosion(float x, float y, float maxRadius, long duration) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
    }

    public boolean isFinished() {
        return System.currentTimeMillis() - startTime > duration;
    }

    public void draw(Canvas canvas, Paint paint) {
        float t = (System.currentTimeMillis() - startTime) / (float) duration;
        if (t < 0) t = 0;
        if (t > 1) t = 1;

        float radius = t * maxRadius;
        int alpha = (int) ((1 - t) * 255);

        // vòng lửa ngoài
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setColor(Color.argb(alpha, 255, 140, 0));
        canvas.drawCircle(x, y, radius, paint);

        // vòng lửa trong
        paint.setStrokeWidth(4);
        paint.setColor(Color.argb(alpha, 255, 200, 0));
        canvas.drawCircle(x, y, radius * 0.7f, paint);

        // lõi sáng
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(alpha, 255, 220, 120));
        canvas.drawCircle(x, y, Math.max(8, radius * 0.5f), paint);

        // tia nhỏ nhiều hơn
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        canvas.drawCircle(x + radius * 0.6f, y, 4, paint);
        canvas.drawCircle(x - radius * 0.6f, y, 4, paint);
        canvas.drawCircle(x, y + radius * 0.6f, 4, paint);
        canvas.drawCircle(x, y - radius * 0.6f, 4, paint);
        canvas.drawCircle(x + radius * 0.4f, y + radius * 0.4f, 3, paint);
        canvas.drawCircle(x - radius * 0.4f, y - radius * 0.4f, 3, paint);
    }
}