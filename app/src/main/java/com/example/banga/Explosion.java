package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Explosion {
    private float x, y;
    private long startTime;
    private long duration = 400; // ms
    private float maxRadius = 45f;

    public Explosion(float x, float y) {
        this.x = x;
        this.y = y;
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

        // vòng lửa
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        paint.setColor(Color.argb(alpha, 255, 140, 0));
        canvas.drawCircle(x, y, radius, paint);

        // lõi sáng
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(alpha, 255, 220, 120));
        canvas.drawCircle(x, y, Math.max(6, radius * 0.4f), paint);

        // tia nhỏ
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        canvas.drawCircle(x + radius * 0.6f, y, 3, paint);
        canvas.drawCircle(x - radius * 0.4f, y - radius * 0.2f, 3, paint);
        canvas.drawCircle(x, y + radius * 0.5f, 3, paint);
    }
}