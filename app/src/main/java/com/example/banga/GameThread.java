package com.example.banga;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {
    
    private SurfaceHolder surfaceHolder;
    private GameView gameView;
    private boolean isRunning;
    private final int FPS = 60;
    private final long targetTime = 1000 / FPS;
    
    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }
    
    public void setRunning(boolean running) {
        this.isRunning = running;
    }
    
    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;
        
        while (isRunning) {
            startTime = System.nanoTime();
            Canvas canvas = null;
            
            try {
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    gameView.update();
                    gameView.draw(canvas);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            timeMillis = (System.nanoTime() - startTime) / 1000000;
            waitTime = targetTime - timeMillis;
            
            try {
                if (waitTime > 0) {
                    sleep(waitTime);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}