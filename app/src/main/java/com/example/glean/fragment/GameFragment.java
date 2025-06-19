package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.glean.R;
import com.example.glean.game.GameEngine;
// import com.example.glean.model.GameScore; // TODO: Create GameScore model
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class GameFragment extends Fragment implements GameEngine.GameCallback {    
    private static final String PREFS_NAME = "EcoSortifyGamePrefs";
    private static final String HIGH_SCORE_KEY = "highScore";
    
    private FrameLayout gameContainer;
    private TextView highScoreText;
    private MaterialButton playButton;
    private MaterialButton pauseButton;
    
    private GameEngine gameEngine;
    private boolean gameStarted = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
          // Find views
        gameContainer = view.findViewById(R.id.game_container);
        highScoreText = view.findViewById(R.id.high_score_text);
        playButton = view.findViewById(R.id.play_button);
        pauseButton = view.findViewById(R.id.pause_button);
          // Set up buttons        
        playButton.setOnClickListener(v -> {
            android.util.Log.d("GameFragment", "Play button clicked");
            startGame();
        });
        pauseButton.setOnClickListener(v -> {
            android.util.Log.d("GameFragment", "Pause button clicked");
            pauseGame();
        });
        
        // Setup help button in toolbar
        androidx.appcompat.widget.Toolbar gameToolbar = view.findViewById(R.id.toolbar_game);
        if (gameToolbar != null) {
            android.widget.ImageButton toolbarHelpButton = gameToolbar.findViewById(R.id.btn_help_game);
            if (toolbarHelpButton != null) {
                toolbarHelpButton.setOnClickListener(v -> showGameHelp());
            }
        }
        
        // Hide pause button initially
        pauseButton.setVisibility(View.GONE);
        
        // Load and display high score
        loadAndDisplayHighScore();
    }
    
    private void loadAndDisplayHighScore() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, requireContext().MODE_PRIVATE);
        int highScore = prefs.getInt(HIGH_SCORE_KEY, 0);
        updateHighScoreText(highScore);
    }

    private void startGame() {
        android.util.Log.d("GameFragment", "startGame() called - gameStarted: " + gameStarted);
        
        if (gameEngine == null) {
            // Create new game engine
            android.util.Log.d("GameFragment", "Creating new GameEngine...");
            gameEngine = new GameEngine(requireContext(), this);
            gameContainer.addView(gameEngine);
            
            // Wait a bit for surface to be ready
            gameContainer.post(() -> {
                if (gameEngine.isSurfaceReady()) {
                    android.util.Log.d("GameFragment", "Surface is ready, starting game");
                    proceedWithGameStart();
                } else {
                    android.util.Log.d("GameFragment", "Surface not ready, waiting...");
                    // Try again after a short delay
                    gameContainer.postDelayed(() -> {
                        if (gameEngine.isSurfaceReady()) {
                            proceedWithGameStart();
                        } else {
                            android.util.Log.w("GameFragment", "Surface still not ready after delay");
                        }
                    }, 100);
                }
            });
        } else {
            proceedWithGameStart();
        }
    }
    
    private void proceedWithGameStart() {
        if (!gameStarted) {
            android.util.Log.d("GameFragment", "Starting new game...");
            // Show pause button, hide play button
            playButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            
            // Start the game
            gameEngine.startGame();
            gameStarted = true;
        } else {
            android.util.Log.d("GameFragment", "Resuming paused game...");
            // Resume the game if it was paused
            gameEngine.startGame();
            pauseButton.setText("Jeda");
            pauseButton.setIcon(requireContext().getDrawable(R.drawable.ic_pause_24));
        }
    }

    private void pauseGame() {
        if (gameEngine != null && gameStarted) {
            gameEngine.pauseGame();
            pauseButton.setText("Lanjut");
            pauseButton.setIcon(requireContext().getDrawable(R.drawable.ic_play_24));
        }
    }    private void showGameHelp() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🎮 Cara Bermain Eco Sorting")
                .setMessage("Seret sampah ke tempat sampah yang sesuai dengan jenisnya:\n\n" +
                        "🟢 ORGANIK (Hijau):\n" +
                        "• Sisa makanan, kulit buah\n" +
                        "• Daun kering, ranting\n\n" +
                        "� ANORGANIK (Kuning):\n" +
                        "• Plastik, botol, kaleng\n" +
                        "• Kertas, kardus\n\n" +
                        "🔴 B3 (Merah):\n" +
                        "• Baterai, lampu\n" +
                        "• Obat-obatan, elektronik\n\n" +
                        "🎯 Tujuan: Raih poin sebanyak-banyaknya!\n" +
                        "❌ Hati-hati: Salah sortir mengurangi poin")
                .setPositiveButton("Mulai Bermain", null)
                .show();
    }

    @Override
    public void onGameOver(int score, int highScore) {
        requireActivity().runOnUiThread(() -> {
            // Update high score text
            updateHighScoreText(highScore);
            
            // Show play button, hide pause button
            playButton.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);
            
            gameStarted = false;
            
            // Show game over dialog with score
            new MaterialAlertDialogBuilder(requireContext())                    .setTitle("Game Over")
                    .setMessage("Skor Akhir: " + score + "\nSkor Tertinggi: " + highScore)
                    .setPositiveButton("Main Lagi", (dialog, which) -> {
                        if (gameEngine != null) {
                            gameEngine.resetGame();
                            startGame();
                        }
                    })
                    .setNegativeButton("Tutup", null)
                    .show();
        });
    }

    @Override
    public void onScoreChanged(int score) {
        // You could update a score display here if needed
    }
    
    private void updateHighScoreText(int highScore) {
        highScoreText.setText("Skor Tertinggi: " + highScore);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (gameEngine != null && gameStarted) {
            gameEngine.pauseGame();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Do not auto-resume, let the user decide
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (gameEngine != null) {
            gameEngine.releaseResources();
            gameEngine = null;
        }
    }
}