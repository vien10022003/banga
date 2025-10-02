package com.example.banga;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
    private Wall wall; // Wall shield
    
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
    private boolean hasTripleShot = false;
    private boolean hasExplosiveBullet = false;
    private long attackBuffEndTime = 0;
    private long tripleShotEndTime = 0;
    private long explosiveBulletEndTime = 0;
    private long shieldEndTime = 0; // reserved
    private boolean hasSpeedBoost = false;
    private boolean hasImmortality = false;
    private boolean hasWallShield = false;
    private long speedBoostEndTime = 0;
    private long immortalityEndTime = 0;
    private long wallShieldEndTime = 0;
    
    // Score
    private int score = 0;
    private boolean gameOver = false;

    // Win condition variables
    private int enemiesKilled = 0;
    private static final int ENEMIES_TO_WIN = 50;
    private boolean gameWon = false;
    private int congratulationSoundId;
    private Bitmap congratulationsImage;
    
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

    // Game Over and Win UI buttons
    private Rect btnReplayRect;
    private Rect btnHomeRect;
    private Rect btnHighScoresRect;
    
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
        wall = null; // Initialize wall as null
        
        initializeSoundPool();
        initializeBackgroundMusic();
        loadCongratulationsAssets();
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
            congratulationSoundId = soundPool.load(context, R.raw.congratulation, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCongratulationsAssets() {
        try {
            // Load congratulations image
            congratulationsImage = BitmapFactory.decodeResource(getResources(), R.drawable.congratulations);
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

            // Handle Win screen buttons
            if (gameWon) {
                if (btnReplayRect != null && btnReplayRect.contains((int)touchX, (int)touchY)) {
                    restartGame();
                    return true;
                }
                if (btnHomeRect != null && btnHomeRect.contains((int)touchX, (int)touchY)) {
                    // Quay về MainActivity và clear tất cả activity khác
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
                    return true;
                }
                if (btnHighScoresRect != null && btnHighScoresRect.contains((int)touchX, (int)touchY)) {
                    Intent intent = new Intent(context, HighScoreActivity.class);
                    context.startActivity(intent);
                    return true;
                }
            }

            // Handle Game Over buttons
            if (gameOver) {
                if (btnReplayRect != null && btnReplayRect.contains((int)touchX, (int)touchY)) {
                    restartGame();
                    return true;
                }
                if (btnHomeRect != null && btnHomeRect.contains((int)touchX, (int)touchY)) {
                    // Quay về MainActivity và clear tất cả activity khác
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
                    return true;
                }
                if (btnHighScoresRect != null && btnHighScoresRect.contains((int)touchX, (int)touchY)) {
                    Intent intent = new Intent(context, HighScoreActivity.class);
                    context.startActivity(intent);
                    return true;
                }
            }

            // Only handle UI buttons when game is active
            if (!gameOver && !gameWon) {
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
        }
        // Only allow gameplay when game is active
        if (!gameOver && !gameWon && event.getAction() == MotionEvent.ACTION_DOWN || 
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
                    if (hasTripleShot) {
                        // Bắn 3 góc khác nhau: trái chéo, giữa, phải chéo
                        float centerAngle = (float) Math.PI / 2; // 90 độ, thẳng lên
                        float spread = (float) Math.toRadians(20); // 20 độ spread
                        bullets.add(new Bullet(player.getX(), player.getY() - 50, hasExplosiveBullet, centerAngle - spread));
                        bullets.add(new Bullet(player.getX(), player.getY() - 50, hasExplosiveBullet, centerAngle));
                        bullets.add(new Bullet(player.getX(), player.getY() - 50, hasExplosiveBullet, centerAngle + spread));
                    } else {
                        // Bắn 1 tia
                        bullets.add(new Bullet(player.getX(), player.getY() - 50, hasExplosiveBullet));
                    }
                    player.setLastShot(System.currentTimeMillis());
                    playBulletSound();
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            playerAtEdge = false;
        }
        return true;
    }
    
    private void restartGame() {
        gameOver = false;
        gameWon = false;
        lives = 2;
        score = 0;
        enemiesKilled = 0;
        isPlaying = true;
        
        // Clear all game objects
        enemies.clear();
        bullets.clear();
        buffs.clear();
        enemyBullets.clear();
        explosions.clear();
        
        // Reset buffs
        hasShield = false;
        hasAttackBuff = false;
        hasTripleShot = false;
        hasExplosiveBullet = false;
        hasSpeedBoost = false;
        hasImmortality = false;
        hasWallShield = false;
        wall = null;
        
        // Reset player position
        if (player != null) {
            player.setPosition(screenWidth / 2, screenHeight - 200);
        }
        
        // Reset spawn timers
        lastEnemySpawn = 0;
        lastBuffSpawn = 0;
        enemySpawnDelay = 2000;
        
        startBackgroundMusic();
    }
    
    public void update() {
        if (!isPlaying) return;

        updateStars();
        playWarningIfAtEdge();

        if (player != null) {
            // Set tốc độ player dựa trên buff
            if (hasSpeedBoost) {
                player.setMaxSpeedPerUpdate(2400f); // Tăng gấp đôi tốc độ
            } else {
                player.setMaxSpeedPerUpdate(12f); // Tốc độ bình thường
            }
            player.update();
            
            // Update wall position to follow player
            if (wall != null) {
                wall.setPosition(player.getX() - 50, player.getY() - 100);
            }
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
            if (bullet.getX() < -50 || bullet.getX() > screenWidth + 50 ||
                bullet.getY() < -50 || bullet.getY() > screenHeight + 50) {
                bullets.remove(i);
            }
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

        // Wall blocks enemy bullets
        if (wall != null) {
            for (int i = enemyBullets.size() - 1; i >= 0; i--) {
                EnemyBullet eb = enemyBullets.get(i);
                Rect ebRect = new Rect(
                        (int)(eb.getX() - eb.getRadius()),
                        (int)(eb.getY() - eb.getRadius()),
                        (int)(eb.getX() + eb.getRadius()),
                        (int)(eb.getY() + eb.getRadius())
                );
                if (wall.collidesWith(ebRect)) {
                    enemyBullets.remove(i);
                    explosions.add(new Explosion(eb.getX(), eb.getY()));
                }
            }
        }

        // Wall blocks enemies
        if (wall != null) {
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                Rect enemyRect = new Rect((int)enemy.getX(), (int)enemy.getY(), 
                                        (int)enemy.getX() + enemy.getWidth(), 
                                        (int)enemy.getY() + enemy.getHeight());
                if (wall.collidesWith(enemyRect)) {
                    enemies.remove(i);
                    explosions.add(new Explosion(enemy.getX() + enemy.getWidth() / 2f,
                                                enemy.getY() + enemy.getHeight() / 2f));
                    maybeDropBuffAt(enemy.getX() + enemy.getWidth() / 2f,
                                    enemy.getY() + enemy.getHeight() / 2f);
                    score += 10;
                    playHitSound();
                }
            }
        }

        // Update buffs
        if (System.currentTimeMillis() - lastBuffSpawn > buffSpawnDelay) {
            int buffX = random.nextInt(screenWidth - 40);
            Buff.BuffType buffType;
            int rand = random.nextInt(7); // 7 loại buff
            switch (rand) {
                case 0: buffType = Buff.BuffType.SHIELD; break;
                case 1: buffType = Buff.BuffType.ATTACK; break;
                case 2: buffType = Buff.BuffType.TRIPLE_SHOT; break;
                case 3: buffType = Buff.BuffType.EXPLOSIVE_BULLET; break;
                case 4: buffType = Buff.BuffType.NO_SPEED_LIMIT; break;
                case 5: buffType = Buff.BuffType.IMMORTALITY; break;
                case 6: buffType = Buff.BuffType.WALL_SHIELD; break;
                default: buffType = Buff.BuffType.SHIELD;
            }
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
        
        // Hết hạn triple shot buff
        if (hasTripleShot && System.currentTimeMillis() > tripleShotEndTime) {
            hasTripleShot = false;
        }
        
        // Hết hạn explosive bullet buff
        if (hasExplosiveBullet && System.currentTimeMillis() > explosiveBulletEndTime) {
            hasExplosiveBullet = false;
        }
        
        // Hết hạn speed boost buff
        if (hasSpeedBoost && System.currentTimeMillis() > speedBoostEndTime) {
            hasSpeedBoost = false;
        }
        
        // Hết hạn immortality buff
        if (hasImmortality && System.currentTimeMillis() > immortalityEndTime) {
            hasImmortality = false;
        }
        
        // Hết hạn wall shield buff
        if (hasWallShield && System.currentTimeMillis() > wallShieldEndTime) {
            hasWallShield = false;
            wall = null; // Remove wall
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
            Buff.BuffType buffType;
            int rand = random.nextInt(7);
            switch (rand) {
                case 0: buffType = Buff.BuffType.SHIELD; break;
                case 1: buffType = Buff.BuffType.ATTACK; break;
                case 2: buffType = Buff.BuffType.TRIPLE_SHOT; break;
                case 3: buffType = Buff.BuffType.EXPLOSIVE_BULLET; break;
                case 4: buffType = Buff.BuffType.NO_SPEED_LIMIT; break;
                case 5: buffType = Buff.BuffType.IMMORTALITY; break;
                case 6: buffType = Buff.BuffType.WALL_SHIELD; break;
                default: buffType = Buff.BuffType.SHIELD;
            }
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
                    if (bullet.isExplosive()) {
                        // Đạn nổ lớn hơn và gây sát thương vùng
                        explosions.add(new Explosion(
                                enemy.getX() + enemy.getWidth() / 2f,
                                enemy.getY() + enemy.getHeight() / 2f,
                                180f, 800 // Tăng radius từ 150 lên 180, duration từ 600 lên 800
                        ));
                        // Kiểm tra và gây sát thương cho các quái khác trong vùng nổ
                        float explosionX = enemy.getX() + enemy.getWidth() / 2f;
                        float explosionY = enemy.getY() + enemy.getHeight() / 2f;
                        for (int k = enemies.size() - 1; k >= 0; k--) {
                            if (k == j) continue; // bỏ qua enemy đã bị nổ
                            Enemy otherEnemy = enemies.get(k);
                            float dx = (otherEnemy.getX() + otherEnemy.getWidth()/2f) - explosionX;
                            float dy = (otherEnemy.getY() + otherEnemy.getHeight()/2f) - explosionY;
                            if (dx*dx + dy*dy <= 10000) { // bán kính 100
                                explosions.add(new Explosion(
                                        otherEnemy.getX() + otherEnemy.getWidth() / 2f,
                                        otherEnemy.getY() + otherEnemy.getHeight() / 2f
                                ));
                                maybeDropBuffAt(otherEnemy.getX() + otherEnemy.getWidth() / 2f,
                                                otherEnemy.getY() + otherEnemy.getHeight() / 2f);
                                enemies.remove(k);
                                // Calculate score based on enemy type and increase enemies killed
                                int pointsEarned = getPointsForEnemyType(otherEnemy.getType());
                                score += pointsEarned;
                                enemiesKilled++;
                            }
                        }
                    } else {
                        explosions.add(new Explosion(
                                enemy.getX() + enemy.getWidth() / 2f,
                                enemy.getY() + enemy.getHeight() / 2f
                        ));
                    }
                    // Thường xuyên rơi buff khi quái nổ
                    maybeDropBuffAt(enemy.getX() + enemy.getWidth() / 2f,
                                    enemy.getY() + enemy.getHeight() / 2f);
                    
                    // Calculate score based on enemy type and increase enemies killed
                    int pointsEarned = getPointsForEnemyType(enemy.getType());
                    score += pointsEarned;
                    enemiesKilled++;
                    
                    // Check win condition
                    if (enemiesKilled >= ENEMIES_TO_WIN && !gameWon) {
                        gameWon = true;
                        isPlaying = false;
                        playCongratulationSound();
                        pauseBackgroundMusic();
                        
                        // Save high score when game won
                        HighScoreManager highScoreManager = new HighScoreManager(context);
                        highScoreManager.saveScore(score);
                    }
                    
                    bullets.remove(i);
                    enemies.remove(j);
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
            case TRIPLE_SHOT:
                hasTripleShot = true;
                tripleShotEndTime = System.currentTimeMillis() + 15000; // 15 giây
                break;
            case EXPLOSIVE_BULLET:
                hasExplosiveBullet = true;
                explosiveBulletEndTime = System.currentTimeMillis() + 12000; // 12 giây
                break;
            case NO_SPEED_LIMIT:
                hasSpeedBoost = true;
                speedBoostEndTime = System.currentTimeMillis() + 10000; // 10 giây
                break;
            case IMMORTALITY:
                hasImmortality = true;
                immortalityEndTime = System.currentTimeMillis() + 8000; // 8 giây
                break;
            case WALL_SHIELD:
                hasWallShield = true;
                wallShieldEndTime = System.currentTimeMillis() + 12000; // 12 giây
                // Create wall in front of player
                if (player != null) {
                    float wallX = player.getX() - 50; // Left of player
                    float wallY = player.getY() - 100; // Above player
                    wall = new Wall(wallX, wallY, 100, 25); // Smaller wall: 100x25
                }
                break;
        }
    }

    // Giảm mạng người chơi và áp dụng bất tử ngắn
    private void handlePlayerDamaged() {
        // Nếu có immortality buff, không bị damage
        if (hasImmortality) {
            return;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastDamageTime < damageCooldownMs) {
            return; // đang bất tử ngắn, bỏ qua sát thương
        }
        lastDamageTime = now;
        explosions.add(new Explosion(player.getX(), player.getY()));
        lives -= 1;
        if (lives <= 0) {
            // Hiệu ứng nổ lớn khi game over
            explosions.add(new Explosion(player.getX(), player.getY(), 200f, 800));
            gameOver = true;
            isPlaying = false;
            
            // Save high score when game over
            HighScoreManager highScoreManager = new HighScoreManager(context);
            highScoreManager.saveScore(score);
            
            pauseBackgroundMusic();
        }
    }
    
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;
        drawSpaceBackground(canvas);

        if (player != null && !gameOver && !gameWon) {
            // Hiệu ứng nhấp nháy khi đang bất tử sau khi dính đòn hoặc có immortality buff
            boolean invulnerable = (System.currentTimeMillis() - lastDamageTime) < damageCooldownMs;
            boolean immortalityActive = hasImmortality;
            boolean shouldDraw = true;
            if (invulnerable || immortalityActive) {
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

        // Vẽ wall shield
        if (wall != null) {
            wall.draw(canvas, paint);
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
        if (hasTripleShot) {
            paint.setColor(Color.YELLOW);
            long remainingTime = (tripleShotEndTime - System.currentTimeMillis()) / 1000;
            canvas.drawText("TRIPLE SHOT: " + remainingTime + "s", 50, buffTextY, paint);
            buffTextY += 45;
        }
        if (hasExplosiveBullet) {
            paint.setColor(Color.rgb(255, 165, 0));
            long remainingTime = (explosiveBulletEndTime - System.currentTimeMillis()) / 1000;
            canvas.drawText("EXPLOSIVE BULLET: " + remainingTime + "s", 50, buffTextY, paint);
            buffTextY += 45;
        }
        if (hasSpeedBoost) {
            paint.setColor(Color.MAGENTA);
            long remainingTime = (speedBoostEndTime - System.currentTimeMillis()) / 1000;
            canvas.drawText("NO SPEED LIMIT: " + remainingTime + "s", 50, buffTextY, paint);
            buffTextY += 45;
        }
        if (hasImmortality) {
            paint.setColor(Color.rgb(255, 20, 147));
            long remainingTime = (immortalityEndTime - System.currentTimeMillis()) / 1000;
            canvas.drawText("IMMORTALITY: " + remainingTime + "s", 50, buffTextY, paint);
            buffTextY += 45;
        }
        if (hasWallShield) {
            paint.setColor(Color.CYAN);
            long remainingTime = (wallShieldEndTime - System.currentTimeMillis()) / 1000;
            canvas.drawText("WALL SHIELD: " + remainingTime + "s", 50, buffTextY, paint);
            buffTextY += 45;
        }

        if (!gameOver && !gameWon) {
            // Draw score and lives
            paint.setColor(Color.WHITE);
            paint.setTextSize(35);
            canvas.drawText("Score: " + score, 50, 50, paint);
            canvas.drawText("Lives: " + lives, 50, 100, paint);
            canvas.drawText("Enemies: " + enemiesKilled + "/" + ENEMIES_TO_WIN, 50, 150, paint);
            
            // Draw UI buttons if initialized
            if (musicButton != null) musicButton.draw(canvas, paint);
            if (soundButton != null) soundButton.draw(canvas, paint);
        }

        if (gameWon) {
            drawWinOverlay(canvas);
        } else if (gameOver) {
            drawGameOverOverlay(canvas);
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
    
    private void playCongratulationSound() {
        try {
            if (soundPool != null && soundLoaded && soundEffectsEnabled) {
                soundPool.play(congratulationSoundId, 0.8f, 0.8f, 1, 0, 1.0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private int getPointsForEnemyType(Enemy.Type type) {
        switch (type) {
            case BASIC: return 10;
            case FAST: return 15;
            case DIVER: return 20;
            case SHOOTER: return 25;
            default: return 10;
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
    
    private void drawWinOverlay(Canvas canvas) {
        // Draw semi-transparent overlay
        paint.setColor(Color.argb(150, 0, 100, 0)); // Dark green overlay
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        
        // Draw congratulations image if available
        if (congratulationsImage != null) {
            float imageWidth = congratulationsImage.getWidth();
            float imageHeight = congratulationsImage.getHeight();
            float scale = Math.min(screenWidth * 0.8f / imageWidth, screenHeight * 0.3f / imageHeight);
            
            int scaledWidth = (int)(imageWidth * scale);
            int scaledHeight = (int)(imageHeight * scale);
            
            Bitmap scaledImage = Bitmap.createScaledBitmap(congratulationsImage, scaledWidth, scaledHeight, true);
            float x = (screenWidth - scaledWidth) / 2f;
            float y = screenHeight / 2f - 200;
            
            canvas.drawBitmap(scaledImage, x, y, paint);
        } else {
            // Fallback text if image not available
            paint.setColor(Color.YELLOW);
            paint.setTextSize(80);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Congratulations!", screenWidth / 2f, screenHeight / 2f - 100, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }
        
        // Victory message
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("YOU WIN!", screenWidth / 2f, screenHeight / 2f + 50, paint);
        
        paint.setTextSize(40);
        canvas.drawText("Final Score: " + score, screenWidth / 2f, screenHeight / 2f + 100, paint);
        canvas.drawText("Enemies Defeated: " + enemiesKilled, screenWidth / 2f, screenHeight / 2f + 150, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        
        // Create buttons
        int buttonWidth = 180;
        int buttonHeight = 60;
        int buttonSpacing = 20;
        int totalButtonsWidth = 3 * buttonWidth + 2 * buttonSpacing;
        int startX = (screenWidth - totalButtonsWidth) / 2;
        int buttonY = screenHeight / 2 + 220;
        
        btnReplayRect = new Rect(startX, buttonY, startX + buttonWidth, buttonY + buttonHeight);
        btnHomeRect = new Rect(startX + buttonWidth + buttonSpacing, buttonY, 
                              startX + 2 * buttonWidth + buttonSpacing, buttonY + buttonHeight);
        btnHighScoresRect = new Rect(startX + 2 * (buttonWidth + buttonSpacing), buttonY,
                                    startX + 3 * buttonWidth + 2 * buttonSpacing, buttonY + buttonHeight);
        
        drawButton(canvas, btnReplayRect, "REPLAY", Color.GREEN);
        drawButton(canvas, btnHomeRect, "HOME", Color.CYAN);
        drawButton(canvas, btnHighScoresRect, "HIGH SCORES", Color.YELLOW);
    }
    
    private void drawGameOverOverlay(Canvas canvas) {
        // Draw semi-transparent overlay
        paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        
        // Game Over text
        paint.setColor(Color.RED);
        paint.setTextSize(100);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f - 50, paint);
        
        paint.setTextSize(50);
        paint.setColor(Color.WHITE);
        canvas.drawText("Final Score: " + score, screenWidth / 2f, screenHeight / 2f + 50, paint);
        canvas.drawText("Enemies Defeated: " + enemiesKilled, screenWidth / 2f, screenHeight / 2f + 100, paint);
        
        // Check and show if it's a new high score
        HighScoreManager highScoreManager = new HighScoreManager(context);
        if (highScoreManager.isNewHighScore(score)) {
            paint.setColor(Color.YELLOW);
            paint.setTextSize(36);
            canvas.drawText("NEW HIGH SCORE!", screenWidth / 2f, screenHeight / 2f + 150, paint);
        }
        
        // Show current highest score
        paint.setColor(Color.CYAN);
        paint.setTextSize(32);
        canvas.drawText("Best: " + highScoreManager.getHighestScore(), screenWidth / 2f, screenHeight / 2f + 190, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        
        // Create buttons (moved down to make room for high score info)
        int buttonWidth = 180;
        int buttonHeight = 60;
        int buttonSpacing = 20;
        int totalButtonsWidth = 3 * buttonWidth + 2 * buttonSpacing;
        int startX = (screenWidth - totalButtonsWidth) / 2;
        int buttonY = screenHeight / 2 + 250;
        
        btnReplayRect = new Rect(startX, buttonY, startX + buttonWidth, buttonY + buttonHeight);
        btnHomeRect = new Rect(startX + buttonWidth + buttonSpacing, buttonY, 
                              startX + 2 * buttonWidth + buttonSpacing, buttonY + buttonHeight);
        btnHighScoresRect = new Rect(startX + 2 * (buttonWidth + buttonSpacing), buttonY,
                                    startX + 3 * buttonWidth + 2 * buttonSpacing, buttonY + buttonHeight);
        
        drawButton(canvas, btnReplayRect, "REPLAY", Color.GREEN);
        drawButton(canvas, btnHomeRect, "HOME", Color.CYAN);
        drawButton(canvas, btnHighScoresRect, "HIGH SCORES", Color.YELLOW);
    }
    
    private void drawButton(Canvas canvas, Rect rect, String text, int color) {
        // Draw button background
        paint.setColor(Color.argb(180, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, 10, 10, paint);
        
        // Draw button border
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, 10, 10, paint);
        paint.setStyle(Paint.Style.FILL);
        
        // Draw button text
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        paint.setTextAlign(Paint.Align.CENTER);
        float textX = rect.centerX();
        float textY = rect.centerY() + 8; // Center vertically
        canvas.drawText(text, textX, textY, paint);
        paint.setTextAlign(Paint.Align.LEFT);
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
    
    private static class Wall {
        private float x, y, width, height;
        
        public Wall(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public void setPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }
        
        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(Color.CYAN);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(x, y, x + width, y + height, paint);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawRect(x, y, x + width, y + height, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        
        public Rect getRect() {
            return new Rect((int)x, (int)y, (int)(x + width), (int)(y + height));
        }
        
        public boolean collidesWith(Rect other) {
            return Rect.intersects(getRect(), other);
        }
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