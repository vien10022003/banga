package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Bullet {
    
    private float x, y;
    private int width = 5;
    private int height = 15;
    private float speed = 15;
    
    public Bullet(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public void update() {
        // Di chuyển lên trên
        y -= speed;
    }
    
    public void draw(Canvas canvas, Paint paint) {
        // Vẽ đạn đơn giản
        paint.setColor(Color.YELLOW);
        canvas.drawRect(x - width/2, y - height, x + width/2, y, paint);
        
        // Thêm hiệu ứng ánh sáng
        paint.setColor(Color.WHITE);
        canvas.drawRect(x - 1, y - height, x + 1, y, paint);
    }
    
    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}