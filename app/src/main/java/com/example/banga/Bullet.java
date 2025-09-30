package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Bullet {
    
    private float x, y;
    private int width = 5;
    private int height = 15;
    private float speed = 15;
    private boolean isExplosive = false;
    private float angle = (float) Math.PI / 2; // mặc định thẳng lên (90 độ)
    
    public Bullet(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public Bullet(float x, float y, boolean isExplosive) {
        this.x = x;
        this.y = y;
        this.isExplosive = isExplosive;
        if (isExplosive) {
            width = 20; // Tăng từ 8 lên 10
            height =50; // Tăng từ 20 lên 25
            speed = 12; // Chậm hơn một chút
        }
    }
    
    public Bullet(float x, float y, boolean isExplosive, float angle) {
        this.x = x;
        this.y = y;
        this.isExplosive = isExplosive;
        this.angle = angle;
        if (isExplosive) {
            width = 20;
            height = 50;
            speed = 12;
        }
    }
    
    public void update() {
        // Di chuyển theo hướng angle
        x += (float) Math.cos(angle) * speed;
        y -= (float) Math.sin(angle) * speed; // y tăng xuống dưới, nên trừ
    }
    
    public void draw(Canvas canvas, Paint paint) {
        if (isExplosive) {
            // Vẽ đạn nổ với màu cam
            paint.setColor(Color.rgb(255, 165, 0)); // Orange
            canvas.drawRect(x - width/2, y - height, x + width/2, y, paint);
            
            // Thêm hiệu ứng lửa nhỏ
            paint.setColor(Color.RED);
            canvas.drawCircle(x, y - height/2, 3, paint);
        } else {
            // Vẽ đạn thường
            paint.setColor(Color.YELLOW);
            canvas.drawRect(x - width/2, y - height, x + width/2, y, paint);
            
            // Thêm hiệu ứng ánh sáng
            paint.setColor(Color.WHITE);
            canvas.drawRect(x - 1, y - height, x + 1, y, paint);
        }
    }
    
    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isExplosive() { return isExplosive; }
}