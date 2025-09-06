package com.example.vizhgoapp.ui.profile;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vizhgoapp.model.ItemSelectListener;
import com.example.vizhgoapp.R;
import com.example.vizhgoapp.adapters.LandmarksAdapter;
import com.example.vizhgoapp.model.Landmark;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class ProfileFragment extends Fragment implements ItemSelectListener {

    public ProfileFragment() {}

    // Log errors using the fragment's tag
    private static final String TAG = "ProfileFragment";


    // UI Components
    private TextView tvUsername;
    private RecyclerView recyclerView;
    private ProgressDialog progressDialog;
    private TextView tvNoLandmarksFound;
    private Button btnLogout;

    // Data
    private LandmarksAdapter landmarksAdapter;
    private ArrayList<Landmark> userLandmarksArrayList;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, handle accordingly
            Toast.makeText(getContext(), "Please log in to view profile", Toast.LENGTH_SHORT).show();
            return view;
        }
        currentUserId = currentUser.getUid();

        initializeViews(view);
        setupRecyclerView();
        loadUserInfo();

        // Show progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.landmarks_adapter_fetching_data));
        progressDialog.show();

        loadUserLandmarks();

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.loginFragment);

        });

        return view;
    }

    private void initializeViews(View view) {
        tvUsername = (TextView) view.findViewById(R.id.tv_username);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_landmarks);
        tvNoLandmarksFound = (TextView) view.findViewById(R.id.tv_no_landmarks_found);
        btnLogout = (Button) view.findViewById(R.id.btn_logout);

    }

    private void setupRecyclerView() {
        userLandmarksArrayList = new ArrayList<Landmark>();
        landmarksAdapter = new LandmarksAdapter(getContext(), this.userLandmarksArrayList, this);

        // Set LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(landmarksAdapter);
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        String username = currentUser.getDisplayName();

        if (username == null || username.trim().isEmpty()) {
            // Try to get username from Firestore
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
                                    ? fetchedUsername : currentUser.getEmail();
                            tvUsername.setText(finalUsername);
                        } else {
                            // Fallback to email if Firestore fetch fails
                            tvUsername.setText(currentUser.getEmail());
                        }
                    });
        } else {
            // Use display name from Firebase Auth
            tvUsername.setText(username);
        }
    }

    // Load landmarks created by current user only
    private void loadUserLandmarks() {

        db.collection("landmarks")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                        if (error != null) {
                            if (progressDialog.isShowing())
                                progressDialog.dismiss();
                            Log.e(TAG, "Error fetching user landmarks: " + error.getMessage());
                            return;
                        }

                        // Clear existing data to avoid duplicates
                        userLandmarksArrayList.clear();

                        if (value != null && !value.isEmpty()) {

                            for (DocumentChange dc : value.getDocumentChanges()) {
                                if (dc.getType() == DocumentChange.Type.ADDED) {
                                    Landmark landmark = dc.getDocument().toObject(Landmark.class);
                                    userLandmarksArrayList.add(landmark);
                                }
                            }

                            // Show/hide no landmarks message
                            if (userLandmarksArrayList.isEmpty()) {
                                tvNoLandmarksFound.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                tvNoLandmarksFound.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            // No landmarks found
                            tvNoLandmarksFound.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }

                        landmarksAdapter.notifyDataSetChanged();

                        if (progressDialog != null && progressDialog.isShowing())
                            progressDialog.dismiss();
                    }
                });
    }

    @Override
    public void onItemClicked(Landmark landmark) {
        // Navigate to landmark details
        Bundle bundle = new Bundle();
        bundle.putSerializable("landmark", landmark);

        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.landmarkDetailsFragment, bundle);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}