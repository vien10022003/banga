package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class Player {
    
    private float x, y;
    private int width = 60;
    private int height = 80;
    private long lastShot = 0;
    
    // Movement limiting
    private float targetX, targetY;
    private float maxSpeedPerUpdate = 12f; // giới hạn tốc độ mỗi khung hình
    
    public Player(float x, float y) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
    }
    
    public void update() {
        // Di chuyển dần về target để giới hạn tốc độ
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0) {
            if (dist <= maxSpeedPerUpdate) {
                x = targetX;
                y = targetY;
            } else {
                x += (dx / dist) * maxSpeedPerUpdate;
                y += (dy / dist) * maxSpeedPerUpdate;
            }
        }
    }
    
    public void draw(Canvas canvas, Paint paint) {
        // Vẽ máy bay đơn giản bằng hình tam giác và hình chữ nhật
        paint.setColor(Color.GREEN);
        
        // Thân máy bay
        canvas.drawRect(x - width/4, y - height/2, x + width/4, y + height/2, paint);
        
        // Đầu máy bay (tam giác)
        Path triangle = new Path();
        triangle.moveTo(x, y - height/2 - 20);
        triangle.lineTo(x - width/4, y - height/2);
        triangle.lineTo(x + width/4, y - height/2);
        triangle.close();
        canvas.drawPath(triangle, paint);
        
        // Cánh máy bay
        canvas.drawRect(x - width/2, y - 10, x - width/4, y + 10, paint);
        canvas.drawRect(x + width/4, y - 10, x + width/2, y + 10, paint);
        
        // Thêm chi tiết - động cơ
        paint.setColor(Color.BLUE);
        canvas.drawCircle(x - width/3, y + 15, 8, paint);
        canvas.drawCircle(x + width/3, y + 15, 8, paint);
        
        // Thêm ánh sáng
        paint.setColor(Color.WHITE);
        canvas.drawRect(x - 2, y - height/2, x + 2, y + height/2, paint);
    }
    
    public void setPosition(float x, float y) {
        // Giữ hàm cũ để tương thích, nhưng sẽ đặt target để áp dụng giới hạn tốc độ
        this.targetX = x;
        this.targetY = y;
    }
    
    public void setTargetPosition(float x, float y) {
        this.targetX = x;
        this.targetY = y;
    }
    
    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getLastShot() { return lastShot; }
    
    // Setters
    public void setLastShot(long lastShot) { this.lastShot = lastShot; }

    public void setMaxSpeedPerUpdate(float maxSpeedPerUpdate) { this.maxSpeedPerUpdate = maxSpeedPerUpdate; }
}