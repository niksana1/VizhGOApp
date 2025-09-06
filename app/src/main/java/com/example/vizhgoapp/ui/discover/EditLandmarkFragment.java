package com.example.vizhgoapp.ui.discover;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.vizhgoapp.R;
import com.example.vizhgoapp.adapters.CategoryDropdownAdapter;
import com.example.vizhgoapp.model.Landmark;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;


public class EditLandmarkFragment extends Fragment implements OnMapReadyCallback {

    public EditLandmarkFragment() {}

    private static final int PICK_IMAGE_REQUEST = 1;

    //UI elements
    private EditText etName, etDescription, etLink;
    private Button btnUploadImg, btnEditLandmark;
    private ImageView imageView;
    private AutoCompleteTextView categoryDropdown, subcategoryDropdown;
    private TextInputLayout subcategoryLayout;
    private TextView tvLocationInfo;
    private ProgressBar progressBar;


    //Map varables
    private GoogleMap mMap;
    private LatLng selectedLocation;
    private Marker selectedMarker;


    //Data variables
    private String selectedCategoryKey;
    private String selectedSubcategoryKey;

    // Image variables
    private Uri selectedImageUri;
    private String uploadedImageUrl;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // Edit variables
    private Landmark existingLandmark;
    private boolean isEditMode = false;
    private static final String ARG_LANDMARK = "landmark";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_landmark, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        initializeViews(view);
        setupCategoryDropdown();
        setupMapFragment();
        setupClickListeners();

        if (isEditMode) {
            populateExistingData();
        }

        return view;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            existingLandmark = (Landmark) getArguments().getSerializable(ARG_LANDMARK);
            isEditMode = existingLandmark != null;
        }
    }

    private void initializeViews(View view) {
        etName = (EditText) view.findViewById(R.id.et_name);
        etDescription = (EditText) view.findViewById(R.id.et_description);
        etLink = (EditText) view.findViewById(R.id.et_link);
        btnUploadImg = (Button) view.findViewById(R.id.btn_upload_img);
        imageView = (ImageView) view.findViewById(R.id.imageView);
        categoryDropdown = (AutoCompleteTextView) view.findViewById(R.id.category_dropdown);
        subcategoryDropdown = (AutoCompleteTextView) view.findViewById(R.id.subcategory_dropdown);
        subcategoryLayout = (TextInputLayout) view.findViewById(R.id.subcategory_layout);
        tvLocationInfo = (TextView) view.findViewById(R.id.tv_location_info);
        btnEditLandmark = (Button) view.findViewById(R.id.btn_edit_landmark);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar_add_landmark);
    }

    private void populateExistingData() {
        if (!isEditMode || existingLandmark == null) return;

        // Populate text fields
        etName.setText(existingLandmark.getName());
        etDescription.setText(existingLandmark.getDescription());
        etLink.setText(existingLandmark.getLink());

        // Set location
        selectedLocation = new LatLng(existingLandmark.getLatitude(), existingLandmark.getLongitude());
        tvLocationInfo.setText(String.format("Избрани: %.4f, %.4f", selectedLocation.latitude, selectedLocation.longitude));

        // Load existing image
        if (existingLandmark.getPictureUrl() != null && !existingLandmark.getPictureUrl().isEmpty()) {
            uploadedImageUrl = existingLandmark.getPictureUrl();
            Glide.with(this)
                    .load(existingLandmark.getPictureUrl())
                    .fitCenter()
                    .into(imageView);
            btnUploadImg.setText(getString(R.string.add_landmark_btn_change_image));
        }

        // Set categories - handle this after setupCategoryDropdown
        selectedCategoryKey = existingLandmark.getCategory();
        selectedSubcategoryKey = existingLandmark.getSubCategory();
    }


    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }
    }

    private void setupCategoryDropdown() {
        CategoryDropdownAdapter.setupCategoryDropdown(
                getContext(),
                categoryDropdown,
                (categoryKey, position) -> {
                    selectedCategoryKey = categoryKey;
                    CategoryDropdownAdapter.setupSubcategoryDropdown(
                            getContext(),
                            subcategoryDropdown,
                            selectedCategoryKey,
                            (subcategoryKey, pos) -> selectedSubcategoryKey = subcategoryKey
                    );
                    subcategoryLayout.setVisibility(View.VISIBLE);
                }
        );
    }

    private void setupClickListeners() {
        btnUploadImg.setOnClickListener(v -> openImagePicker());
        btnEditLandmark.setOnClickListener(v -> saveLandmarkToFirestore());
    }

    private void openImagePicker(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK
                && data != null && data.getData() != null){
            selectedImageUri = data.getData();

            //Display selected image in ImageView usign Glide
            Glide.with(this)
                    .load(selectedImageUri)
                    .fitCenter()
                    .into(imageView);

            // Update button text to show img is selecteed
            btnUploadImg.setText(getString(R.string.add_landmark_btn_change_image));

            Toast.makeText(getContext(), getString(R.string.add_landmark_picture_selected_successfully), Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadImage() {
        if (selectedImageUri != null) {
            // Upload new image (existing code)
            btnEditLandmark.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            String fileName = "images/" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child(fileName);

            imageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            uploadedImageUrl = uri.toString();
                            saveLandmark();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), getString(R.string.add_landmark_error_uploading_image), Toast.LENGTH_SHORT).show();
                        btnEditLandmark.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                    });
        } else if (isEditMode && uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
            // Edit mode with existing image - no need to upload
            saveLandmark();
        } else {
            Toast.makeText(getContext(), getString(R.string.add_landmark_select_picture), Toast.LENGTH_SHORT).show();
        }
    }


    // Get the user's username, then call the createAndSaveLandmark function to save to firestore
    private void saveLandmark() {
        String currentUserId = auth.getCurrentUser().getUid();
        String username = auth.getCurrentUser().getDisplayName();

        if (username == null || username.trim().isEmpty()) {
            // If no display name, fetch from Firestore users collection
            db.collection("users").document(currentUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            String fetchedUsername = null;
                            if (document.exists()) {
                                fetchedUsername = document.getString("username");
                                if (fetchedUsername == null) {
                                    fetchedUsername = document.getString("displayName");
                                }
                            }
                            // Use fetched username or fallback to email
                            String finalUsername = (fetchedUsername != null && !fetchedUsername.trim().isEmpty())
                                    ? fetchedUsername : auth.getCurrentUser().getEmail();
                            createAndSaveLandmark(finalUsername);
                        } else {
                            // Fallback to email if Firestore fetch fails
                            createAndSaveLandmark(auth.getCurrentUser().getEmail());
                        }
                    });
        } else {
            // Use display name from Firebase Auth
            createAndSaveLandmark(username);
        }

    }

    private void createAndSaveLandmark(String username) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser.getUid();

        Landmark landmark;
        DocumentReference landmarkRef;

        if (isEditMode) {
            // Edit mode - use existing landmark and its ID
            landmark = existingLandmark;
            landmarkRef = db.collection("landmarks").document(existingLandmark.getId());
        } else {
            // Add mode - create new landmark with new ID
            landmarkRef = db.collection("landmarks").document();
            landmark = new Landmark();
            landmark.setId(landmarkRef.getId());
            landmark.setUserId(userId);
            landmark.setCreatedAt(new Date());
            landmark.setCreatedBy(username);
        }

        // Update landmark data (both add and edit modes)
        landmark.setName(etName.getText().toString().trim());
        if (uploadedImageUrl != null) {
            landmark.setPictureUrl(uploadedImageUrl);
        }
        landmark.setDescription(etDescription.getText().toString().trim());
        landmark.setLink(etLink.getText().toString().trim());
        landmark.setCategory(selectedCategoryKey);
        landmark.setSubCategory(selectedSubcategoryKey);
        landmark.setLatitude(selectedLocation.latitude);
        landmark.setLongitude(selectedLocation.longitude);

        // Save using the document reference
        landmarkRef.set(landmark)
                .addOnSuccessListener(aVoid -> {
                    String message = isEditMode ?
                            getString(R.string.edit_landmark_updated_successfully) :
                            getString(R.string.add_landmark_added_successfully);

                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);

                    // Navigate back
                    if (isEditMode) {
                        // Go back to landmark details
                        NavHostFragment.findNavController(EditLandmarkFragment.this).popBackStack();
                    } else {
                        // Go to discover fragment
                        NavOptions opts = new NavOptions.Builder()
                                .setPopUpTo(R.id.addLandmarkFragment, true)
                                .build();
                        NavHostFragment.findNavController(EditLandmarkFragment.this)
                                .navigate(R.id.discoverFragment, null, opts);
                    }
                })
                .addOnFailureListener(e -> {
                    String message = isEditMode ?
                            getString(R.string.edit_landmark_update_failed) :
                            getString(R.string.add_landmark_add_failed);

                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnEditLandmark.setEnabled(true);
                    btnEditLandmark.setText(isEditMode ?
                            getString(R.string.edit_landmark_button_text) :
                            getString(R.string.add_landmark_button_text));
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom in and zoom out buttons
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Set camera position
        if (isEditMode && selectedLocation != null) {
            // Edit mode - center on existing landmark
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));

            // Add marker for existing location
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(selectedLocation)
                    .title(getString(R.string.add_landmark_map_selected_location))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            // Add mode - show Bulgaria
            LatLng defaultBulgaria = new LatLng(42.7339, 25.4858);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultBulgaria, 6));
        }

        mMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;

            //Clear previous marker
            if (selectedMarker != null) {
                selectedMarker.remove();
            }

            // Add new marker
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.add_landmark_map_selected_location))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Update location info
            tvLocationInfo.setText(String.format("Избрани: %.4f, %.4f", latLng.latitude, latLng.longitude));
        });
    }

    private void saveLandmarkToFirestore() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError(getString(R.string.add_landmark_error_empty_name));
            etName.requestFocus();
            return;
        }

        if (selectedCategoryKey == null) {
            Toast.makeText(getContext(), getString(R.string.category_pick_name), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSubcategoryKey == null) {
            Toast.makeText(getContext(), getString(R.string.subcategory_pick_name), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedLocation == null) {
            Toast.makeText(getContext(), getString(R.string.add_landmark_map_select_location), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null && (!isEditMode || uploadedImageUrl == null || uploadedImageUrl.isEmpty())) {
            Toast.makeText(getContext(), getString(R.string.add_landmark_select_picture), Toast.LENGTH_SHORT).show();
            return;
        }


        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), getString(R.string.add_landmark_error_must_be_logged_in), Toast.LENGTH_SHORT).show();
            return;
        }

        uploadImage();
    }
}