package com.example.vizhgoapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            //bottom padding is set to 0 to fix a bug, where when 3-button nav is open there is too much padding
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        setupToolbar();
        setupNavigation();
    }


    private void setupToolbar(){
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
    private void setupNavigation(){
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainerView);
        navController = navHostFragment.getNavController();

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.discoverFragment, R.id.addLandmarkFragment, R.id.mapFragment, R.id.profileFragment
        ).build();


        // Listener to keep bottom nav in sync with navigation
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

            // Clear selection if we're on auth fragment
            if (destination.getId() == R.id.loginFragment || destination.getId() == R.id.registerFragment) {
                Log.d(TAG, "On auth fragment, clearing bottom nav selection");
                bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
                for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
                    bottomNavigationView.getMenu().getItem(i).setChecked(false);
                }
                bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
            } else {
                // Update bottom nav selection to match current fragment
                MenuItem menuItem = bottomNavigationView.getMenu().findItem(destination.getId());
                if (menuItem != null && !menuItem.isChecked()) {
                    Log.d(TAG, "Updating bottom nav selection to match destination: " + destination.getId());
                    menuItem.setChecked(true);
                }
            }
        });

        // Custom click handler
        // Check if the user is logged in to navigate properly
        // open profile or login
        // intercept going to addLandmarkFragment if not logged in
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if(itemId == R.id.profileFragment){
                Log.d(TAG, "Profile tab clicked, handling authentication check");
                return handleProfileNavigation();
            } else if (itemId == R.id.addLandmarkFragment) {
                Log.d(TAG, "Add tab clicked, handling authentication check");
                return handleAddNavigation();
            } else {
                try {
                    navController.navigate(itemId);
                    Log.d(TAG, "Direct navigation successful");
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Direct navigation failed: " + e.getMessage());
                    return false;
                }
            }
        });
        Log.d(TAG, "Navigation setup completed");
    }

    private boolean handleProfileNavigation(){
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){
            // Not logged in -> go to loginFragment
            navController.navigate(R.id.loginFragment);
        } else {
            // Logged in -> go to profileFragment
            navController.navigate(R.id.profileFragment);
        }
        return true;
    }

    private boolean handleAddNavigation() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.login_to_open_add_fragment), Toast.LENGTH_SHORT).show();
            return false;
        } else {
            navController.navigate(R.id.addLandmarkFragment);
            return true;
        }
    }

    // Handle back button properly
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}

