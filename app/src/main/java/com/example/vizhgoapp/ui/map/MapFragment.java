package com.example.vizhgoapp.ui.map;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.vizhgoapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


public class MapFragment extends Fragment implements OnMapReadyCallback {

    public MapFragment() {
    }


    private static final String TAG = "MapFragment";

    private GoogleMap mMap;
    private FirebaseFirestore db;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        //Initialize Firestore
        db = FirebaseFirestore.getInstance();

        setupMapFragment();

        return view;
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom in and zoom out buttons
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Set default location - Bulgaria
        LatLng defaultBulgaria = new LatLng(42.7339, 25.4858);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultBulgaria, 6));

        // Load all landmarks
        loadLandmarksFromFirestore();
    }

    private void loadLandmarksFromFirestore() {
        db.collection("landmarks")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            // Clear any existing markers
                            mMap.clear();

                            // Add markers for each landmark
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    // Get landmark data from document
                                    String name = document.getString("name");
                                    String landmarkCategory = document.getString("category");
                                    Double latitude = document.getDouble("latitude");
                                    Double longitude = document.getDouble("longitude");

                                    // Check if required fields exist
                                    if (name != null && latitude != null && longitude != null) {
                                        LatLng landmarkLocation = new LatLng(latitude, longitude);

                                        // Get marker color based on the class
                                        float markerColor = getMarkerColor(landmarkCategory);

                                        mMap.addMarker(new MarkerOptions()
                                                .position(landmarkLocation)
                                                .title(name)
                                                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                                    } else {
                                        Log.w(TAG, "Landmark missing required fields: " + document.getId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing landmark: " + document.getId(), e);
                                }
                            }
                        } else {
                            Log.w(TAG, "No landmarks collection found");
                            Toast.makeText(getContext(),
                                    getText(R.string.no_landmarks_found),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error getting landmarks", task.getException());
                        Toast.makeText(getContext(), getString(R.string.landmarks_failed_to_load), Toast.LENGTH_SHORT).show();
                    }
                });
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
                return BitmapDescriptorFactory.HUE_RED;
        }
    }
}