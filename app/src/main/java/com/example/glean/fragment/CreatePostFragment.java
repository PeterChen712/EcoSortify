package com.example.glean.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.glean.R;
import com.example.glean.databinding.FragmentCreatePostBinding;
import com.example.glean.db.AppDatabase;
import com.example.glean.model.PostEntity;
import com.example.glean.model.UserEntity;
import com.example.glean.repository.CommunityRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreatePostFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_IMAGE_GALLERY = 1002;
    private static final int REQUEST_LOCATION_PERMISSION = 1003;    private FragmentCreatePostBinding binding;
    private AppDatabase db;
    private CommunityRepository repository;
    private ExecutorService executor;
    private int currentUserId;
    private FusedLocationProviderClient fusedLocationClient;
    
    private Uri imageUri;
    private String currentPhotoPath;
    private Location currentLocation;
    private String selectedCategory = "plogging";    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        repository = new CommunityRepository(requireContext());
        executor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Get current user ID from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", requireContext().MODE_PRIVATE);
        currentUserId = prefs.getInt("current_user_id", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreatePostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
          if (currentUserId == -1) {
            Toast.makeText(requireContext(), getString(R.string.create_post_sign_in_required), Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        setupUI();
        getCurrentLocation();
    }

    private void setupUI() {
        // Setup category spinner
        String[] categories = {"plogging", "achievement", "tips", "general"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(adapter);
        
        binding.spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = categories[position];
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedCategory = "plogging";
            }
        });

        // Setup click listeners
        binding.btnBack.setOnClickListener(v -> navigateBack());
        binding.btnCamera.setOnClickListener(v -> openCamera());
        binding.btnGallery.setOnClickListener(v -> openGallery());
        binding.btnLocation.setOnClickListener(v -> getCurrentLocation());
        binding.btnPost.setOnClickListener(v -> createPost());
        binding.btnRemoveImage.setOnClickListener(v -> removeImage());

        // Initially hide remove image button
        binding.btnRemoveImage.setVisibility(View.GONE);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(requireContext(), getString(R.string.create_post_error_image_file), Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.glean.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "GLEANGO_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir("Pictures");
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }

        binding.btnLocation.setEnabled(false);
        binding.tvLocationStatus.setText("Getting location...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    binding.btnLocation.setEnabled(true);
                    if (location != null) {
                        currentLocation = location;
                        updateLocationDisplay(location);
                    } else {
                        binding.tvLocationStatus.setText("Unable to get location");
                    }
                })
                .addOnFailureListener(e -> {
                    binding.btnLocation.setEnabled(true);                binding.tvLocationStatus.setText("Location error");
                    Toast.makeText(requireContext(), getString(R.string.create_post_location_error, e.getMessage()), 
                                   Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLocationDisplay(Location location) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String locationName = address.getLocality();
                if (locationName == null) locationName = address.getSubAdminArea();
                if (locationName == null) locationName = address.getAdminArea();
                if (locationName == null) locationName = "Unknown location";
                
                binding.tvLocationStatus.setText("📍 " + locationName);
            } else {
                binding.tvLocationStatus.setText(String.format(Locale.getDefault(), 
                        "📍 %.6f, %.6f", location.getLatitude(), location.getLongitude()));
            }
        } catch (IOException e) {
            binding.tvLocationStatus.setText(String.format(Locale.getDefault(), 
                    "📍 %.6f, %.6f", location.getLatitude(), location.getLongitude()));
        }
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                           Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
    }    private void createPost() {
        String content = binding.etContent.getText().toString().trim();
        if (content.isEmpty()) {
            binding.etContent.setError("Please enter some content");
            return;
        }

        binding.btnPost.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Get user data from local database
        executor.execute(() -> {
            UserEntity user = db.userDao().getUserByIdSync(currentUserId);
            if (user == null) {
                requireActivity().runOnUiThread(() -> {
                    binding.btnPost.setEnabled(true);                binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), getString(R.string.create_post_user_not_found), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // Create post object
            PostEntity post = new PostEntity();
            post.setUserId(currentUserId);
            post.setAuthorName(user.getName());
            post.setContent(content);
            post.setCategory(selectedCategory);
            post.setCreatedAt(System.currentTimeMillis());
            
            // Add location if available
            if (currentLocation != null) {
                post.setLatitude(currentLocation.getLatitude());
                post.setLongitude(currentLocation.getLongitude());
                post.setLocation(binding.tvLocationStatus.getText().toString().replace("📍 ", ""));
            }

            // Add metadata for plogging posts
            if ("plogging".equals(selectedCategory)) {
                String metadata = String.format(
                        "{\"created_via\":\"community_post\",\"has_image\":%b}",
                        imageUri != null
                );
                post.setMetadata(metadata);
            }

            // Set image path if exists
            if (imageUri != null && currentPhotoPath != null) {
                post.setImageUrl(currentPhotoPath);
            }

            requireActivity().runOnUiThread(() -> {
                savePost(post);
            });
        });
    }    private void savePost(PostEntity post) {
        executor.execute(() -> {
            try {
                repository.insertPost(post, insertedPost -> {
                    requireActivity().runOnUiThread(() -> {
                        binding.btnPost.setEnabled(true);
                        binding.progressBar.setVisibility(View.GONE);                    Toast.makeText(requireContext(), getString(R.string.create_post_success), Toast.LENGTH_SHORT).show();
                        navigateBack();
                    });
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    binding.btnPost.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), getString(R.string.create_post_error, e.getMessage()), 
                                   Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void removeImage() {
        imageUri = null;
        currentPhotoPath = null;
        binding.ivSelectedImage.setVisibility(View.GONE);
        binding.btnRemoveImage.setVisibility(View.GONE);
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    if (currentPhotoPath != null) {
                        imageUri = Uri.fromFile(new File(currentPhotoPath));
                        displaySelectedImage();
                    }
                    break;
                    
                case REQUEST_IMAGE_GALLERY:
                    if (data != null && data.getData() != null) {
                        imageUri = data.getData();
                        // For gallery images, we need to copy to our app directory
                        // This is simplified - in production, you'd handle this properly
                        displaySelectedImage();
                    }
                    break;
            }
        }
    }

    private void displaySelectedImage() {
        if (imageUri != null) {
            binding.ivSelectedImage.setVisibility(View.VISIBLE);
            binding.btnRemoveImage.setVisibility(View.VISIBLE);
            
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivSelectedImage);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();            } else {
                Toast.makeText(requireContext(), getString(R.string.create_post_location_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}