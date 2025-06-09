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
import com.example.glean.adapter.NewsAdapter;
import com.example.glean.databinding.FragmentCommunityNewsBinding;
import com.example.glean.model.NewsItem;

import java.util.ArrayList;
import java.util.List;

public class NewsFragment extends Fragment implements NewsAdapter.OnNewsItemClickListener {
    
    private FragmentCommunityNewsBinding binding;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsList = new ArrayList<>();
      @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCommunityNewsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupSwipeRefresh();
        setupCategories();
        loadNews();
    }
      private void setupRecyclerView() {
        newsAdapter = new NewsAdapter(requireContext(), newsList, this);
        binding.recyclerViewNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewNews.setAdapter(newsAdapter);
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadNews);
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
    }
    
    private void setupCategories() {
        // Setup category chips
        binding.chipAll.setOnClickListener(v -> filterByCategory("all"));
        binding.chipEnvironment.setOnClickListener(v -> filterByCategory("environment"));
        binding.chipPlogging.setOnClickListener(v -> filterByCategory("plogging"));
        binding.chipTips.setOnClickListener(v -> filterByCategory("tips"));
        binding.chipEducation.setOnClickListener(v -> filterByCategory("education"));
    }
    
    private void filterByCategory(String category) {
        // Reset chip states
        binding.chipAll.setChecked(false);
        binding.chipEnvironment.setChecked(false);
        binding.chipPlogging.setChecked(false);
        binding.chipTips.setChecked(false);
        binding.chipEducation.setChecked(false);
        
        // Set selected chip
        switch (category) {
            case "all":
                binding.chipAll.setChecked(true);
                break;
            case "environment":
                binding.chipEnvironment.setChecked(true);
                break;
            case "plogging":
                binding.chipPlogging.setChecked(true);
                break;
            case "tips":
                binding.chipTips.setChecked(true);
                break;
            case "education":
                binding.chipEducation.setChecked(true);
                break;
        }
        
        loadNews(category);
    }
    
    private void loadNews() {
        loadNews("all");
    }
      private void loadNews(String category) {
        binding.swipeRefreshLayout.setRefreshing(true);
        newsList.clear();
        
        // Sample environmental news - replace with actual API call
        NewsItem news1 = new NewsItem();
        news1.setId(1);        news1.setTitle("Indonesia Targetkan Zero Waste pada 2030");
        news1.setPreview("Pemerintah Indonesia berkomitmen mencapai target zero waste melalui berbagai program pengelolaan sampah yang berkelanjutan.");
        news1.setImageUrl("https://example.com/news1.jpg");
        news1.setUrl("https://www.detik.com/edu/detikpedia/d-5234567/indonesia-targetkan-zero-waste-pada-2030");
        news1.setCategory("environment");
        news1.setDate("2025-06-07T10:00:00Z");
        news1.setSource("Kementerian Lingkungan Hidup");
        newsList.add(news1);
        
        NewsItem news2 = new NewsItem();
        news2.setId(2);
        news2.setTitle("Tips Plogging untuk Pemula");
        news2.setPreview("Panduan lengkap memulai aktivitas plogging dengan aman dan efektif untuk membersihkan lingkungan.");
        news2.setImageUrl("https://example.com/news2.jpg");
        news2.setUrl("https://www.kompas.com/lifestyle/read/2025/06/06/tips-plogging-untuk-pemula");
        news2.setCategory("tips");
        news2.setDate("2025-06-06T15:30:00Z");
        news2.setSource("Komunitas Plogging Indonesia");
        newsList.add(news2);
        
        NewsItem news3 = new NewsItem();
        news3.setId(3);
        news3.setTitle("Dampak Sampah Plastik terhadap Ekosistem Laut");
        news3.setPreview("Penelitian terbaru menunjukkan dampak serius sampah plastik terhadap kehidupan laut dan rantai makanan.");
        news3.setImageUrl("https://example.com/news3.jpg");
        news3.setUrl("https://www.cnnindonesia.com/teknologi/20250605091500-199-dampak-sampah-plastik-laut");
        news3.setCategory("education");
        news3.setDate("2025-06-05T09:15:00Z");
        news3.setSource("Lembaga Penelitian Lingkungan");
        newsList.add(news3);
        
        NewsItem news4 = new NewsItem();
        news4.setId(4);
        news4.setTitle("Gerakan Plogging Menyebar ke 50 Kota di Indonesia");
        news4.setPreview("Aktivitas plogging kini telah menyebar ke 50 kota besar di Indonesia dengan partisipasi ribuan relawan.");
        news4.setImageUrl("https://example.com/news4.jpg");
        news4.setUrl("https://www.tempo.co/read/1234567/gerakan-plogging-menyebar-50-kota-indonesia");
        news4.setCategory("plogging");
        news4.setDate("2025-06-04T14:20:00Z");
        news4.setSource("Plogging Indonesia");
        newsList.add(news4);
        
        NewsItem news5 = new NewsItem();
        news5.setId(5);
        news5.setTitle("Cara Memilah Sampah yang Benar");
        news5.setPreview("Edukasi tentang cara memilah sampah organik dan anorganik untuk mendukung program daur ulang.");
        news5.setImageUrl("https://example.com/news5.jpg");
        news5.setUrl("https://jakarta.go.id/artikel/cara-memilah-sampah-yang-benar");
        news5.setCategory("education");
        news5.setDate("2025-06-03T11:45:00Z");
        news5.setSource("Dinas Kebersihan DKI");
        newsList.add(news5);
        
        // Filter by category if not "all"
        if (!"all".equals(category)) {
            newsList.removeIf(news -> !news.getCategory().equals(category));
        }
        
        newsAdapter.notifyDataSetChanged();
        binding.swipeRefreshLayout.setRefreshing(false);
        
        // Show/hide empty state
        if (newsList.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewNews.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerViewNews.setVisibility(View.VISIBLE);
        }
    }    @Override
    public void onNewsItemClick(NewsItem news) {
        // Open news article directly in browser (Chrome)
        try {
            String url = news.getUrl();
            if (url == null || url.isEmpty()) {
                Toast.makeText(requireContext(), "URL tidak tersedia", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.android.chrome"); // Try Chrome first
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to default browser if Chrome not available
                intent.setPackage(null);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Tidak dapat membuka artikel", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
