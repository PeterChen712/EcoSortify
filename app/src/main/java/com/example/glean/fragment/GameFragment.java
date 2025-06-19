package com.example.glean.fragment;

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

public class GameFragment extends Fragment implements GameEngine.GameCallback {    private FrameLayout gameContainer;
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
        
        // Set up buttons        playButton.setOnClickListener(v -> startGame());
        pauseButton.setOnClickListener(v -> pauseGame());
        
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
    }

    private void startGame() {
        if (gameEngine == null) {
            // Create new game engine
            gameEngine = new GameEngine(requireContext(), this);
            gameContainer.addView(gameEngine);
        }
        
        if (!gameStarted) {
            // Show pause button, hide play button
            playButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            
            // Start the game
            gameEngine.startGame();
            gameStarted = true;
        } else {
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