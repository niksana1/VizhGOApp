package com.example.vizhgoapp.ui.discover;

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
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.example.vizhgoapp.model.ItemSelectListener;
import com.example.vizhgoapp.R;
import com.example.vizhgoapp.adapters.LandmarksAdapter;
import com.example.vizhgoapp.model.Landmark;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;


public class DiscoverFragment extends Fragment implements ItemSelectListener {

    public DiscoverFragment() {}

    private static final String TAG = "DiscoverFragment";

    // UI Components
    private RecyclerView recyclerView;
    private ProgressDialog progressDialog;
    private TextView tvNoLandmarksFound;

    // Data
    private LandmarksAdapter landmarksAdapter;
    private ArrayList<Landmark> landmarksArrayList;

    // Firebase
    private FirebaseFirestore db;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        setupRecyclerView();

        // Showing progress dialog after UI is set up
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.landmarks_adapter_fetching_data));
        progressDialog.show();

        EventChangeListener();

        recyclerView.setAdapter(landmarksAdapter);

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_landmarks);
        tvNoLandmarksFound = (TextView) view.findViewById(R.id.tv_no_landmarks_found);
    }

    private void setupRecyclerView() {
        landmarksArrayList = new ArrayList<Landmark>();
        landmarksAdapter = new LandmarksAdapter(getContext(), this.landmarksArrayList, this);

        // Set LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(landmarksAdapter);
    }

    // .orderBy() (newest to oldest)
    private void EventChangeListener(){
        db.collection("landmarks").orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                        if (error != null){
                            if (progressDialog.isShowing())
                                progressDialog.dismiss();
                            Log.e(TAG, "Error fetching landmarks: " + error.getMessage());
                            return;
                        }

                        for (DocumentChange dc : value.getDocumentChanges()){

                            if (dc.getType() == DocumentChange.Type.ADDED){
                                Landmark landmark = dc.getDocument().toObject(Landmark.class);
                                landmarksArrayList.add(landmark);

                            }

                            landmarksAdapter.notifyDataSetChanged();

                            if (progressDialog != null && progressDialog.isShowing())
                                progressDialog.dismiss();
                        }

                    }
                });
    }

    @Override
    public void onItemClicked(Landmark landmark) {

        // Create bundle with landmark data
        Bundle bundle = new Bundle();
        bundle.putSerializable("landmark", landmark);

        // Navigate using NavController
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