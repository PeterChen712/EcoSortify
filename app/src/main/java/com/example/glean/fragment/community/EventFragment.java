package com.example.glean.fragment.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.glean.R;
import com.example.glean.adapter.EventAdapter;
import com.example.glean.databinding.FragmentEventBinding;
import com.example.glean.model.EventEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EventFragment extends Fragment implements EventAdapter.OnEventClickListener {
    
    private FragmentEventBinding binding;
    private EventAdapter eventAdapter;
    private List<EventEntity> events = new ArrayList<>();
    private String currentTab = "events";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();
        loadEvents();
    }
    
    private void setupTabs() {
        binding.chipEvents.setOnClickListener(v -> {
            currentTab = "events";
            updateTabSelection();
            loadEvents();
        });
        
        binding.chipChallenges.setOnClickListener(v -> {
            currentTab = "challenges";
            updateTabSelection();
            loadChallenges();
        });
        
        binding.chipContests.setOnClickListener(v -> {
            currentTab = "contests";
            updateTabSelection();
            loadContests();
        });
    }
    
    private void updateTabSelection() {
        binding.chipEvents.setChecked(currentTab.equals("events"));
        binding.chipChallenges.setChecked(currentTab.equals("challenges"));
        binding.chipContests.setChecked(currentTab.equals("contests"));
    }
    
    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(events, this);
        binding.recyclerViewEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewEvents.setAdapter(eventAdapter);
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            switch (currentTab) {
                case "events":
                    loadEvents();
                    break;
                case "challenges":
                    loadChallenges();
                    break;
                case "contests":
                    loadContests();
                    break;
            }
        });
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
    }
    
    private void setupFab() {
        binding.fabCreateEvent.setOnClickListener(v -> {
            // Navigate to create event screen
            Toast.makeText(requireContext(), "Fitur buat event akan segera hadir!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadEvents() {
        binding.swipeRefreshLayout.setRefreshing(true);
        events.clear();
        
        // Sample events
        EventEntity event1 = new EventEntity();
        event1.setId("e1");
        event1.setTitle("Plogging Bersama Jakarta");
        event1.setDescription("Mari bergabung dalam kegiatan plogging massal di Taman Monas. Gratis untuk semua peserta!");
        event1.setType("event");
        event1.setLocation("Taman Monas, Jakarta");
        event1.setDate(getDateInMillis(2025, 6, 15, 7, 0));
        event1.setParticipants(45);
        event1.setMaxParticipants(100);
        event1.setImageUrl("event_jakarta");
        event1.setOrganizer("Komunitas Plogging Jakarta");
        events.add(event1);
        
        EventEntity event2 = new EventEntity();
        event2.setId("e2");
        event2.setTitle("Workshop Daur Ulang Sampah");
        event2.setDescription("Pelajari cara mengubah sampah menjadi barang berguna. Materi dan alat disediakan.");
        event2.setType("event");
        event2.setLocation("Gedung Serbaguna, Bandung");
        event2.setDate(getDateInMillis(2025, 6, 20, 13, 0));
        event2.setParticipants(23);
        event2.setMaxParticipants(50);
        event2.setImageUrl("workshop_bandung");
        event2.setOrganizer("EcoCircle Bandung");
        events.add(event2);
        
        EventEntity event3 = new EventEntity();
        event3.setId("e3");
        event3.setTitle("Beach Clean-up Bali");
        event3.setDescription("Kegiatan pembersihan pantai di Sanur. Mari jaga keindahan pantai Bali bersama-sama!");
        event3.setType("event");
        event3.setLocation("Pantai Sanur, Bali");
        event3.setDate(getDateInMillis(2025, 6, 25, 6, 30));
        event3.setParticipants(78);
        event3.setMaxParticipants(150);
        event3.setImageUrl("beach_bali");
        event3.setOrganizer("Bali Clean Initiative");
        events.add(event3);
        
        eventAdapter.notifyDataSetChanged();
        binding.swipeRefreshLayout.setRefreshing(false);
        updateEmptyState();
    }
    
    private void loadChallenges() {
        binding.swipeRefreshLayout.setRefreshing(true);
        events.clear();
        
        // Sample challenges
        EventEntity challenge1 = new EventEntity();
        challenge1.setId("c1");
        challenge1.setTitle("Challenge 30 Hari Plogging");
        challenge1.setDescription("Lakukan plogging selama 30 hari berturut-turut. Dapatkan badge khusus dan poin bonus!");
        challenge1.setType("challenge");
        challenge1.setProgress(15);
        challenge1.setTarget(30);
        challenge1.setReward("Badge Master Plogging + 500 poin");
        challenge1.setParticipants(1245);
        challenge1.setTimeLeft(15 * 24 * 60 * 60 * 1000L); // 15 days
        events.add(challenge1);
        
        EventEntity challenge2 = new EventEntity();
        challenge2.setId("c2");
        challenge2.setTitle("Kumpulkan 100kg Sampah");
        challenge2.setDescription("Target mengumpulkan total 100kg sampah dalam sebulan. Challenge untuk yang serius!");
        challenge2.setType("challenge");
        challenge2.setProgress(35);
        challenge2.setTarget(100);
        challenge2.setReward("Trophy Eco Warrior + 1000 poin");
        challenge2.setParticipants(567);
        challenge2.setTimeLeft(20 * 24 * 60 * 60 * 1000L); // 20 days
        events.add(challenge2);
        
        EventEntity challenge3 = new EventEntity();
        challenge3.setId("c3");
        challenge3.setTitle("Ajak 5 Teman Plogging");
        challenge3.setDescription("Ajak minimal 5 teman untuk bergabung dalam komunitas plogging. Semakin banyak semakin baik!");
        challenge3.setType("challenge");
        challenge3.setProgress(2);
        challenge3.setTarget(5);
        challenge3.setReward("Badge Social Connector + 300 poin");
        challenge3.setParticipants(892);
        challenge3.setTimeLeft(10 * 24 * 60 * 60 * 1000L); // 10 days
        events.add(challenge3);
        
        eventAdapter.notifyDataSetChanged();
        binding.swipeRefreshLayout.setRefreshing(false);
        updateEmptyState();
    }
    
    private void loadContests() {
        binding.swipeRefreshLayout.setRefreshing(true);
        events.clear();
        
        // Sample contests
        EventEntity contest1 = new EventEntity();
        contest1.setId("co1");
        contest1.setTitle("Kontes Foto Plogging Terbaik");
        contest1.setDescription("Upload foto terbaik kegiatan plogging Anda. Foto dengan like terbanyak menang!");
        contest1.setType("contest");
        contest1.setPrize("Peralatan Plogging Lengkap");
        contest1.setParticipants(234);
        contest1.setTimeLeft(7 * 24 * 60 * 60 * 1000L); // 7 days
        contest1.setImageUrl("photo_contest");
        events.add(contest1);
        
        EventEntity contest2 = new EventEntity();
        contest2.setId("co2");
        contest2.setTitle("Weekly Distance Challenge");
        contest2.setDescription("Siapa yang bisa berlari sambil plogging dengan jarak terjauh minggu ini?");
        contest2.setType("contest");
        contest2.setPrize("Sepatu Lari + Voucher 500rb");
        contest2.setParticipants(156);
        contest2.setTimeLeft(3 * 24 * 60 * 60 * 1000L); // 3 days
        contest2.setImageUrl("distance_contest");
        events.add(contest2);
        
        EventEntity contest3 = new EventEntity();
        contest3.setId("co3");
        contest3.setTitle("Inovasi Daur Ulang Sampah");
        contest3.setDescription("Ciptakan inovasi produk dari sampah yang dikumpulkan. Jury akan memilih yang terbaik!");
        contest3.setType("contest");
        contest3.setPrize("Uang Tunai 2 Juta + Trophy");
        contest3.setParticipants(67);
        contest3.setTimeLeft(14 * 24 * 60 * 60 * 1000L); // 14 days
        contest3.setImageUrl("innovation_contest");
        events.add(contest3);
        
        eventAdapter.notifyDataSetChanged();
        binding.swipeRefreshLayout.setRefreshing(false);
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (events.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewEvents.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerViewEvents.setVisibility(View.VISIBLE);
        }
    }
    
    private long getDateInMillis(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }
    
    @Override
    public void onEventClick(EventEntity event) {
        Toast.makeText(requireContext(), "Membuka: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onJoinClick(EventEntity event) {
        Toast.makeText(requireContext(), "Bergabung: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onShareClick(EventEntity event) {
        Toast.makeText(requireContext(), "Dibagikan: " + event.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
