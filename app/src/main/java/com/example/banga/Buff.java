package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Buff {
    public enum BuffType {
        SHIELD,  // Buff giáp - miễn 1 lần sát thương
        ATTACK,   // Buff tấn công - tăng tốc độ bắn x2
        TRIPLE_SHOT,  // Buff bắn 3 tia
        EXPLOSIVE_BULLET,   // Buff đạn nổ
        NO_SPEED_LIMIT,  // Buff loại bỏ giới hạn tốc độ di chuyển
        IMMORTALITY,  // Buff bất tử với nhấp nháy liên tục
        WALL_SHIELD   // Buff tạo bức tường chặn đạn và kẻ địch
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
                
            case TRIPLE_SHOT:
                // Buff bắn 3 tia màu vàng
                paint.setColor(Color.YELLOW);
                canvas.drawRect(x, y, x + width, y + height, paint);
                
                // Vẽ ký hiệu triple shot (3 chấm)
                paint.setColor(Color.BLACK);
                paint.setTextSize(18);
                canvas.drawText("3", x + 15, y + 25, paint);
                break;
                
            case EXPLOSIVE_BULLET:
                // Buff đạn nổ màu cam
                paint.setColor(Color.rgb(255, 165, 0)); // Orange
                canvas.drawRect(x, y, x + width, y + height, paint);
                
                // Vẽ ký hiệu explosive (vòng tròn)
                paint.setColor(Color.WHITE);
                paint.setTextSize(20);
                canvas.drawText("E", x + 15, y + 25, paint);
                break;
                
            case NO_SPEED_LIMIT:
                // Buff loại bỏ giới hạn tốc độ màu tím
                paint.setColor(Color.MAGENTA);
                canvas.drawRect(x, y, x + width, y + height, paint);
                
                // Vẽ ký hiệu speed (mũi tên)
                paint.setColor(Color.WHITE);
                paint.setTextSize(20);
                canvas.drawText("S", x + 15, y + 25, paint);
                break;
                
            case IMMORTALITY:
                // Buff bất tử màu hồng
                paint.setColor(Color.rgb(255, 20, 147)); // Deep Pink
                canvas.drawRect(x, y, x + width, y + height, paint);
                
                // Vẽ ký hiệu immortality (vòng tròn với dấu +)
                paint.setColor(Color.WHITE);
                paint.setTextSize(18);
                canvas.drawText("I", x + 15, y + 25, paint);
                break;
                
            case WALL_SHIELD:
                // Buff tường chắn màu xanh dương nhạt
                paint.setColor(Color.CYAN);
                canvas.drawRect(x, y, x + width, y + height, paint);
                
                // Vẽ ký hiệu wall (hình vuông)
                paint.setColor(Color.BLACK);
                paint.setTextSize(20);
                canvas.drawText("W", x + 15, y + 25, paint);
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