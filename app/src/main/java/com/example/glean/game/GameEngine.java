package com.example.glean.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.glean.R;
// import com.example.glean.model.GameScore; // TODO: Create GameScore model

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameEngine extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // Constants
    private static final int MAX_WASTE_ITEMS = 10;
    private static final long INITIAL_SPAWN_DELAY = 2000; // 2 seconds
    private static final String PREFS_NAME = "EcoSortifyGamePrefs";
    private static final String HIGH_SCORE_KEY = "highScore";

    // Game state
    private Thread gameThread;
    private boolean isRunning = false;
    private boolean gameOver = false;
    private boolean gamePaused = false;
    private int score = 0;
    private int lives = 3;
    private int level = 1;
    private int highScore = 0;
    
    // Game timing and difficulty
    private long lastItemSpawnTime = 0;
    private long spawnDelay = INITIAL_SPAWN_DELAY;
    private float baseSpeed = 4.0f;  // Base falling speed for level 1
    
    // Game objects
    private List<WasteItem> wasteItems = new ArrayList<>();
    private List<TrashBin> trashBins = new ArrayList<>();
    private Random random = new Random();
    
    // Graphics
    private Paint textPaint;
    private Paint scorePaint;
    private Paint livePaint;
    private Paint gameOverPaint;
    private Paint pauseOverlayPaint;
    
    // Sound
    private SoundPool soundPool;
    private int correctSound;
    private int wrongSound;
    private int levelUpSound;
    private int gameOverSound;
    private MediaPlayer bgMusic;
    
    // Touch handling
    private WasteItem draggedItem = null;
    private float dragOffsetX, dragOffsetY;
    
    // Context
    private Context context;
    private GameCallback callback;
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;

    private boolean surfaceReady = false;

    public interface GameCallback {
        void onGameOver(int score, int highScore);
        void onScoreChanged(int score);
    }

    public GameEngine(Context context, GameCallback callback) {
        super(context);
        this.context = context;
        this.callback = callback;
        
        // Get the holder and add callback
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        
        // Make focusable so it can handle events
        setFocusable(true);
        
        // Initialize paints
        initializePaints();
        
        // Initialize sound
        initializeSound();
        
        // Load high score
        loadHighScore();
    }

    private void initializePaints() {
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        scorePaint = new Paint();
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(50);
        scorePaint.setTextAlign(Paint.Align.LEFT);
        
        livePaint = new Paint();
        livePaint.setColor(Color.RED);
        livePaint.setTextSize(50);
        livePaint.setTextAlign(Paint.Align.RIGHT);
        
        gameOverPaint = new Paint();
        gameOverPaint.setColor(Color.BLACK);
        gameOverPaint.setTextSize(70);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
        
        pauseOverlayPaint = new Paint();
        pauseOverlayPaint.setColor(Color.argb(180, 0, 0, 0));
    }

    private void initializeSound() {
        // Set up SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        
        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build();          // Load sounds
        correctSound = soundPool.load(context, R.raw.correct, 1);
        wrongSound = soundPool.load(context, R.raw.wrong, 1);
        levelUpSound = soundPool.load(context, R.raw.level_up, 1);
        gameOverSound = soundPool.load(context, R.raw.game_over, 1);
        
        // Set up background music
        bgMusic = MediaPlayer.create(context, R.raw.game_music);
        if (bgMusic != null) {
            bgMusic.setLooping(true);
            bgMusic.setVolume(0.3f, 0.3f); // Reduced volume for better balance
        }
    }
      private void loadHighScore() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            highScore = prefs.getInt(HIGH_SCORE_KEY, 0);
            android.util.Log.d("GameEngine", "High score loaded: " + highScore);
        } catch (Exception e) {
            android.util.Log.e("GameEngine", "Error loading high score", e);
            highScore = 0;
        }
    }
      private void saveHighScore() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(HIGH_SCORE_KEY, highScore);
            editor.apply();
            android.util.Log.d("GameEngine", "High score saved: " + highScore);
        } catch (Exception e) {
            android.util.Log.e("GameEngine", "Error saving high score", e);
        }
    }    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        android.util.Log.d("GameEngine", "surfaceCreated() called");
        // Store screen dimensions
        screenWidth = getWidth();
        screenHeight = getHeight();
        
        android.util.Log.d("GameEngine", "Screen dimensions: " + screenWidth + "x" + screenHeight);
        
        // Create trash bins
        createTrashBins();
        
        // DO NOT auto-start game here - let user control it
        android.util.Log.d("GameEngine", "Surface created, waiting for user to start game");
        surfaceReady = true;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        // Update screen dimensions
        screenWidth = width;
        screenHeight = height;
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        // Stop game
        stopGame();
    }    public void startGame() {
        android.util.Log.d("GameEngine", "startGame() called - isRunning: " + isRunning + ", gamePaused: " + gamePaused + ", surfaceReady: " + surfaceReady);
        
        if (!surfaceReady) {
            android.util.Log.w("GameEngine", "Surface not ready yet, cannot start game");
            return;
        }
        
        if (!isRunning) {
            android.util.Log.d("GameEngine", "Starting new game...");
            isRunning = true;
            gameOver = false;
            gamePaused = false;
            gameThread = new Thread(this);
            gameThread.start();
            
            // Start music
            if (bgMusic != null && !bgMusic.isPlaying()) {
                bgMusic.start();
                android.util.Log.d("GameEngine", "Background music started");
            }
        } else if (gamePaused) {
            // Resume game
            android.util.Log.d("GameEngine", "Resuming paused game...");
            gamePaused = false;
            
            if (bgMusic != null && !bgMusic.isPlaying()) {
                bgMusic.start();
                android.util.Log.d("GameEngine", "Background music resumed");
            }
        }
    }

    public void pauseGame() {
        gamePaused = true;
        
        if (bgMusic != null && bgMusic.isPlaying()) {
            bgMusic.pause();
        }
    }

    public void stopGame() {
        isRunning = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Stop music
        if (bgMusic != null && bgMusic.isPlaying()) {
            bgMusic.stop();
        }
    }    @Override
    public void run() {
        android.util.Log.d("GameEngine", "Game loop started");
        
        while (isRunning) {
            try {
                // Skip updates if game is paused or over
                if (!gamePaused && !gameOver) {
                    update();
                }
                draw();
                control();
            } catch (Exception e) {
                android.util.Log.e("GameEngine", "Error in game loop", e);
                // Continue running even if there's an error
            }
        }
        
        android.util.Log.d("GameEngine", "Game loop ended");
    }

    private void update() {
        // Spawn new items
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastItemSpawnTime > spawnDelay && wasteItems.size() < MAX_WASTE_ITEMS) {
            spawnWasteItem();
            lastItemSpawnTime = currentTime;
        }
        
        // Update waste items (falling)
        Iterator<WasteItem> iterator = wasteItems.iterator();
        while (iterator.hasNext()) {
            WasteItem item = iterator.next();
            
            // Only move items that aren't being dragged
            if (item != draggedItem) {
                item.y += item.speed;
                  // Check if item is out of screen
                if (item.y > screenHeight) {
                    iterator.remove();
                    lives--;
                    
                    // Play wrong sound with null check
                    if (soundPool != null && wrongSound > 0) {
                        soundPool.play(wrongSound, 0.8f, 0.8f, 0, 0, 1);
                    }
                    
                    // Check for game over
                    if (lives <= 0) {
                        handleGameOver();
                    }
                }
            }
        }
        
        // Check for level up (every 100 points)
        int newLevel = 1 + (score / 100);
        if (newLevel > level) {
            levelUp(newLevel);
        }
    }

    private void levelUp(int newLevel) {
        level = newLevel;
        spawnDelay = Math.max(500, INITIAL_SPAWN_DELAY - (level - 1) * 200);
        baseSpeed += 0.5f;
          // Play level up sound with null check
        if (soundPool != null && levelUpSound > 0) {
            soundPool.play(levelUpSound, 0.9f, 0.9f, 0, 0, 1);
        }
    }

    private void draw() {
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas != null) {
                // Clear the canvas
                canvas.drawColor(Color.rgb(245, 252, 240)); // Light green background
                
                // Draw trash bins
                for (TrashBin bin : trashBins) {
                    canvas.drawBitmap(bin.bitmap, bin.x, bin.y, null);
                }
                
                // Draw waste items
                for (WasteItem item : wasteItems) {
                    canvas.drawBitmap(item.bitmap, item.x, item.y, null);
                }
                
                // Draw score and lives
                canvas.drawText("Skor: " + score, 20, 60, scorePaint);
                canvas.drawText("Level: " + level, screenWidth / 2, 60, textPaint);
                canvas.drawText("Nyawa: " + lives, screenWidth - 20, 60, livePaint);
                
                // Draw game over screen
                if (gameOver) {
                    canvas.drawRect(0, 0, screenWidth, screenHeight, pauseOverlayPaint);
                    canvas.drawText("GAME OVER", screenWidth / 2, screenHeight / 2 - 100, gameOverPaint);
                    canvas.drawText("Skor Akhir: " + score, screenWidth / 2, screenHeight / 2, gameOverPaint);
                    canvas.drawText("Skor Tertinggi: " + highScore, screenWidth / 2, screenHeight / 2 + 100, gameOverPaint);
                    canvas.drawText("Tap untuk main lagi", screenWidth / 2, screenHeight / 2 + 200, textPaint);
                }
                
                // Draw pause overlay
                if (gamePaused) {
                    canvas.drawRect(0, 0, screenWidth, screenHeight, pauseOverlayPaint);
                    canvas.drawText("GAME PAUSED", screenWidth / 2, screenHeight / 2, gameOverPaint);
                    canvas.drawText("Tap untuk lanjutkan", screenWidth / 2, screenHeight / 2 + 100, textPaint);
                }
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    private void control() {
        try {
            Thread.sleep(17); // ~60fps
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createTrashBins() {
        int binWidth = screenWidth / 3;
        int binHeight = screenHeight / 6;  // Make bin 1/6 of screen height
        int binY = screenHeight - binHeight;
        
        // Create organic bin (green)
        Bitmap organicBin = BitmapFactory.decodeResource(getResources(), R.drawable.bin_organic);
        organicBin = Bitmap.createScaledBitmap(organicBin, binWidth, binHeight, true);
        trashBins.add(new TrashBin(organicBin, 0, binY, WasteType.ORGANIC));
        
        // Create inorganic bin (blue)
        Bitmap inorganicBin = BitmapFactory.decodeResource(getResources(), R.drawable.bin_inorganic);
        inorganicBin = Bitmap.createScaledBitmap(inorganicBin, binWidth, binHeight, true);
        trashBins.add(new TrashBin(inorganicBin, binWidth, binY, WasteType.INORGANIC));
        
        // Create hazardous bin (red)
        Bitmap hazardousBin = BitmapFactory.decodeResource(getResources(), R.drawable.bin_hazardous);
        hazardousBin = Bitmap.createScaledBitmap(hazardousBin, binWidth, binHeight, true);
        trashBins.add(new TrashBin(hazardousBin, binWidth * 2, binY, WasteType.HAZARDOUS));
    }

    private void spawnWasteItem() {
        // Randomly choose waste type
        WasteType type = WasteType.values()[random.nextInt(WasteType.values().length)];
        
        // Select an image resource based on waste type
        int imageResource;
        switch (type) {
            case ORGANIC:
                imageResource = getRandomResource(organicImages);
                break;
            case HAZARDOUS:
                imageResource = getRandomResource(hazardousImages);
                break;
            case INORGANIC:
            default:
                imageResource = getRandomResource(inorganicImages);
                break;
        }
        
        // Create bitmap from resource
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imageResource);
        
        // Scale bitmap
        int itemSize = screenWidth / 8;  // 1/8 of screen width
        bitmap = Bitmap.createScaledBitmap(bitmap, itemSize, itemSize, true);
        
        // Calculate random position (leaving margin on sides)
        int margin = itemSize / 2;
        float x = margin + random.nextInt(screenWidth - itemSize - 2 * margin);
        
        // Calculate speed based on level
        float speed = baseSpeed + random.nextFloat() * 2;  // Add some randomness
        
        // Create waste item
        WasteItem item = new WasteItem(bitmap, x, -itemSize, type, speed);
        wasteItems.add(item);
    }

    private int getRandomResource(int[] resources) {
        return resources[random.nextInt(resources.length)];
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
          // Handle game over or paused state
        if (gameOver) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                resetGame();
                return true;
            }
        } else if (gamePaused) {
            // When game is paused, don't handle any touch events
            // Let the UI button handle resume
            return true;
        }
        
        // Handle waste item interactions
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if a waste item is touched
                for (int i = wasteItems.size() - 1; i >= 0; i--) {
                    WasteItem item = wasteItems.get(i);
                    if (isTouching(touchX, touchY, item)) {
                        draggedItem = item;
                        dragOffsetX = touchX - item.x;
                        dragOffsetY = touchY - item.y;
                        return true;
                    }
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                // Update dragged item position
                if (draggedItem != null) {
                    draggedItem.x = touchX - dragOffsetX;
                    draggedItem.y = touchY - dragOffsetY;
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
                // Release dragged item
                if (draggedItem != null) {
                    // Check if the item is dropped on a bin
                    for (TrashBin bin : trashBins) {
                        if (isOverlapping(draggedItem, bin)) {
                            handleWasteDisposal(draggedItem, bin);
                            draggedItem = null;
                            return true;
                        }
                    }
                    
                    // Not dropped on a bin, continue falling
                    draggedItem = null;
                }
                break;
        }
        
        return true;
    }

    private boolean isTouching(float x, float y, WasteItem item) {
        return x >= item.x && x <= item.x + item.bitmap.getWidth() &&
               y >= item.y && y <= item.y + item.bitmap.getHeight();
    }

    private boolean isOverlapping(WasteItem item, TrashBin bin) {
        // Check if the center of the item is over the bin
        float itemCenterX = item.x + item.bitmap.getWidth() / 2;
        return itemCenterX >= bin.x && 
               itemCenterX <= bin.x + bin.bitmap.getWidth() &&
               item.y + item.bitmap.getHeight() >= bin.y;
    }

    private void handleWasteDisposal(WasteItem item, TrashBin bin) {
        // Remove the item
        wasteItems.remove(item);
        
        // Check if correct bin
        boolean isCorrect = (item.type == bin.type);
        
        if (isCorrect) {
            // Increase score
            score += 10;
              // Play correct sound with null check
            if (soundPool != null && correctSound > 0) {
                soundPool.play(correctSound, 0.7f, 0.7f, 0, 0, 1);
            }
            
            // Notify callback
            if (callback != null) {
                callback.onScoreChanged(score);
            }
        } else {
            // Decrease lives
            lives--;
              // Play wrong sound with null check
            if (soundPool != null && wrongSound > 0) {
                soundPool.play(wrongSound, 0.8f, 0.8f, 0, 0, 1);
            }
            
            // Check for game over
            if (lives <= 0) {
                handleGameOver();
            }
        }
    }

    private void handleGameOver() {
        gameOver = true;
          // Play game over sound with null check
        if (soundPool != null && gameOverSound > 0) {
            soundPool.play(gameOverSound, 0.9f, 0.9f, 0, 0, 1);
        }
        
        // Stop background music
        if (bgMusic != null && bgMusic.isPlaying()) {
            bgMusic.pause();
        }
        
        // Update high score if needed
        if (score > highScore) {
            highScore = score;
            saveHighScore();
        }
        
        // Notify callback
        if (callback != null) {
            callback.onGameOver(score, highScore);
        }
    }

    public void resetGame() {
        // Reset game state
        score = 0;
        lives = 3;
        level = 1;
        gameOver = false;
        spawnDelay = INITIAL_SPAWN_DELAY;
        baseSpeed = 4.0f;
        
        // Clear waste items
        wasteItems.clear();
        
        // Reset timer
        lastItemSpawnTime = 0;
        
        // Notify callback
        if (callback != null) {
            callback.onScoreChanged(score);
        }
        
        // Start background music
        if (bgMusic != null) {
            bgMusic.seekTo(0);
            bgMusic.start();
        }
    }

    public void releaseResources() {
        // Release sound resources
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        
        if (bgMusic != null) {
            bgMusic.release();
            bgMusic = null;
        }
    }

    // Resource arrays for different types of waste items
    private static final int[] organicImages = {
            R.drawable.waste_apple,
            R.drawable.waste_banana,
            R.drawable.waste_leaf,
            R.drawable.waste_eggshell,
            R.drawable.waste_food
    };
    
    private static final int[] inorganicImages = {
            R.drawable.waste_bottle,
            R.drawable.waste_paper,
            R.drawable.waste_plastic,
            R.drawable.waste_can,
            R.drawable.waste_glass
    };
    
    private static final int[] hazardousImages = {
            R.drawable.waste_battery,
            R.drawable.waste_bulb,
            R.drawable.waste_chemical,
            R.drawable.waste_medicine,
            R.drawable.waste_paint
    };

    // Game object classes
    private static class WasteItem {
        Bitmap bitmap;
        float x, y;
        WasteType type;
        float speed;

        WasteItem(Bitmap bitmap, float x, float y, WasteType type, float speed) {
            this.bitmap = bitmap;
            this.x = x;
            this.y = y;
            this.type = type;
            this.speed = speed;
        }
    }

    private static class TrashBin {
        Bitmap bitmap;
        float x, y;
        WasteType type;

        TrashBin(Bitmap bitmap, float x, float y, WasteType type) {
            this.bitmap = bitmap;
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    // Waste types
    public enum WasteType {
        ORGANIC,
        INORGANIC,
        HAZARDOUS
    }

    public boolean isSurfaceReady() {
        return surfaceReady;
    }
    
    public boolean isGamePaused() {
        return gamePaused;
    }
    
    public boolean isGameRunning() {
        return isRunning && !gameOver;
    }
}