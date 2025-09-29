package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class EnemyBullet {
    private float x, y;
    private float vx, vy;
    private int radius = 8;
    private float speed = 7f;

    public EnemyBullet(float startX, float startY, float targetX, float targetY) {
        this.x = startX;
        this.y = startY;
        float dx = targetX - startX;
        float dy = targetY - startY;
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len == 0) len = 1;
        vx = (dx / len) * speed;
        vy = (dy / len) * speed;
    }

    public void update() {
        x += vx;
        y += vy;
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(Color.RED);
        canvas.drawCircle(x, y, radius, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, Math.max(2, radius/3), paint);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public int getRadius() { return radius; }
}