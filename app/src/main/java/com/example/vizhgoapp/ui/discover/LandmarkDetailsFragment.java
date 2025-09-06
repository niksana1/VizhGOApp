package com.example.vizhgoapp.ui.discover;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.vizhgoapp.R;
import com.example.vizhgoapp.adapters.ReviewsAdapter;
import com.example.vizhgoapp.model.Landmark;
import com.example.vizhgoapp.model.Review;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;


public class LandmarkDetailsFragment extends Fragment implements OnMapReadyCallback {

    public LandmarkDetailsFragment() {}

    // Debugging
    private static final String TAG = "LandmarkDetailsFragment";

    private static final String ARG_LANDMARK = "landmark";


    private boolean isUserAdmin = false;

    // UI Components
    private ImageView ivLandmarkImage;
    private TextView tvLandmarkName;
    private TextView tvSubcategory;
    private TextView tvAvgRating;
    private TextView tvDescription;
    private TextView tvLocationInfo;
    private TextView tvAddedBy;
    private TextView tvAddedOnDate;
    private TextView tvNoReviews;
    private LinearLayout layoutEditDeleteButtons;
    private MaterialButton btnEdit, btnDelete;
    private MaterialButton btnVisitLink, btnAddReview;
    private RecyclerView recyclerView;

    // Data
    private Landmark landmark;
    private GoogleMap mMap;
    private ReviewsAdapter reviewsAdapter;
    private ArrayList<Review> reviewsArrayList;

    // Firebase
    private FirebaseFirestore db;


    // Factory method using Serializable
    public static LandmarkDetailsFragment newInstance(Landmark landmark) {
        LandmarkDetailsFragment fragment = new LandmarkDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LANDMARK, landmark);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            landmark = (Landmark) getArguments().getSerializable(ARG_LANDMARK);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_landmark_details, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        setupMapFragment();
        populateLandmarkData();
        setupWebsiteButton();
        setupRecyclerView();
        recyclerView.setAdapter(reviewsAdapter);

        setupAddReviewButton();
        loadReviewsFromFirestore();

        return view;
    }

    private void initializeViews(View view) {
        ivLandmarkImage = view.findViewById(R.id.iv_landmark_image);
        tvLandmarkName = view.findViewById(R.id.tv_landmark_name);
        tvSubcategory = view.findViewById(R.id.tv_subcategory);
        tvAvgRating = view.findViewById(R.id.tv_landmark_avg_rating);
        tvDescription = view.findViewById(R.id.tv_description);
        tvLocationInfo = view.findViewById(R.id.tv_location_info);
        tvAddedBy = view.findViewById(R.id.tv_added_by);
        tvAddedOnDate = view.findViewById(R.id.tv_added_on_date);
        tvNoReviews = view.findViewById(R.id.tv_no_reviews);
        btnVisitLink = view.findViewById(R.id.btn_visit_link);
        btnAddReview = view.findViewById(R.id.btn_add_review);

        layoutEditDeleteButtons = view.findViewById(R.id.layout_edit_delete_buttons);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnDelete = view.findViewById(R.id.btn_delete);

        recyclerView = view.findViewById(R.id.recycler_view_reviews);
    }

    private void setupRecyclerView() {
        reviewsArrayList = new ArrayList<Review>();
        reviewsAdapter = new ReviewsAdapter(getContext(), this.reviewsArrayList);

        recyclerView.setLayoutManager((new LinearLayoutManager(getContext())));
        recyclerView.setAdapter(reviewsAdapter);
        recyclerView.setNestedScrollingEnabled(false);

        updateReviewsDisplay();
    }
    private void updateReviewsDisplay() {
        if (reviewsArrayList.isEmpty()) {
            tvNoReviews.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoReviews.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void populateLandmarkData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        tvLandmarkName.setText(landmark.getName());

        // Set subcategory
        if (landmark.getSubCategory() != null && !landmark.getSubCategory().isEmpty()) {
            String translatedCategory = getTranslatedSubcategoryName(landmark.getSubCategory());
            tvSubcategory.setText(translatedCategory);
        } else {
            tvSubcategory.setText(getString(R.string.missing_category));
        }

        // Set Rating Display
        if (landmark.getRatingDisplay() != null && !landmark.getRatingDisplay().isEmpty()){
            tvAvgRating.setText(landmark.getRatingDisplay());
        }

        // Set description
        if (landmark.getDescription() != null && !landmark.getDescription().isEmpty()) {
            tvDescription.setText(landmark.getDescription());
        } else {
            tvDescription.setText(getText(R.string.description_missing));
        }

        // Set coordinates
        tvLocationInfo.setText(String.format("Координати: %.4f, %.4f", landmark.getLatitude(), landmark.getLongitude()));

        // Set image using Glide (make sure you have Glide dependency in your build.gradle)
        if (landmark.getPictureUrl() != null && !landmark.getPictureUrl().isEmpty()) {
            Glide.with(this)
                    .load(landmark.getPictureUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_placeholder_image)
                    .into(ivLandmarkImage);
        }

        // Set added by user
        if (landmark.getCreatedBy() != null && !landmark.getCreatedBy().isEmpty()) {
            tvAddedBy.setText(landmark.getCreatedBy());
        } else {
            tvAddedBy.setText(getString(R.string.unknown_user));
        }

        // Set creation date
        if (landmark.getCreatedAt() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(landmark.getCreatedAt());
            tvAddedOnDate.setText(formattedDate);
        } else {
            tvAddedOnDate.setText(getString(R.string.unknown_date));
        }

        //Check if the user is admin or owner then update the EDIT/DELETE buttons
        checkIfUserIsAdmin();
        updateButtonVisibility();
    }

    // First check if the user is admin, then update the EDIT/DELETE buttons
    private void checkIfUserIsAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            isUserAdmin = false;
            updateButtonVisibility();
            return;
        }

        String currentUid = currentUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.vizhgoapp.model.User user = documentSnapshot.toObject(com.example.vizhgoapp.model.User.class);
                        isUserAdmin = (user != null && user.isAdmin());
                    } else {
                        isUserAdmin = false;
                    }

                    updateButtonVisibility();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking admin status", e);
                    isUserAdmin = false;
                    updateButtonVisibility();
                });
    }


    private void updateButtonVisibility() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Check if user is Author or Admin
        boolean isOwner = currentUser != null && landmark != null &&
                landmark.getUserId() != null &&
                landmark.getUserId().equals(currentUser.getUid());

        boolean canEdit = isOwner || isUserAdmin;

        if (canEdit) {
            // Show buttons for landmark author & admin
            layoutEditDeleteButtons.setVisibility(View.VISIBLE);
            btnEdit.setOnClickListener(v -> editLandmark());
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        } else {
            // Hide buttons for non-owners
            layoutEditDeleteButtons.setVisibility(View.GONE);
        }
    }


    private String getTranslatedSubcategoryName(String subcategoryKey) {
        Context context = getContext();

        // Check if context is null
        if (context == null) {
            Log.w(TAG, "Context is null, returning original subcategory key");
            return subcategoryKey != null ? subcategoryKey : "Unknown";
        }
        if (subcategoryKey == null || subcategoryKey.isEmpty()) {
            return context.getString(R.string.missing_subcategory); // add this string resource
        }

        int stringId = context.getResources().getIdentifier("subcat_" + subcategoryKey,
                "string", context.getPackageName());

        if (stringId != 0) {
            return getContext().getString(stringId);
        } else {
            Log.w("LandmarksAdapter", "Translation not found for subcategory: " + subcategoryKey);
            return subcategoryKey; // fallback to key if translation not found
        }
    }

    private void setupAddReviewButton() {
        btnAddReview.setOnClickListener(v -> {
            AddReviewBottomSheetFragment bottomSheet = AddReviewBottomSheetFragment.newInstance(landmark);

            // Set callback to refresh reviews when new review is added
            bottomSheet.setOnReviewAddedListener(() -> {
                // Refresh your reviews list here
                loadReviewsFromFirestore();
                reloadLandmarkData();
            });

            bottomSheet.show(getParentFragmentManager(), "AddReviewBottomSheet");
        });
    }

    private void loadReviewsFromFirestore() {
        if (landmark == null || landmark.getId() == null) {
            Log.e(TAG, "Landmark or landmark ID is null");
            return;
        }
        Log.d(TAG, "Loading reviews for landmark: " + landmark.getId());

        db.collection("landmarks")
                .document(landmark.getId())
                .collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING) // Show newest reviews first
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " reviews");


                    reviewsArrayList.clear(); // clear existing reviews to avoid duplicates when reloading

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Review review = document.toObject(Review.class);
                            reviewsArrayList.add(review);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing review: " + e.getMessage());
                        }
                    }

                    // Notify adapter and update display
                    if (reviewsAdapter != null) {
                        reviewsAdapter.notifyDataSetChanged();
                    }
                    updateReviewsDisplay();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews", e);
                    Toast.makeText(getContext(), "Error loading reviews", Toast.LENGTH_SHORT).show();
                    updateReviewsDisplay();
                });
    }


    // Add this new method to reload landmark data:
    private void reloadLandmarkData() {
        if (landmark == null || landmark.getId() == null) return;

        db.collection("landmarks")
                .document(landmark.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Landmark updatedLandmark = documentSnapshot.toObject(Landmark.class);
                        if (updatedLandmark != null) {
                            this.landmark = updatedLandmark;
                            updateLandmarkDisplay(); // Update the UI with new rating info
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error reloading landmark data", e);
                });
    }


    private void updateLandmarkDisplay() {
        // Update UI to show the landmark rating
        if (tvAvgRating != null) {
            tvAvgRating.setText(landmark.getRatingDisplay());
        }
    }

    private void setupWebsiteButton() {
        if (landmark != null && landmark.getLink() != null && !landmark.getLink().isEmpty()) {
            btnVisitLink.setVisibility(View.VISIBLE);
            btnVisitLink.setOnClickListener(v -> openLink(landmark.getLink()));
        } else {
            btnVisitLink.setVisibility(View.GONE);
        }
    }

    private void openLink(String url) {
        try{
            // Add http:// if not present
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening website: " + e.getMessage());
            Toast.makeText(getContext(), getString(R.string.error_loading_website), Toast.LENGTH_SHORT).show();
        }
    }

    private void editLandmark() {
        Bundle bundle = new Bundle();
        bundle.putSerializable("landmark", landmark);

        NavHostFragment.findNavController(this)
                .navigate(R.id.editLandmarkFragment, bundle);
    }

    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_landmark))
                .setMessage(getString(R.string.delete_landmark_message))
                .setPositiveButton(getString(R.string.btn_delete), (dialog, which) -> deleteLandmark())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void deleteLandmark() {
        if (landmark == null || landmark.getId() == null) return;

        //First delete the image from Firebase Storage, then call deleteLandmarkDocument
        if (landmark.getPictureUrl() != null && !landmark.getPictureUrl().isEmpty()) {
            try {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference imageRef = storage.getReferenceFromUrl(landmark.getPictureUrl());

                imageRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            // After image is deleted, delete the landmark document
                            deleteLandmarkDocument();
                        })
                        .addOnFailureListener(e -> {
                            // Even if image deletion fails, still delete the document
                            deleteLandmarkDocument();
                        });
            } catch (Exception e) {
                Log.w(TAG, "Error parsing image URL, proceeding with document deletion", e);
                deleteLandmarkDocument();
            }
        } else {
            // No image to delete, go straight to document deletion
            deleteLandmarkDocument();
        }
    }

    private void deleteLandmarkDocument() {

        db.collection("landmarks")
                .document(landmark.getId())
                .collection("reviews")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();

                    // Add all reviews to the batch
                    for (QueryDocumentSnapshot reviewDoc : queryDocumentSnapshots) {
                        batch.delete(reviewDoc.getReference());
                    }

                    // Add landmark deletion to the batch
                    DocumentReference landmarkRef = db.collection("landmarks").document(landmark.getId());
                    batch.delete(landmarkRef);

                    // Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Landmark and all reviews deleted successfully");
                                Toast.makeText(getContext(), getString(R.string.landmark_deleted_successfully), Toast.LENGTH_SHORT).show();
                                NavHostFragment.findNavController(this).popBackStack();
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error fetching reviews for deletion, deleting landmark only", e);
                                // If we can't fetch reviews, just delete the landmark document
                                db.collection("landmarks")
                                        .document(landmark.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), R.string.landmark_deleted_successfully, Toast.LENGTH_SHORT).show();
                                            NavHostFragment.findNavController(this).popBackStack();
                                        });
                            });
                });
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (landmark != null) {
            LatLng landmarkLocation = new LatLng(landmark.getLatitude(), landmark.getLongitude());

            // Get marker color based on the class
            float markerColor = getMarkerColor(landmark.getCategory());

            mMap.addMarker(new MarkerOptions()
                    .position(landmarkLocation)
                    .title(landmark.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            // Move camera above the location
            mMap.moveCamera((CameraUpdateFactory.newLatLngZoom(landmarkLocation, 15)));

            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(true);

        } else {
            Log.e(TAG, "Landmark location data is not available");
            Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
        }
    }
    private float getMarkerColor(String landmarkCategory) {
        switch (landmarkCategory.toLowerCase()) {
            case "natural_landmarks":
                return BitmapDescriptorFactory.HUE_GREEN;
            case "historical_landmarks":
                return BitmapDescriptorFactory.HUE_RED;
            case "culture_and_art":
                return BitmapDescriptorFactory.HUE_BLUE;
            default:
                return BitmapDescriptorFactory.HUE_RED; // Default color
        }
    }
}