package com.example.banga;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    
    private GameThread gameThread;
    private boolean isPlaying = false;
    private int screenWidth, screenHeight;
    
    // Game objects
    private Player player;
    private List<Enemy> enemies;
    private List<Bullet> bullets;
    private List<Buff> buffs;

    // NEW: enemy bullets + explosions
    private List<EnemyBullet> enemyBullets;
    private List<Explosion> explosions;
    
    // Game variables
    private long lastEnemySpawn = 0;
    private long enemySpawnDelay = 2000; // 2 giây
    private Random random;
    private Paint paint;
    
    // Buff system
    private long lastBuffSpawn = 0;
    private long buffSpawnDelay = 6000; // spawn buff thường xuyên hơn (7 giây ban đầu)
    private boolean hasShield = false;
    private boolean hasAttackBuff = false;
    private long attackBuffEndTime = 0;
    private long shieldEndTime = 0; // reserved
    
    // Score
    private int score = 0;
    private boolean gameOver = false;
    
    // Lives system
    private int lives = 2; // số mạng ban đầu
    private long lastDamageTime = 0;
    private long damageCooldownMs = 1000; // bất tử ~1s sau khi trúng đòn (có nhấp nháy)
    
    // Sound
    private SoundPool soundPool;
    private int bulletSoundId;
    private int hitSoundId;
    private int warningSoundId;
    private boolean soundLoaded = false;
    private Context context;
    
    // Background music
    private MediaPlayer backgroundMusic;
    private boolean musicEnabled = true;
    
    // Sound effects control
    private boolean soundEffectsEnabled = true;
    
    // UI Buttons
    private GameButton musicButton;
    private GameButton soundButton;
    private boolean buttonsInitialized = false;
    
    // Warning system
    private long lastWarningTime = 0;
    private long warningCooldown = 500;
    private boolean playerAtEdge = false;
    
    // Background stars
    private List<Star> stars;
    private static class Star {
        float x, y, speed;
        int brightness;
        Star(float x, float y, float speed, int brightness) {
            this.x = x; this.y = y; this.speed = speed; this.brightness = brightness;
        }
    }
    
    public GameView(Context context) {
        super(context);
        this.context = context;
        getHolder().addCallback(this);
        gameThread = new GameThread(getHolder(), this);
        setFocusable(true);
        
        random = new Random();
        paint = new Paint();
        
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();
        buffs = new ArrayList<>();
        stars = new ArrayList<>();
        enemyBullets = new ArrayList<>();
        explosions = new ArrayList<>();
        
        initializeSoundPool();
        initializeBackgroundMusic();
    }
    
    private void initializeSoundPool() {
        try {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(audioAttributes)
                    .build();
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    if (status == 0) soundLoaded = true;
                }
            });
            bulletSoundId = soundPool.load(context, R.raw.bullet, 1);
            hitSoundId = soundPool.load(context, R.raw.hit, 1);
            warningSoundId = soundPool.load(context, R.raw.warning, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initializeBackgroundMusic() {
        try {
            backgroundMusic = MediaPlayer.create(context, R.raw.gamemusic);
            if (backgroundMusic != null) {
                backgroundMusic.setLooping(true);
                backgroundMusic.setVolume(0.5f, 0.5f);
                backgroundMusic.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {}
                });
                backgroundMusic.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) { return true; }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initializeButtons() {
        if (screenWidth > 0 && screenHeight > 0 && !buttonsInitialized) {
            float buttonSize = 80;
            float buttonY = screenHeight / 2 - buttonSize / 2;
            musicButton = new GameButton(context, 20, buttonY, buttonSize, buttonSize,
                    "music_turnon", "music_turnoff", musicEnabled);
            soundButton = new GameButton(context, screenWidth - buttonSize - 20, buttonY, buttonSize, buttonSize,
                    "sound_on", "sound_off", soundEffectsEnabled);
            buttonsInitialized = true;
        }
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        player = new Player(screenWidth / 2, screenHeight - 200);
        initializeButtons();
        createStars();
        isPlaying = true;
        gameThread = new GameThread(getHolder(), this);
        gameThread.setRunning(true);
        gameThread.start();
        startBackgroundMusic();
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                gameThread.setRunning(false);
                gameThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        cleanup();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();
            if (musicButton != null && musicButton.isPressed(touchX, touchY)) {
                musicButton.toggle();
                musicEnabled = musicButton.isEnabled();
                if (musicEnabled) startBackgroundMusic(); else pauseBackgroundMusic();
                return true;
            }
            if (soundButton != null && soundButton.isPressed(touchX, touchY)) {
                soundButton.toggle();
                soundEffectsEnabled = soundButton.isEnabled();
                return true;
            }
        }
        
        if (event.getAction() == MotionEvent.ACTION_DOWN || 
            event.getAction() == MotionEvent.ACTION_MOVE) {
            float touchX = event.getX();
            float touchY = event.getY();
            if (player != null) {
                if (touchX <= 50 || touchX >= screenWidth - 50 || 
                    touchY <= 50 || touchY >= screenHeight - 50) {
                    playerAtEdge = true;
                } else {
                    playerAtEdge = false;
                }
                // Đặt target để Player di chuyển có giới hạn tốc độ
                player.setTargetPosition(touchX, touchY);
                long shootDelay = hasAttackBuff ? 150 : 300;
                if (System.currentTimeMillis() - player.getLastShot() > shootDelay) {
                    bullets.add(new Bullet(player.getX(), player.getY() - 50));
                    player.setLastShot(System.currentTimeMillis());
                    playBulletSound();
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            playerAtEdge = false;
        }
        return true;
    }
    
    public void update() {
        if (!isPlaying) return;

        updateStars();
        playWarningIfAtEdge();

        if (player != null) {
            player.update();
        }

        // Duy trì tối thiểu 3 quái trên màn hình
        ensureMinimumEnemies(3);

        // Spawn thêm theo nhịp khó dần
        if (System.currentTimeMillis() - lastEnemySpawn > enemySpawnDelay) {
            spawnRandomEnemy();
            lastEnemySpawn = System.currentTimeMillis();
            if (enemySpawnDelay > 800) enemySpawnDelay -= 10;
        }

        // Update bullets (player)
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update();
            if (bullet.getY() < 0) bullets.remove(i);
        }

        // Update enemies: di chuyển theo loại + shooter bắn
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            enemy.update(player.getX(), player.getY());

            // SHOOTER bắn
            if (enemy.getType() == Enemy.Type.SHOOTER && enemy.shouldShoot()) {
                float sx = enemy.getX() + enemy.getWidth() / 2f;
                float sy = enemy.getY() + enemy.getHeight() / 2f;
                enemyBullets.add(new EnemyBullet(sx, sy, player.getX(), player.getY()));
            }

            // rơi khỏi màn hình -> respawn từ trên cùng
            if (enemy.getY() > screenHeight) {
                enemy.respawn((int)enemy.getX(), 0);
            }
        }

        // Update enemy bullets
        for (int i = enemyBullets.size() - 1; i >= 0; i--) {
            EnemyBullet eb = enemyBullets.get(i);
            eb.update();
            if (eb.getX() < -50 || eb.getX() > screenWidth + 50 ||
                eb.getY() < -50 || eb.getY() > screenHeight + 50) {
                enemyBullets.remove(i);
            }
        }

        // Update buffs
        if (System.currentTimeMillis() - lastBuffSpawn > buffSpawnDelay) {
            int buffX = random.nextInt(screenWidth - 40);
            Buff.BuffType buffType = random.nextBoolean() ? Buff.BuffType.SHIELD : Buff.BuffType.ATTACK;
            buffs.add(new Buff(buffX, 0, buffType));
            lastBuffSpawn = System.currentTimeMillis();
            buffSpawnDelay = 4000 + random.nextInt(4000); // 4-8 giây giữa các lần spawn
        }
        for (int i = buffs.size() - 1; i >= 0; i--) {
            Buff buff = buffs.get(i);
            buff.update();
            if (buff.getY() > screenHeight || buff.isExpired()) {
                buffs.remove(i);
            }
        }

        // Hết hạn attack buff
        if (hasAttackBuff && System.currentTimeMillis() > attackBuffEndTime) {
            hasAttackBuff = false;
        }

        // Va chạm
        checkCollisions();

        // Clear explosions đã kết thúc
        for (int i = explosions.size() - 1; i >= 0; i--) {
            if (explosions.get(i).isFinished()) {
                explosions.remove(i);
            }
        }
    }

    private void ensureMinimumEnemies(int minCount) {
        while (enemies.size() < minCount) {
            spawnRandomEnemy();
        }
    }

    private void spawnRandomEnemy() {
        int enemyX = random.nextInt(Math.max(1, screenWidth - 100));
        // Random loại: BASIC, FAST, DIVER, SHOOTER (ít hơn để cân bằng)
        int r = random.nextInt(100);
        Enemy.Type type;
        if (r < 40) type = Enemy.Type.BASIC;
        else if (r < 65) type = Enemy.Type.FAST;
        else if (r < 85) type = Enemy.Type.DIVER;
        else type = Enemy.Type.SHOOTER;
        enemies.add(new Enemy(enemyX, 0, type));
    }

    // Xác suất rơi buff khi quái nổ (thường xuyên)
    private void maybeDropBuffAt(float centerX, float centerY) {
        // 25% rơi buff (giảm tần suất)
        if (random.nextFloat() < 0.25f) {
            Buff.BuffType buffType = random.nextBoolean() ? Buff.BuffType.SHIELD : Buff.BuffType.ATTACK;
            // Điều chỉnh vị trí để vẽ từ góc trái trên của buff
            float bx = Math.max(0, Math.min(centerX - 20, screenWidth - 40));
            float by = Math.max(0, centerY - 20);
            buffs.add(new Buff(bx, by, buffType));
        }
    }
    
    private void checkCollisions() {
        // Đạn người chơi vs quái
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            Rect bulletRect = new Rect((int)bullet.getX() - 5, (int)bullet.getY() - 10, 
                                     (int)bullet.getX() + 5, (int)bullet.getY() + 10);
            boolean bulletConsumed = false;

            // Va chạm: đạn người chơi vs đạn quái
            for (int k = enemyBullets.size() - 1; k >= 0; k--) {
                EnemyBullet eb = enemyBullets.get(k);
                // Xấp xỉ: kiểm tra khoảng cách giữa tâm đạn quái và đầu đạn người chơi
                float bx = bullet.getX();
                float by = bullet.getY() - bullet.getHeight() / 2f;
                float ex = eb.getX();
                float ey = eb.getY();
                float dx = bx - ex;
                float dy = by - ey;
                float sumRadius = 6 + eb.getRadius();
                if (dx * dx + dy * dy <= sumRadius * sumRadius) {
                    explosions.add(new Explosion(ex, ey));
                    enemyBullets.remove(k);
                    bullets.remove(i);
                    bulletConsumed = true;
                    break;
                }
            }
            if (bulletConsumed) continue;
            for (int j = enemies.size() - 1; j >= 0; j--) {
                Enemy enemy = enemies.get(j);
                Rect enemyRect = new Rect((int)enemy.getX(), (int)enemy.getY(), 
                                        (int)enemy.getX() + enemy.getWidth(), 
                                        (int)enemy.getY() + enemy.getHeight());
                if (Rect.intersects(bulletRect, enemyRect)) {
                    // Nổ trên quái
                    explosions.add(new Explosion(
                            enemy.getX() + enemy.getWidth() / 2f,
                            enemy.getY() + enemy.getHeight() / 2f
                    ));
                    // Thường xuyên rơi buff khi quái nổ
                    maybeDropBuffAt(enemy.getX() + enemy.getWidth() / 2f,
                                    enemy.getY() + enemy.getHeight() / 2f);
                    bullets.remove(i);
                    enemies.remove(j);
                    score += 10;
                    playHitSound();
                    bulletConsumed = true;
                    break;
                }
            }
            if (bulletConsumed) {
                // đã remove bullet; tiếp theo i--
            }
        }

        if (player != null) {
            Rect playerRect = new Rect((int)player.getX() - player.getWidth()/2, 
                                     (int)player.getY() - player.getHeight()/2,
                                     (int)player.getX() + player.getWidth()/2, 
                                     (int)player.getY() + player.getHeight()/2);

            // Quái vs người chơi
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                Rect enemyRect = new Rect((int)enemy.getX(), (int)enemy.getY(), 
                                        (int)enemy.getX() + enemy.getWidth(), 
                                        (int)enemy.getY() + enemy.getHeight());
                if (Rect.intersects(playerRect, enemyRect)) {
                    if (hasShield) {
                        hasShield = false;
                        // Quái bị phá bởi shield -> có thể rơi buff
                        maybeDropBuffAt(enemy.getX() + enemy.getWidth() / 2f,
                                        enemy.getY() + enemy.getHeight() / 2f);
                        enemies.remove(i);
                        playHitSound();
                    } else {
                        handlePlayerDamaged();
                        enemies.remove(i);
                    }
                    break;
                }
            }

            // Đạn quái vs người chơi
            for (int i = enemyBullets.size() - 1; i >= 0; i--) {
                EnemyBullet eb = enemyBullets.get(i);
                // kiểm tra bằng hình tròn vs rect đơn giản
                Rect ebRect = new Rect(
                        (int)(eb.getX() - eb.getRadius()),
                        (int)(eb.getY() - eb.getRadius()),
                        (int)(eb.getX() + eb.getRadius()),
                        (int)(eb.getY() + eb.getRadius())
                );
                if (Rect.intersects(playerRect, ebRect)) {
                    enemyBullets.remove(i);
                    if (hasShield) {
                        hasShield = false;
                        playHitSound();
                        // nổ nhỏ trên đạn
                        explosions.add(new Explosion(eb.getX(), eb.getY()));
                    } else {
                        handlePlayerDamaged();
                    }
                    break;
                }
            }

            // Người chơi vs buff
            for (int i = buffs.size() - 1; i >= 0; i--) {
                Buff buff = buffs.get(i);
                Rect buffRect = new Rect((int)buff.getX(), (int)buff.getY(),
                                       (int)buff.getX() + buff.getWidth(),
                                       (int)buff.getY() + buff.getHeight());
                if (Rect.intersects(playerRect, buffRect)) {
                    activateBuff(buff.getType());
                    buffs.remove(i);
                    playHitSound();
                }
            }
        }
    }
    
    private void activateBuff(Buff.BuffType buffType) {
        switch (buffType) {
            case SHIELD:
                hasShield = true;
                break;
            case ATTACK:
                hasAttackBuff = true;
                attackBuffEndTime = System.currentTimeMillis() + 10000;
                break;
        }
    }

    // Giảm mạng người chơi và áp dụng bất tử ngắn
    private void handlePlayerDamaged() {
        long now = System.currentTimeMillis();
        if (now - lastDamageTime < damageCooldownMs) {
            return; // đang bất tử ngắn, bỏ qua sát thương
        }
        lastDamageTime = now;
        explosions.add(new Explosion(player.getX(), player.getY()));
        lives -= 1;
        if (lives <= 0) {
            gameOver = true;
            isPlaying = false;
        }
    }
    
    public void draw(Canvas canvas) {
        if (canvas == null) return;
        drawSpaceBackground(canvas);

        if (player != null) {
            // Hiệu ứng nhấp nháy khi đang bất tử sau khi dính đòn
            boolean invulnerable = (System.currentTimeMillis() - lastDamageTime) < damageCooldownMs;
            boolean shouldDraw = true;
            if (invulnerable) {
                // nhấp nháy ~10 lần/giây
                long t = System.currentTimeMillis();
                shouldDraw = ((t / 100) % 2) == 0;
            }
            if (shouldDraw) {
                player.draw(canvas, paint);
                if (hasShield) {
                    paint.setColor(Color.BLUE);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    canvas.drawCircle(player.getX(), player.getY(), player.getWidth() / 2 + 15, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }
        }

        for (Enemy enemy : enemies) {
            enemy.draw(canvas, paint);
        }

        for (Bullet bullet : bullets) {
            bullet.draw(canvas, paint);
        }

        // Vẽ đạn quái
        for (EnemyBullet eb : enemyBullets) {
            eb.draw(canvas, paint);
        }

        for (Buff buff : buffs) {
            buff.draw(canvas, paint);
        }

        // Vẽ hiệu ứng nổ
        for (Explosion ex : explosions) {
            ex.draw(canvas, paint);
        }

        if (musicButton != null) musicButton.draw(canvas, paint);
        if (soundButton != null) soundButton.draw(canvas, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        canvas.drawText("Score: " + score, 50, 100, paint);

        // Vẽ số mạng (trái tim)
        drawLives(canvas);

        int buffTextY = 160;
        paint.setTextSize(35);
        if (hasShield) {
            paint.setColor(Color.BLUE);
            canvas.drawText("SHIELD ACTIVE", 50, buffTextY, paint);
            buffTextY += 45;
        }
        if (hasAttackBuff) {
            paint.setColor(Color.RED);
            long remainingTime = (attackBuffEndTime - System.currentTimeMillis()) / 1000;
            canvas.drawText("ATTACK BOOST: " + remainingTime + "s", 50, buffTextY, paint);
            buffTextY += 45;
        }

        if (gameOver) {
            paint.setColor(Color.RED);
            paint.setTextSize(100);
            canvas.drawText("GAME OVER", screenWidth/2 - 250, screenHeight/2, paint);
            paint.setTextSize(50);
            canvas.drawText("Final Score: " + score, screenWidth/2 - 150, screenHeight/2 + 100, paint);
            paint.setTextSize(40);
            canvas.drawText("Nhấn BACK để quay lại menu", screenWidth/2 - 200, screenHeight/2 + 180, paint);
        }
    }
    
    public void resume() {
        isPlaying = true;
        gameThread = new GameThread(getHolder(), this);
        gameThread.setRunning(true);
        gameThread.start();
        startBackgroundMusic();
    }
    
    public void pause() {
        try {
            isPlaying = false;
            gameThread.setRunning(false);
            gameThread.join();
            pauseBackgroundMusic();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void playBulletSound() {
        try {
            if (soundPool != null && soundLoaded && soundEffectsEnabled) {
                soundPool.play(bulletSoundId, 0.3f, 0.3f, 1, 0, 1.0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void playHitSound() {
        try {
            if (soundPool != null && soundLoaded && soundEffectsEnabled) {
                soundPool.play(hitSoundId, 0.4f, 0.4f, 2, 0, 1.0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void playWarningSound() {
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWarningTime > warningCooldown) {
                if (soundPool != null && soundLoaded && soundEffectsEnabled) {
                    soundPool.play(warningSoundId, 0.5f, 0.5f, 3, 0, 1.0f);
                    lastWarningTime = currentTime;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void playWarningIfAtEdge() {
        if (playerAtEdge) {
            playWarningSound();
        }
    }
    
    public void cleanup() {
        stopBackgroundMusic();
        if (musicButton != null) musicButton.cleanup();
        if (soundButton != null) soundButton.cleanup();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
    
    private void createStars() {
        stars.clear();
        for (int i = 0; i < 100; i++) {
            float x = random.nextFloat() * screenWidth;
            float y = random.nextFloat() * screenHeight;
            float speed = 2 + random.nextFloat() * 6;
            int brightness = 100 + random.nextInt(156);
            stars.add(new Star(x, y, speed, brightness));
        }
    }
    
    private void updateStars() {
        for (Star star : stars) {
            star.y += star.speed;
            if (star.y > screenHeight) {
                star.y = 0;
                star.x = random.nextFloat() * screenWidth;
            }
        }
    }
    
    private void drawSpaceBackground(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        for (Star star : stars) {
            paint.setColor(Color.argb(star.brightness, 255, 255, 255));
            float starSize = star.speed / 2;
            canvas.drawCircle(star.x, star.y, starSize, paint);
            if (star.speed > 5) {
                paint.setColor(Color.argb(star.brightness / 3, 255, 255, 255));
                canvas.drawCircle(star.x, star.y - star.speed * 2, starSize / 2, paint);
            }
        }
    }

    // Vẽ trái tim biểu thị số mạng
    private void drawLives(Canvas canvas) {
        int heartSize = 40;
        int startX = 50;
        int y = 150;
        for (int i = 0; i < lives; i++) {
            drawHeart(canvas, startX + i * (heartSize + 10), y, heartSize, Color.RED);
        }
    }

    private void drawHeart(Canvas canvas, int cx, int cy, int size, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        float half = size / 2f;
        // hai hình tròn
        canvas.drawCircle(cx - half/2, cy - half/4, half/2, paint);
        canvas.drawCircle(cx + half/2, cy - half/4, half/2, paint);
        // tam giác dưới
        android.graphics.Path p = new android.graphics.Path();
        p.moveTo(cx - half, cy - half/4);
        p.lineTo(cx + half, cy - half/4);
        p.lineTo(cx, cy + half);
        p.close();
        canvas.drawPath(p, paint);
        paint.setStyle(Paint.Style.FILL);
    }
    
    private void startBackgroundMusic() {
        try {
            if (backgroundMusic != null && musicEnabled && !backgroundMusic.isPlaying()) {
                backgroundMusic.start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void pauseBackgroundMusic() {
        try {
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void stopBackgroundMusic() {
        try {
            if (backgroundMusic != null) {
                if (backgroundMusic.isPlaying()) {
                    backgroundMusic.stop();
                }
                backgroundMusic.release();
                backgroundMusic = null;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    public void toggleBackgroundMusic() {
        musicEnabled = !musicEnabled;
        if (musicEnabled) startBackgroundMusic(); else pauseBackgroundMusic();
    }
    
    public boolean isMusicEnabled() {
        return musicEnabled;
    }
    
    private static class GameButton {
        private float x, y, width, height;
        private boolean isEnabled;
        private Bitmap iconOn, iconOff;
        private Context context;
        public GameButton(Context context, float x, float y, float width, float height, 
                         String iconOnName, String iconOffName, boolean initialState) {
            this.context = context;
            this.x = x; this.y = y; this.width = width; this.height = height; this.isEnabled = initialState;
            loadIcons(iconOnName, iconOffName);
        }
        private void loadIcons(String iconOnName, String iconOffName) {
            try {
                int onResId = context.getResources().getIdentifier(iconOnName, "drawable", context.getPackageName());
                Bitmap originalOn = BitmapFactory.decodeResource(context.getResources(), onResId);
                iconOn = Bitmap.createScaledBitmap(originalOn, (int)width - 10, (int)height - 10, true);
                int offResId = context.getResources().getIdentifier(iconOffName, "drawable", context.getPackageName());
                Bitmap originalOff = BitmapFactory.decodeResource(context.getResources(), offResId);
                iconOff = Bitmap.createScaledBitmap(originalOff, (int)width - 10, (int)height - 10, true);
            } catch (Exception e) {
                e.printStackTrace();
                iconOn = Bitmap.createBitmap((int)width - 10, (int)height - 10, Bitmap.Config.ARGB_8888);
                iconOff = Bitmap.createBitmap((int)width - 10, (int)height - 10, Bitmap.Config.ARGB_8888);
            }
        }
        public boolean isPressed(float touchX, float touchY) {
            return touchX >= x && touchX <= x + width && touchY >= y && touchY <= y + height;
        }
        public void toggle() { isEnabled = !isEnabled; }
        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(Color.argb(100, 255, 255, 255));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(x, y, x + width, y + height, 10, 10, paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawRoundRect(x, y, x + width, y + height, 10, 10, paint);
            paint.setStyle(Paint.Style.FILL);
            Bitmap iconToDraw = isEnabled ? iconOn : iconOff;
            if (iconToDraw != null) {
                canvas.drawBitmap(iconToDraw, x + 5, y + 5, paint);
            }
        }
        public boolean isEnabled() { return isEnabled; }
        public void setEnabled(boolean enabled) { this.isEnabled = enabled; }
        public void cleanup() {
            if (iconOn != null && !iconOn.isRecycled()) iconOn.recycle();
            if (iconOff != null && !iconOff.isRecycled()) iconOff.recycle();
        }
    }
}