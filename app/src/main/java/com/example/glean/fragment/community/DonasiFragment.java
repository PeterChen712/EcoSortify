package com.example.glean.fragment.community;

import android.content.Intent;
import android.net.Uri;
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
import com.example.glean.adapter.DonationAdapter;
import com.example.glean.databinding.FragmentDonasiBinding;
import com.example.glean.model.DonationEntity;

import java.util.ArrayList;
import java.util.List;

public class DonasiFragment extends Fragment implements DonationAdapter.OnDonationClickListener {
    
    private FragmentDonasiBinding binding;
    private DonationAdapter donationAdapter;
    private List<DonationEntity> donations = new ArrayList<>();
    private String currentTab = "donations";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDonasiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        loadDonations();
    }
    
    private void setupTabs() {
        binding.chipDonations.setOnClickListener(v -> {
            currentTab = "donations";
            updateTabSelection();
            loadDonations();
        });
        
        binding.chipCollaborations.setOnClickListener(v -> {
            currentTab = "collaborations";
            updateTabSelection();
            loadCollaborations();
        });
        
        binding.chipPrograms.setOnClickListener(v -> {
            currentTab = "programs";
            updateTabSelection();
            loadPrograms();
        });
    }
    
    private void updateTabSelection() {
        binding.chipDonations.setChecked(currentTab.equals("donations"));
        binding.chipCollaborations.setChecked(currentTab.equals("collaborations"));
        binding.chipPrograms.setChecked(currentTab.equals("programs"));
    }
    
    private void setupRecyclerView() {
        donationAdapter = new DonationAdapter(donations, this);
        binding.recyclerViewDonations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewDonations.setAdapter(donationAdapter);
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            switch (currentTab) {
                case "donations":
                    loadDonations();
                    break;
                case "collaborations":
                    loadCollaborations();
                    break;
                case "programs":
                    loadPrograms();
                    break;
            }
        });
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
    }
    
    private void loadDonations() {
        binding.swipeRefreshLayout.setRefreshing(true);
        donations.clear();
        
        // Sample donation campaigns
        DonationEntity donation1 = new DonationEntity();
        donation1.setId("d1");
        donation1.setTitle("Selamatkan Pantai Indonesia");
        donation1.setDescription("Kampanye untuk membersihkan pantai-pantai di Indonesia dari sampah plastik. Dana akan digunakan untuk alat pembersih dan edukasi masyarakat.");
        donation1.setType("donation");
        donation1.setTargetAmount(50000000);
        donation1.setCurrentAmount(32450000);
        donation1.setDonorCount(1247);
        donation1.setImageUrl("beach_cleanup");
        donation1.setOrganization("Yayasan Laut Bersih Indonesia");
        donation1.setTimeLeft(25 * 24 * 60 * 60 * 1000L); // 25 days
        donations.add(donation1);
        
        DonationEntity donation2 = new DonationEntity();
        donation2.setId("d2");
        donation2.setTitle("Bank Sampah Komunitas");
        donation2.setDescription("Membangun bank sampah di desa-desa untuk mendukung ekonomi circular dan mengurangi sampah yang berakhir di TPA.");
        donation2.setType("donation");
        donation2.setTargetAmount(25000000);
        donation2.setCurrentAmount(18200000);
        donation2.setDonorCount(856);
        donation2.setImageUrl("waste_bank");
        donation2.setOrganization("Gerakan Desa Hijau");
        donation2.setTimeLeft(18 * 24 * 60 * 60 * 1000L); // 18 days
        donations.add(donation2);
        
        DonationEntity donation3 = new DonationEntity();
        donation3.setId("d3");
        donation3.setTitle("Edukasi Lingkungan Sekolah");
        donation3.setDescription("Program edukasi lingkungan untuk anak-anak sekolah dasar tentang pentingnya menjaga kebersihan dan kelestarian alam.");
        donation3.setType("donation");
        donation3.setTargetAmount(15000000);
        donation3.setCurrentAmount(12750000);
        donation3.setDonorCount(623);
        donation3.setImageUrl("school_education");
        donation3.setOrganization("EcoKids Indonesia");
        donation3.setTimeLeft(12 * 24 * 60 * 60 * 1000L); // 12 days
        donations.add(donation3);
        
        donationAdapter.notifyDataSetChanged();
        binding.swipeRefreshLayout.setRefreshing(false);
        updateEmptyState();
    }
    
    private void loadCollaborations() {
        binding.swipeRefreshLayout.setRefreshing(true);
        donations.clear();
        
        // Sample collaborations
        DonationEntity collab1 = new DonationEntity();
        collab1.setId("c1");
        collab1.setTitle("Kemitraan dengan Unilever");
        collab1.setDescription("Kolaborasi untuk program 'Plastik Jadi Rupiah' - tukar sampah plastik dengan poin yang bisa ditukar uang.");
        collab1.setType("collaboration");
        collab1.setPartner("Unilever Indonesia");
        collab1.setParticipants(5432);
        collab1.setReward("Poin tukar rupiah");
        collab1.setImageUrl("unilever_collab");
        donations.add(collab1);
        
        DonationEntity collab2 = new DonationEntity();
        collab2.setId("c2");
        collab2.setTitle("Kolaborasi Grab x EcoRide");
        collab2.setDescription("Program transportasi ramah lingkungan dengan driver yang juga berpartisipasi dalam kegiatan plogging.");
        collab2.setType("collaboration");
        collab2.setPartner("Grab Indonesia");
        collab2.setParticipants(2156);
        collab2.setReward("Diskon transportasi");
        collab2.setImageUrl("grab_collab");
        donations.add(collab2);
        
        DonationEntity collab3 = new DonationEntity();
        collab3.setId("c3");
        collab3.setTitle("Partnership Tzu Chi");
        collab3.setDescription("Kerjasama dengan Tzu Chi untuk program daur ulang dan pemberdayaan masyarakat melalui bank sampah.");
        collab3.setType("collaboration");
        collab3.setPartner("Yayasan Buddha Tzu Chi");
        collab3.setParticipants(1876);
        collab3.setReward("Pelatihan gratis");
        collab3.setImageUrl("tzuchi_collab");
        donations.add(collab3);
        
        donationAdapter.notifyDataSetChanged();
        binding.swipeRefreshLayout.setRefreshing(false);
        updateEmptyState();
    }
    
    private void loadPrograms() {
        binding.swipeRefreshLayout.setRefreshing(true);
        donations.clear();
        
        // Sample social programs
        DonationEntity program1 = new DonationEntity();
        program1.setId("p1");
        program1.setTitle("Adopsi Hutan Mangrove");
        program1.setDescription("Program adopsi dan perawatan hutan mangrove untuk melindungi ekosistem pesisir dari abrasi dan polusi.");
        program1.setType("program");
        program1.setLocation("Pantai Utara Jawa");
        program1.setVolunteers(234);
        program1.setImpact("500 pohon ditanam");
        program1.setImageUrl("mangrove_program");
        donations.add(program1);
        
        DonationEntity program2 = new DonationEntity();
        program2.setId("p2");
        program2.setTitle("Kampung Tanpa Sampah");
        program2.setDescription("Program transformasi kampung menjadi bebas sampah melalui sistem pengelolaan sampah terpadu dan edukasi warga.");
        program2.setType("program");
        program2.setLocation("Kampung Hijau, Yogyakarta");
        program2.setVolunteers(156);
        program2.setImpact("95% pengurangan sampah");
        program2.setImageUrl("zero_waste_village");
        donations.add(program2);
        
        DonationEntity program3 = new DonationEntity();
        program3.setId("p3");
        program3.setTitle("Sekolah Hijau Nusantara");
        program3.setDescription("Program mengubah sekolah menjadi lingkungan hijau dengan taman, kompos, dan kurikulum berbasis lingkungan.");
        program3.setType("program");
        program3.setLocation("Seluruh Indonesia");
        program3.setVolunteers(445);
        program3.setImpact("50 sekolah hijau");
        program3.setImageUrl("green_school");
        donations.add(program3);
        
        donationAdapter.notifyDataSetChanged();
        binding.swipeRefreshLayout.setRefreshing(false);
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (donations.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewDonations.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerViewDonations.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onDonationClick(DonationEntity donation) {
        Toast.makeText(requireContext(), "Membuka: " + donation.getTitle(), Toast.LENGTH_SHORT).show();
    }
      @Override
    public void onDonateClick(DonationEntity donation) {
        // Open donation page or payment gateway
        String donateUrl = "https://donation.example.com/" + donation.getId();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(donateUrl));
        startActivity(intent);
    }
    
    @Override
    public void onLearnMoreClick(DonationEntity donation) {
        Toast.makeText(requireContext(), "Pelajari lebih lanjut: " + donation.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onJoinProgramClick(DonationEntity donation) {
        Toast.makeText(requireContext(), "Bergabung program: " + donation.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onShareClick(DonationEntity donation) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Mari dukung: " + donation.getTitle() + "\n" + donation.getDescription());
        startActivity(Intent.createChooser(shareIntent, "Bagikan Program"));
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
