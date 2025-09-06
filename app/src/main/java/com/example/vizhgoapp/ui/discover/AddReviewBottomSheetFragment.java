package com.example.vizhgoapp.ui.discover;

import android.os.Bundle;

import androidx.annotation.Nullable;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import com.example.vizhgoapp.R;
import com.example.vizhgoapp.model.Landmark;
import com.example.vizhgoapp.model.Review;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;

import java.util.Date;

public class AddReviewBottomSheetFragment extends BottomSheetDialogFragment {

    public AddReviewBottomSheetFragment() {}

    private static final String TAG = "AddReviewBottomSheet";

    private static final String ARG_LANDMARK = "landmark";

    // UI Components
    private RatingBar ratingBar;
    private TextInputEditText etReview;
    private MaterialButton btnCancel, btnSubmit;

    // Data
    private Landmark landmark;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Callback interface to notify parent fragment when review is added
    public interface OnReviewAddedListener {
        void onReviewAdded();
    }

    private OnReviewAddedListener reviewAddedListener;

    public static AddReviewBottomSheetFragment newInstance(Landmark landmark) {
        AddReviewBottomSheetFragment fragment = new AddReviewBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LANDMARK, landmark);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            landmark = (Landmark) getArguments().getSerializable(ARG_LANDMARK);
        }

        // Initilize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_review_bottom_sheet, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        ratingBar = view.findViewById(R.id.ratingBar);
        etReview = view.findViewById(R.id.etReview);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSubmit = view.findViewById(R.id.btnSubmit);
    }

    private void setupClickListeners() {
        btnCancel.setOnClickListener(v -> dismiss());

        btnSubmit.setOnClickListener(v -> submitReview());
    }


    private void submitReview() {
        //Validate input
        float ratingFloat = ratingBar.getRating();
        String reviewText = etReview.getText().toString().trim();

        if (ratingFloat == 0) {
            Toast.makeText(getContext(), getString(R.string.review_please_select_rating), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(reviewText)) {
            Toast.makeText(getContext(), getString(R.string.review_please_add_review), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), getString(R.string.review_must_be_logged_in), Toast.LENGTH_SHORT).show();
            return;
        }

        //Disable submit button to prevent double submission
        btnSubmit.setEnabled(false);

        Review review = new Review();
        review.setUserId(currentUser.getUid());
        review.setRating(Math.round(ratingFloat));
        review.setText(reviewText);
        review.setCreatedAt(new Date());

        // Fetch username and save review
        fetchUsername(review, ratingFloat, reviewText);
    }


    private void fetchUsername(Review review, float ratingFloat, String reviewText) {
        FirebaseUser currentUser = auth.getCurrentUser();
        String displayName = currentUser.getDisplayName();

        if (displayName == null || displayName.trim().isEmpty()) {
            // Fetch from Firestore users collection
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        String fetchedUsername = null;

                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                fetchedUsername = document.getString("username");
                                if (fetchedUsername == null) {
                                    fetchedUsername = document.getString("displayName");
                                }
                            }
                        }

                        // Use fetched username if available, otherwise fallback to email
                        if (fetchedUsername != null && !fetchedUsername.trim().isEmpty()) {
                            review.setCreatedBy(fetchedUsername);
                        } else {
                            // Single fallback for both success and failure cases
                            if (currentUser.getEmail() != null) {
                                String email = currentUser.getEmail();
                                String emailUsername = email.substring(0, email.indexOf("@"));
                                review.setCreatedBy(emailUsername);
                            } else {
                                review.setCreatedBy(getString(R.string.unknown_user));
                            }
                        }

                        saveReviewToFirestore(review);
                    });
        } else {
            // Display name exists, use it
            review.setCreatedBy(displayName);
            saveReviewToFirestore(review);
        }
    }

    private void saveReviewToFirestore(Review review) {
        // Get references
        DocumentReference landmarkRef = db.collection("landmarks").document(landmark.getId());
        DocumentReference reviewRef = landmarkRef.collection("reviews").document(auth.getCurrentUser().getUid());

        // First check if review already exists
        reviewRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    WriteBatch batch = db.batch();

                    if (documentSnapshot.exists()) {
                        // Review exists - update
                        Review existingReview = documentSnapshot.toObject(Review.class);
                        int oldRating = existingReview != null ? existingReview.getRating() : 0;
                        int newRating = review.getRating();
                        int ratingDifference = newRating - oldRating;

                        // Update the review
                        batch.set(reviewRef, review);

                        // Only update totalScore (totalRatings stays the same)
                        if (ratingDifference != 0) {
                            batch.update(landmarkRef, "totalScore", FieldValue.increment(ratingDifference));
                        }
                    } else {
                        // Add the new review
                        batch.set(reviewRef, review);

                        // Increment both counters
                        batch.update(landmarkRef,
                                "totalRatings", FieldValue.increment(1),
                                "totalScore", FieldValue.increment(review.getRating())
                        );
                    }

                    // Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Review saved and landmark counters updated successfully");
                                Toast.makeText(getContext(), getString(R.string.review_added_successfully), Toast.LENGTH_SHORT).show();

                                if (reviewAddedListener != null) {
                                    reviewAddedListener.onReviewAdded();
                                }

                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error saving review or updating counters", e);
                                Toast.makeText(getContext(), getString(R.string.review_saving_error), Toast.LENGTH_SHORT).show();
                                btnSubmit.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking existing review", e);
                    Toast.makeText(getContext(), getString(R.string.review_checking_error), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                });
    }


    // Method to set the callback listener
    public void setOnReviewAddedListener(OnReviewAddedListener listener) {
        this.reviewAddedListener = listener;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        reviewAddedListener = null;
    }

}