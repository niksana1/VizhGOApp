package com.example.vizhgoapp.ui.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
import com.google.firebase.auth.FirebaseAuth;


public class ForgotPassword extends Fragment {

    public ForgotPassword() {}

    // UI elements
    private EditText etEmail;
    private Button btnResetPassword;

    private FirebaseAuth mAuth;
    private ProgressBar progressBar;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forgot_password, container, false);

        etEmail = (EditText) view.findViewById(R.id.et_email);
        btnResetPassword = (Button) view.findViewById(R.id.btn_reset_password);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar_login);

        mAuth = FirebaseAuth.getInstance();

        View.OnClickListener listener = v -> {
            if(v.getId() == R.id.btn_reset_password){
                resetPassword();
            }
        };
        btnResetPassword.setOnClickListener(listener);

        return view;
    }

    private void resetPassword(){
        String email = etEmail.getText().toString().trim();

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

        progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()){
                    Toast.makeText(getContext(), getString(R.string.email_check_for_new_password), Toast.LENGTH_LONG).show();
                } else{
                    Toast.makeText(getContext(), getString(R.string.email_error_check_for_new_password), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}