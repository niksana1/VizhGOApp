package com.example.vizhgoapp.ui.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.vizhgoapp.R;
import com.example.vizhgoapp.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class RegisterFragment extends Fragment {

    public RegisterFragment() {}

    // UI elements
    EditText etUsername, etEmail, etPassword, etPasswordConfirm;
    Button btnRegister, btnGotoLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);


        etUsername = (EditText) view.findViewById(R.id.et_username);
        etEmail = (EditText) view.findViewById(R.id.et_email);
        etPassword = (EditText) view.findViewById(R.id.et_password);
        etPasswordConfirm = (EditText) view.findViewById(R.id.et_password_confirm);
        btnRegister = (Button) view.findViewById(R.id.btn_register);
        btnGotoLogin = (Button) view.findViewById(R.id.btn_goTo_login);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        View.OnClickListener listener = v -> {
            if(v.getId() == R.id.btn_register){
                registerUser();
            }
            else if (v.getId() == R.id.btn_goTo_login){
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_registerFragment_to_loginFragment);
            }
        };
        btnGotoLogin.setOnClickListener(listener);
        btnRegister.setOnClickListener(listener);

        return view;
    }

    private void registerUser(){
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etPasswordConfirm.getText().toString().trim();

        if(!validateInput(username, email, password, confirmPassword)){
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        checkUsernameAvailability(username, email, password);
    }

    private boolean validateInput(String username, String email, String password, String confirmPassword){
        if(username.isEmpty() || (username.length() < 6)){
            etUsername.setError(getString(R.string.username_error_lenght));
            etUsername.requestFocus();
            return false;
        }

        if(email.isEmpty()){
            etEmail.setError(getString(R.string.email_error_required));
            etEmail.requestFocus();
            return false;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            etEmail.setError(getString(R.string.email_error_enter_valid));
            etEmail.requestFocus();
            return false;
        }

        if(password.isEmpty() || (password.length() < 8)){
            etPassword.setError(getString(R.string.password_error_lenght));
            etPassword.requestFocus();
            return false;
        }
        if(!confirmPassword.equals(password)){
            etPasswordConfirm.setError(getString(R.string.password_error_dont_match));
            etPasswordConfirm.requestFocus();
            return false;
        }
        return true;
    }

    private void checkUsernameAvailability(String username, String email, String password){
        // Query Firestore to check if username alredy exists
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Username already exists
                            progressBar.setVisibility(View.GONE);
                            etUsername.setError(getString(R.string.username_error_already_Taken));
                            etUsername.requestFocus();
                        } else {
                            // Username is available, proceed with registration
                            createFirebaseUser(username, email, password);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e("RegisterFragment", "Error checking username availability", e);
                        Toast.makeText(getContext(), R.string.username_error_checking_username, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createFirebaseUser(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    FirebaseUser user = authResult.getUser();
                    if (user != null){
                        saveUserToFirestore(user.getUid(), username, email);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.e("RegisterFragment", "User is null after successful authentication");
                        Toast.makeText(getContext(), "Възникна грешка при регистрацията", Toast.LENGTH_SHORT).show();
                    }

                }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.registration_unsuccessful), Toast.LENGTH_SHORT).show();
                Log.e("RegisterFragment", "Unexpected error", e);
            }
        });
    }

    private void saveUserToFirestore(String userId, String username, String email) {
        // Create User object using the model class
        User user = new User(username, email);

        //Save to Firestore in "users" collection with uID as document ID
        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), getString(R.string.registration_successful), Toast.LENGTH_SHORT).show();
                        Log.d("RegisterFragment", "User data saved to Firestore successfully");

                        // Navigate to 'profile fragment' and clear back stack
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true) // removes both loginFragment and registerFragment from back stack
                                .build();

                        NavHostFragment.findNavController(RegisterFragment.this)
                                .navigate(R.id.profileFragment, null, navOptions);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e("RegisterFragment", "Error saving user data to Firestore", e);
                        Toast.makeText(getContext(), "Регистрацията е успешна, но има проблем със запазването на данните", Toast.LENGTH_LONG).show();

                        // Still navigate to profile even if Firestore save fails
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build();

                        NavHostFragment.findNavController(RegisterFragment.this)
                                .navigate(R.id.profileFragment, null, navOptions);
                    }
                });
    }

}