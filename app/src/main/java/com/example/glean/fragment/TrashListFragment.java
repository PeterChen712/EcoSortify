package com.example.glean.fragment;

import android.os.Bundle;
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
import com.example.glean.adapter.TrashAdapter;
import com.example.glean.databinding.FragmentTrashListBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.RecordEntity;
import com.example.glean.model.TrashEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashListFragment extends Fragment implements TrashAdapter.OnTrashClickListener {

    private FragmentTrashListBinding binding;
    private AppDatabase db;
    private ExecutorService executor;
    private TrashAdapter adapter;
    private List<TrashEntity> trashList = new ArrayList<>();
    private int recordId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        executor = Executors.newSingleThreadExecutor();
        
        if (getArguments() != null) {
            recordId = getArguments().getInt("RECORD_ID", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTrashListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup RecyclerView
        binding.rvTrash.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TrashAdapter(trashList, this);
        binding.rvTrash.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener(this::loadTrashItems);
        binding.swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
        
        // Set click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        
        // Load data
        loadRecord();
        loadTrashItems();
    }
    
    private void loadRecord() {
        if (recordId != -1) {
            // Use LiveData properly
            db.recordDao().getRecordById(recordId).observe(getViewLifecycleOwner(), record -> {
                if (record != null) {
                    // Format the date from createdAt timestamp
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    String formattedDate = dateFormat.format(new Date(record.getCreatedAt()));
                    binding.tvTitle.setText("Trash Items - " + formattedDate);
                } else {
                    binding.tvTitle.setText("Trash Items");
                }
            });
        } else {
            binding.tvTitle.setText("Trash Items");
        }
    }
    
    private void loadTrashItems() {
        binding.swipeRefresh.setRefreshing(true);
        binding.tvEmptyList.setVisibility(View.GONE);
        
        if (recordId != -1) {
            // Use LiveData properly
            db.trashDao().getTrashByRecordId(recordId).observe(getViewLifecycleOwner(), items -> {
                binding.swipeRefresh.setRefreshing(false);
                
                if (items != null && !items.isEmpty()) {
                    trashList.clear();
                    trashList.addAll(items);
                    adapter.notifyDataSetChanged();
                    binding.tvEmptyList.setVisibility(View.GONE);
                } else {
                    trashList.clear();
                    adapter.notifyDataSetChanged();
                    binding.tvEmptyList.setVisibility(View.VISIBLE);
                }
            });
        } else {
            binding.swipeRefresh.setRefreshing(false);
            binding.tvEmptyList.setVisibility(View.VISIBLE);
        }
    }
    
    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }
    
    @Override
    public void onTrashItemClick(TrashEntity trash) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putInt("TRASH_ID", (int)trash.getId()); // Convert long to int
        navController.navigate(R.id.action_trashListFragment_to_trashDetailFragment, args);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}