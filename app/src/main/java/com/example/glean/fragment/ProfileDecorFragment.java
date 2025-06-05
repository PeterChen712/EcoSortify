package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.glean.R;
import com.example.glean.adapter.DecorationAdapter;
import com.example.glean.databinding.FragmentProfileDecorBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.Decoration;
import com.example.glean.model.UserEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileDecorFragment extends Fragment implements DecorationAdapter.OnDecorationClickListener {

    private FragmentProfileDecorBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private int userId;
    private UserEntity user;
    private List<Decoration> decorations = new ArrayList<>();
    private DecorationAdapter adapter;
    private List<String> ownedDecorations = new ArrayList<>();    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        // Get user ID from arguments first, then SharedPreferences
        if (getArguments() != null) {
            userId = getArguments().getInt("USER_ID", -1);
            Log.d("ProfileDecorFragment", "Got userId from arguments: " + userId);
        }
        
        // Fallback to SharedPreferences if not in arguments
        if (userId == -1) {
            userId = requireActivity().getSharedPreferences("USER_PREFS", 0).getInt("USER_ID", -1);
            Log.d("ProfileDecorFragment", "Got userId from USER_PREFS: " + userId);
        }
        
        // Final fallback to default preferences
        if (userId == -1) {
            SharedPreferences defaultPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
            userId = defaultPrefs.getInt("USER_ID", -1);
            Log.d("ProfileDecorFragment", "Got userId from default prefs: " + userId);
        }
        
        Log.d("ProfileDecorFragment", "Final userId: " + userId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileDecorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        
        // Setup RecyclerView
        binding.rvDecorations.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new DecorationAdapter(decorations, this);
        binding.rvDecorations.setAdapter(adapter);
        
        // Load user data and decorations
        loadUserData();
        loadDecorations();
    }
      private void loadUserData() {
        if (userId == -1) {
            Log.e("ProfileDecorFragment", "Invalid user ID: " + userId);
            Toast.makeText(requireContext(), "User not found. Please try again.", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }
        
        Log.d("ProfileDecorFragment", "Loading user data for userId: " + userId);
        
        executor.execute(() -> {
            try {
                user = db.userDao().getUserByIdSync(userId);
                
                if (user != null) {
                    Log.d("ProfileDecorFragment", "User loaded successfully: " + user.getUsername());
                    
                    // Update UI with user data
                    requireActivity().runOnUiThread(() -> {
                        binding.tvPoints.setText(String.valueOf(user.getPoints()));
                        
                        // Parse owned decorations
                        if (user.getDecorations() != null && !user.getDecorations().isEmpty()) {
                            ownedDecorations = Arrays.asList(user.getDecorations().split(","));
                        }
                        
                        // Update decoration items to show owned status
                        updateDecorationItems();
                    });
                } else {
                    Log.e("ProfileDecorFragment", "User not found in database for userId: " + userId);
                    
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    });
                }
            } catch (Exception e) {
                Log.e("ProfileDecorFragment", "Error loading user data", e);
                
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                    navigateBack();
                });
            }
        });
    }
    
    private void loadDecorations() {
        // Add available decorations
        decorations.add(new Decoration(1, "Frame Gold", "Golden profile frame", 500, R.drawable.decor_frame_gold));
        decorations.add(new Decoration(2, "Frame Silver", "Silver profile frame", 300, R.drawable.decor_frame_silver));
        decorations.add(new Decoration(3, "Frame Bronze", "Bronze profile frame", 100, R.drawable.decor_frame_bronze));
        decorations.add(new Decoration(4, "Background Forest", "Forest background", 200, R.drawable.decor_bg_forest));
        decorations.add(new Decoration(5, "Background Beach", "Beach background", 200, R.drawable.decor_bg_beach));
        decorations.add(new Decoration(6, "Background Mountain", "Mountain background", 200, R.drawable.decor_bg_mountain));
        decorations.add(new Decoration(7, "Title Eco Warrior", "Eco Warrior title", 400, R.drawable.decor_title_eco));
        decorations.add(new Decoration(8, "Title Trash Hunter", "Trash Hunter title", 400, R.drawable.decor_title_hunter));
        
        adapter.notifyDataSetChanged();
    }
    
    private void updateDecorationItems() {
        for (Decoration decoration : decorations) {
            decoration.setOwned(ownedDecorations.contains(String.valueOf(decoration.getId())));
        }
        adapter.notifyDataSetChanged();
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onDecorationClick(Decoration decoration) {
        if (decoration.isOwned()) {
            // Apply decoration
            applyDecoration(decoration);
        } else {
            // Purchase decoration
            purchaseDecoration(decoration);
        }
    }
    
    private void purchaseDecoration(Decoration decoration) {
        if (user == null) return;
        
        if (user.getPoints() >= decoration.getPrice()) {
            executor.execute(() -> {
                // Deduct points
                user.setPoints(user.getPoints() - decoration.getPrice());
                
                // Add decoration to owned list
                String decorations = user.getDecorations();
                if (decorations == null || decorations.isEmpty()) {
                    decorations = String.valueOf(decoration.getId());
                } else {
                    decorations += "," + decoration.getId();
                }
                user.setDecorations(decorations);
                
                // Update user in database
                db.userDao().update(user);
                
                // Add to owned decorations list in memory
                ownedDecorations.add(String.valueOf(decoration.getId()));
                
                // Update UI
                requireActivity().runOnUiThread(() -> {
                    binding.tvPoints.setText(String.valueOf(user.getPoints()));
                    decoration.setOwned(true);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), 
                            "Purchased " + decoration.getName(), 
                            Toast.LENGTH_SHORT).show();
                });
            });
        } else {
            Toast.makeText(requireContext(), 
                    "Not enough points! You need " + 
                    (decoration.getPrice() - user.getPoints()) + " more", 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void applyDecoration(Decoration decoration) {
        if (user == null) return;
        
        executor.execute(() -> {
            // Apply decoration
            user.setActiveDecoration(String.valueOf(decoration.getId()));
            
            // Update user in database
            db.userDao().update(user);
            
            // Update UI
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), 
                        "Applied " + decoration.getName(), 
                        Toast.LENGTH_SHORT).show();
            });
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}