package com.example.glean.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.glean.R;
import com.example.glean.adapter.HistoryAdapter;
import com.example.glean.databinding.FragmentHistoryBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnHistoryClickListener {

    private FragmentHistoryBinding binding;
    private AppDatabase db;
    private int userId;
    private ExecutorService executor;
    private HistoryAdapter adapter;
    private List<RecordEntity> recordList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        userId = prefs.getInt("USER_ID", -1);
        executor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup RecyclerView
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(recordList, this);
        binding.rvHistory.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener(this::loadHistory);
        binding.swipeRefresh.setColorSchemeResources(
                android.R.color.holo_green_light,
                android.R.color.holo_blue_light,
                android.R.color.holo_orange_light
        );
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        
        // Load data
        loadHistory();
    }
    
    private void loadHistory() {
        binding.swipeRefresh.setRefreshing(true);
        binding.tvEmptyHistory.setVisibility(View.GONE);
        
        if (userId != -1) {
            executor.execute(() -> {
                // Use synchronous method instead of LiveData
                List<RecordEntity> records = db.recordDao().getAllRecordsByUserIdSync(userId);
                
                requireActivity().runOnUiThread(() -> {
                    binding.swipeRefresh.setRefreshing(false);
                    
                    if (records != null && !records.isEmpty()) {
                        recordList.clear();
                        recordList.addAll(records);
                        adapter.notifyDataSetChanged();
                        binding.tvEmptyHistory.setVisibility(View.GONE);
                    } else {
                        recordList.clear();
                        adapter.notifyDataSetChanged();
                        binding.tvEmptyHistory.setVisibility(View.VISIBLE);
                    }
                });
            });
        } else {
            binding.swipeRefresh.setRefreshing(false);
            binding.tvEmptyHistory.setVisibility(View.VISIBLE);
        }
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }
    
    @Override
    public void onHistoryItemClick(RecordEntity record) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putInt("RECORD_ID", record.getId());
        navController.navigate(R.id.action_historyFragment_to_summaryFragment, args);
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