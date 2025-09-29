package com.example.banga;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.Random;

public class Enemy {
    
    public enum Type {
        BASIC,     // rơi thẳng
        FAST,      // rơi nhanh
        DIVER,     // lao theo hướng người chơi
        SHOOTER    // rơi thẳng + bắn đạn vào người chơi
    }

    private float x, y;
    private int width = 50;
    private int height = 60;
    private float speed;
    private int color;
    private float originalSpeed;
    private Type type;

    // Dùng cho SHOOTER
    private long lastShotTime = 0;
    private long shootCooldownMs;

    public Enemy(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;

        Random random = new Random();

        // Thiết lập theo loại
        switch (type) {
            case BASIC:
                this.speed = 3 + random.nextFloat() * 3; // 3-6
                this.color = Color.rgb(255, 165, 0); // cam
                this.shootCooldownMs = Long.MAX_VALUE; // không bắn
                this.width = 50; this.height = 60;
                break;
            case FAST:
                this.speed = 5 + random.nextFloat() * 4; // 5-9
                this.color = Color.MAGENTA; // hồng
                this.shootCooldownMs = Long.MAX_VALUE;
                this.width = 40; this.height = 50; // nhỏ, thon
                break;
            case DIVER:
                this.speed = 3.5f + random.nextFloat() * 3.5f; // 3.5-7
                this.color = Color.RED; // đỏ
                this.shootCooldownMs = Long.MAX_VALUE;
                this.width = 60; this.height = 60; // to, cân đối
                break;
            case SHOOTER:
                this.speed = 3 + random.nextFloat() * 3; // 3-6
                this.color = Color.CYAN; // xanh nhạt để dễ phân biệt
                this.shootCooldownMs = 1200 + random.nextInt(800); // 1.2s - 2.0s
                this.width = 55; this.height = 45; // thân drone ngang
                break;
        }
        this.originalSpeed = this.speed;
    }
    
    // Cập nhật vị trí: tất cả loại đều có xu hướng bay về phía người chơi (mức độ khác nhau)
    public void update(float targetX, float targetY) {
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        float dx = targetX - centerX;
        float dy = targetY - centerY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            dx /= len;
            dy /= len;
        }

        switch (type) {
            case BASIC: {
                // Rơi thẳng là chính, chỉnh nhẹ theo hướng người chơi
                y += speed;
                x += dx * speed * 0.2f;
                break;
            }
            case FAST: {
                // Rơi nhanh hơn và kéo mạnh hơn về phía người chơi
                y += speed * 1.7f;
                x += dx * speed * 0.4f;
                break;
            }
            case DIVER: {
                // Lao mạnh về phía người chơi
                x += dx * speed;
                y += dy * speed;
                break;
            }
            case SHOOTER: {
                // Vừa rơi vừa lượn theo hướng người chơi, giữ khoảng cách tương đối
                y += speed;
                x += dx * speed * 0.25f;
                break;
            }
        }
    }

    public boolean canShoot() {
        return type == Type.SHOOTER;
    }

    public boolean shouldShoot() {
        long now = System.currentTimeMillis();
        if (now - lastShotTime >= shootCooldownMs) {
            lastShotTime = now;
            return true;
        }
        return false;
    }
    
    public void respawn(float newX, float newY) {
        this.x = newX;
        this.y = newY;
        this.speed = this.originalSpeed;
    }
    
    public void draw(Canvas canvas, Paint paint) {
        switch (type) {
            case BASIC: {
                // Gà cơ bản: thân oval + đầu + mỏ + cánh
                paint.setColor(color);
                canvas.drawOval(x, y + 10, x + width, y + height - 10, paint);
                paint.setColor(Color.YELLOW);
                canvas.drawCircle(x + width/2f, y + 12, Math.min(12, width/4f), paint);
                paint.setColor(Color.YELLOW);
                Path beak = new Path();
                beak.moveTo(x + width/2f - 5, y + 5);
                beak.lineTo(x + width/2f + 5, y + 5);
                beak.lineTo(x + width/2f, y);
                beak.close();
                canvas.drawPath(beak, paint);
                paint.setColor(color);
                canvas.drawOval(x + 5, y + height/3f, x + 20, y + height/3f + 20, paint);
                canvas.drawOval(x + width - 20, y + height/3f, x + width - 5, y + height/3f + 20, paint);
                paint.setColor(Color.BLACK);
                canvas.drawCircle(x + width/2f - 4, y + 10, 2.5f, paint);
                canvas.drawCircle(x + width/2f + 4, y + 10, 2.5f, paint);
                break;
            }
            case FAST: {
                // Chim nhanh: thân tam giác nhọn xuống dưới
                paint.setColor(color);
                Path tri = new Path();
                float cx = x + width/2f;
                tri.moveTo(cx, y); // đỉnh trên
                tri.lineTo(x, y + height); // trái dưới
                tri.lineTo(x + width, y + height); // phải dưới
                tri.close();
                canvas.drawPath(tri, paint);
                // vạch trung tâm
                paint.setColor(Color.WHITE);
                canvas.drawLine(cx, y + 8, cx, y + height - 8, paint);
                break;
            }
            case DIVER: {
                // Dơi/lao: cánh cong hai bên + thân tròn
                paint.setColor(color);
                // thân
                canvas.drawCircle(x + width/2f, y + height/2f, Math.min(width, height)/4f, paint);
                // cánh trái
                Path wingL = new Path();
                wingL.moveTo(x + width/2f - 8, y + height/2f);
                wingL.quadTo(x + width*0.15f, y + height*0.15f, x, y + height/2f);
                wingL.quadTo(x + width*0.15f, y + height*0.85f, x + width/2f - 8, y + height/2f);
                canvas.drawPath(wingL, paint);
                // cánh phải
                Path wingR = new Path();
                wingR.moveTo(x + width/2f + 8, y + height/2f);
                wingR.quadTo(x + width*0.85f, y + height*0.15f, x + width, y + height/2f);
                wingR.quadTo(x + width*0.85f, y + height*0.85f, x + width/2f + 8, y + height/2f);
                canvas.drawPath(wingR, paint);
                // mắt
                paint.setColor(Color.WHITE);
                canvas.drawCircle(x + width/2f - 6, y + height/2f - 4, 3, paint);
                canvas.drawCircle(x + width/2f + 6, y + height/2f - 4, 3, paint);
                paint.setColor(Color.BLACK);
                canvas.drawCircle(x + width/2f - 6, y + height/2f - 4, 1.5f, paint);
                canvas.drawCircle(x + width/2f + 6, y + height/2f - 4, 1.5f, paint);
                break;
            }
            case SHOOTER: {
                // Drone bắn: thân chữ nhật bo góc + "nòng súng" phía dưới
                paint.setColor(color);
                float r = 12f;
                // bo góc thủ công bằng 4 cung tròn nhỏ + rect trung tâm
                // rect chính
                canvas.drawRect(x + r/2, y + r/2, x + width - r/2, y + height - r/2, paint);
                // nòng súng
                paint.setColor(Color.DKGRAY);
                canvas.drawRect(x + width/2f - 4, y + height - 6, x + width/2f + 4, y + height + 8, paint);
                // mắt cảm biến
                paint.setColor(Color.BLACK);
                canvas.drawCircle(x + width/2f, y + height/2f, 4, paint);
                // vạch trang trí
                paint.setColor(Color.WHITE);
                canvas.drawLine(x + 6, y + height/3f, x + width - 6, y + height/3f, paint);
                break;
            }
        }
    }
    
    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Type getType() { return type; }
}