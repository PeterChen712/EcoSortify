package com.example.glean.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.glean.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder surfaceHolder;
    private Thread gameThread;
    private boolean isRunning = false;
    private int score = 0;
    private int lives = 3;
    
    // Game objects
    private List<WasteItem> wasteItems = new ArrayList<>();
    private List<TrashBin> trashBins = new ArrayList<>();
    private Random random = new Random();
    
    // Graphics
    private Paint textPaint;
    private Paint scorePaint;
    private Paint livePaint;
    
    // Timing
    private long lastItemSpawnTime = 0;
    private static final long SPAWN_DELAY = 2000; // 2 seconds
    
    // Sound
    private SoundPool soundPool;
    private int correctSound;
    private int wrongSound;
    
    // Dragging
    private WasteItem draggedItem = null;
    private float dragOffsetX, dragOffsetY;
    
    // Game state
    private boolean gameOver = false;

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        
        setFocusable(true);
        
        // Initialize paints
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(50);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        scorePaint = new Paint();
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(60);
        
        livePaint = new Paint();
        livePaint.setColor(Color.RED);
        livePaint.setTextSize(40);
        
        // Initialize sound
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();
          // Load sounds - commented out until audio files are available
        // correctSound = soundPool.load(getContext(), R.raw.correct, 1);
        // wrongSound = soundPool.load(getContext(), R.raw.wrong, 1);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // Create trash bins when the surface is created
        if (trashBins.isEmpty()) {
            createTrashBins();
        }
        
        // Start the game loop
        startGame();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        // Adjust positioning if needed when surface changes
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        // Stop the game loop
        stopGame();
    }

    private void startGame() {
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void stopGame() {
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            if (!gameOver) {
                update();
            }
            draw();
            control();
        }
    }

    private void update() {
        // Spawn new waste items
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastItemSpawnTime > SPAWN_DELAY) {
            spawnWasteItem();
            lastItemSpawnTime = currentTime;
        }
        
        // Update waste items (falling)
        Iterator<WasteItem> iterator = wasteItems.iterator();
        while (iterator.hasNext()) {
            WasteItem item = iterator.next();
            if (item != draggedItem) {
                item.y += item.speed;
                
                // Check if item is out of screen
                if (item.y > getHeight()) {
                    iterator.remove();
                    lives--;
                    
                    // Check for game over
                    if (lives <= 0) {
                        gameOver = true;
                    }
                }
            }
        }
    }

    private void draw() {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas();
            if (canvas != null) {
                // Draw background
                canvas.drawColor(Color.rgb(247, 251, 241)); // Light green background
                
                // Draw trash bins
                for (TrashBin bin : trashBins) {
                    canvas.drawBitmap(bin.bitmap, bin.x, bin.y, null);
                }
                
                // Draw waste items
                for (WasteItem item : wasteItems) {
                    canvas.drawBitmap(item.bitmap, item.x, item.y, null);
                }
                
                // Draw score and lives
                canvas.drawText("Score: " + score, 100, 60, scorePaint);
                canvas.drawText("Lives: " + lives, getWidth() - 100, 60, livePaint);
                
                // Draw game over message
                if (gameOver) {
                    canvas.drawText("GAME OVER", getWidth() / 2, getHeight() / 2, textPaint);
                    canvas.drawText("Final Score: " + score, getWidth() / 2, getHeight() / 2 + 100, textPaint);
                    canvas.drawText("Tap to restart", getWidth() / 2, getHeight() / 2 + 200, textPaint);
                }
            }
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
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

    private void spawnWasteItem() {
        if (wasteItems.size() >= 10) return; // Limit number of items
        
        int wasteType = random.nextInt(3); // 0=organic, 1=inorganic, 2=hazardous
        
        Bitmap bitmap = null;
        switch (wasteType) {
            case 0: // Organic
                int organicIndex = random.nextInt(organicImages.length);
                bitmap = BitmapFactory.decodeResource(getResources(), organicImages[organicIndex]);
                break;
            case 1: // Inorganic
                int inorganicIndex = random.nextInt(inorganicImages.length);
                bitmap = BitmapFactory.decodeResource(getResources(), inorganicImages[inorganicIndex]);
                break;
            case 2: // Hazardous
                int hazardousIndex = random.nextInt(hazardousImages.length);
                bitmap = BitmapFactory.decodeResource(getResources(), hazardousImages[hazardousIndex]);
                break;
        }
        
        if (bitmap != null) {
            // Scale bitmap
            bitmap = Bitmap.createScaledBitmap(bitmap, 120, 120, false);
            
            // Create waste item
            float x = random.nextInt(getWidth() - 120);
            WasteItem item = new WasteItem(bitmap, x, -120, wasteType, 5 + random.nextInt(5));
            wasteItems.add(item);
        }
    }

    private void createTrashBins() {
        // Create three trash bins at the bottom
        int binWidth = getWidth() / 3;
        int binHeight = 200;
        
        // Organic bin
        Bitmap organicBin = BitmapFactory.decodeResource(getResources(), R.drawable.bin_organic);
        organicBin = Bitmap.createScaledBitmap(organicBin, binWidth, binHeight, false);
        trashBins.add(new TrashBin(organicBin, 0, getHeight() - binHeight, 0));
        
        // Inorganic bin
        Bitmap inorganicBin = BitmapFactory.decodeResource(getResources(), R.drawable.bin_inorganic);
        inorganicBin = Bitmap.createScaledBitmap(inorganicBin, binWidth, binHeight, false);
        trashBins.add(new TrashBin(inorganicBin, binWidth, getHeight() - binHeight, 1));
        
        // Hazardous bin
        Bitmap hazardousBin = BitmapFactory.decodeResource(getResources(), R.drawable.bin_hazardous);
        hazardousBin = Bitmap.createScaledBitmap(hazardousBin, binWidth, binHeight, false);
        trashBins.add(new TrashBin(hazardousBin, binWidth * 2, getHeight() - binHeight, 2));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (gameOver) {
                    // Restart game
                    resetGame();
                    return true;
                }
                
                // Check if a waste item is touched
                for (WasteItem item : wasteItems) {
                    if (touchX >= item.x && touchX <= item.x + item.bitmap.getWidth() &&
                            touchY >= item.y && touchY <= item.y + item.bitmap.getHeight()) {
                        draggedItem = item;
                        dragOffsetX = touchX - item.x;
                        dragOffsetY = touchY - item.y;
                        break;
                    }
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (draggedItem != null) {
                    draggedItem.x = touchX - dragOffsetX;
                    draggedItem.y = touchY - dragOffsetY;
                }
                break;
                
            case MotionEvent.ACTION_UP:
                if (draggedItem != null) {
                    // Check if the item is dropped on a bin
                    for (TrashBin bin : trashBins) {
                        if (draggedItem.x + draggedItem.bitmap.getWidth() / 2 >= bin.x &&
                                draggedItem.x + draggedItem.bitmap.getWidth() / 2 <= bin.x + bin.bitmap.getWidth()) {
                            
                            // Check if correct bin
                            if (draggedItem.type == bin.type) {
                                // Correct bin
                                score += 10;
                                soundPool.play(correctSound, 1, 1, 0, 0, 1);
                            } else {
                                // Wrong bin
                                lives--;
                                soundPool.play(wrongSound, 1, 1, 0, 0, 1);
                                
                                // Check for game over
                                if (lives <= 0) {
                                    gameOver = true;
                                }
                            }
                            
                            // Remove item
                            wasteItems.remove(draggedItem);
                            draggedItem = null;
                            return true;
                        }
                    }
                    
                    // Item not dropped on a bin, continue falling
                    draggedItem = null;
                }
                break;
        }
        
        return true;
    }

    private void resetGame() {
        score = 0;
        lives = 3;
        wasteItems.clear();
        gameOver = false;
        lastItemSpawnTime = 0;
    }

    // Resource arrays for different types of waste items
    private static final int[] organicImages = {
            R.drawable.waste_apple,
            R.drawable.waste_banana,
            R.drawable.waste_leaf
    };
    
    private static final int[] inorganicImages = {
            R.drawable.waste_bottle,
            R.drawable.waste_paper,
            R.drawable.waste_plastic
    };
    
    private static final int[] hazardousImages = {
            R.drawable.waste_battery,
            R.drawable.waste_bulb,
            R.drawable.waste_chemical
    };

    // Game object classes
    private static class WasteItem {
        Bitmap bitmap;
        float x, y;
        int type; // 0=organic, 1=inorganic, 2=hazardous
        float speed;

        WasteItem(Bitmap bitmap, float x, float y, int type, float speed) {
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
        int type; // 0=organic, 1=inorganic, 2=hazardous

        TrashBin(Bitmap bitmap, float x, float y, int type) {
            this.bitmap = bitmap;
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }
}