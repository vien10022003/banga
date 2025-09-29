package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Buff {
    public enum BuffType {
        SHIELD,  // Buff giáp - miễn 1 lần sát thương
        ATTACK   // Buff tấn công - tăng tốc độ bắn x2
    }
    
    private float x, y;
    private float speed;
    private BuffType type;
    private int width = 40;
    private int height = 40;
    private long spawnTime;
    private static final long LIFETIME = 8000; // Buff tồn tại 8 giây trên màn hình
    
    public Buff(float x, float y, BuffType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.speed = 3; // Rơi chậm hơn enemy
        this.spawnTime = System.currentTimeMillis();
    }
    
    public void update() {
        y += speed;
    }
    
    public void draw(Canvas canvas, Paint paint) {
        // Vẽ buff với màu sắc khác nhau tùy loại
        switch (type) {
            case SHIELD:
                // Buff giáp màu xanh dương
                paint.setColor(Color.BLUE);
                canvas.drawRect(x, y, x + width, y + height, paint);
                
                // Vẽ ký hiệu shield (hình khiên)
                paint.setColor(Color.WHITE);
                paint.setTextSize(20);
                canvas.drawText("S", x + 15, y + 25, paint);
                break;
                
            case ATTACK:
                // Buff tấn công màu đỏ
                paint.setColor(Color.RED);
                canvas.drawRect(x, y, x + width, y + height, paint);
                
                // Vẽ ký hiệu attack (mũi tên lên)
                paint.setColor(Color.WHITE);
                paint.setTextSize(20);
                canvas.drawText("A", x + 15, y + 25, paint);
                break;
        }
        
        // Viền trắng để dễ nhìn
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRect(x, y, x + width, y + height, paint);
        paint.setStyle(Paint.Style.FILL);
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > LIFETIME;
    }
    
    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public BuffType getType() { return type; }
}