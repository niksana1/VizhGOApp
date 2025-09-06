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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class LoginFragment extends Fragment {

    public LoginFragment() {}

    // UI elements
    private EditText etEmail, etPassword;
    private Button btnLogin, btnGotoRegister, btnForgotPassword;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);


        etEmail = (EditText) view.findViewById(R.id.et_email);
        etPassword = (EditText) view.findViewById(R.id.et_password);
        btnLogin = (Button) view.findViewById(R.id.btn_login);
        btnGotoRegister = (Button) view.findViewById(R.id.btn_goTo_register);
        btnForgotPassword = (Button) view.findViewById(R.id.btn_forgot_password);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar_login);

        mAuth = FirebaseAuth.getInstance();

        View.OnClickListener listener = v -> {
            if(v.getId() == R.id.btn_login){
                loginUser();
            }
            else if (v.getId() == R.id.btn_goTo_register) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_loginFragment_to_registerFragment);
            }
            else if (v.getId() == R.id.btn_forgot_password) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment);
            }

        };
        btnLogin.setOnClickListener(listener);
        btnGotoRegister.setOnClickListener(listener);
        btnForgotPassword.setOnClickListener(listener);

        return view;
    }

    private void loginUser(){
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if(email.isEmpty()){
            etEmail.setError(getString(R.string.email_error_required));
            etEmail.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            etEmail.setError(getString(R.string.email_error_enter_valid));
            etEmail.requestFocus();
            return;
        }

        if(password.isEmpty() || (password.length() < 8)){
            etPassword.setError(getString(R.string.password_error_lenght));
            etPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()){
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.login_successful), Toast.LENGTH_SHORT).show();

                    // Redirect to 'profile fragment' and clear back stack
                    NavOptions opts = new NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)   // removes the Login from the back stack
                            .build();

                    NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.profileFragment, null, opts);
                 }
                else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.login_unsuccessful), Toast.LENGTH_LONG).show();
                    Log.e("LoginFragment", "Unexpected error", task.getException());
                }
            }
        });


    }
}