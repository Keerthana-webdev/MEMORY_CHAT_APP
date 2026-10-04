package com.example.vmessenger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class login extends AppCompatActivity {

    TextView logsignup;
    Button button;
    EditText email, password;

    FirebaseAuth auth;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();

        // If already logged in, go directly to MainActivity
        if (auth.getCurrentUser() != null) {

            Intent intent = new Intent(login.this, MainActivity.class);

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.setCancelable(false);

        button = findViewById(R.id.logbutton);
        email = findViewById(R.id.editTexLogEmail);
        password = findViewById(R.id.editTextLogPassword);
        logsignup = findViewById(R.id.logsignup);

        // SIGN UP
        logsignup.setOnClickListener(v -> {

            Intent intent = new Intent(
                    login.this,
                    registration.class
            );

            startActivity(intent);
        });

        // LOGIN
        button.setOnClickListener(v -> {

            String Email = email.getText().toString().trim();
            String pass = password.getText().toString();

            // Email empty
            if (TextUtils.isEmpty(Email)) {

                email.setError("Enter your email");
                email.requestFocus();
                return;
            }

            // Password empty
            if (TextUtils.isEmpty(pass)) {

                password.setError("Enter your password");
                password.requestFocus();
                return;
            }

            // Basic email validation
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(Email).matches()) {

                email.setError("Enter a valid email address");
                email.requestFocus();
                return;
            }

            // Password validation
            if (pass.length() < 6) {

                password.setError(
                        "Password must contain at least 6 characters"
                );

                password.requestFocus();
                return;
            }

            progressDialog.show();

            auth.signInWithEmailAndPassword(Email, pass)
                    .addOnCompleteListener(
                            login.this,
                            new OnCompleteListener<AuthResult>() {

                                @Override
                                public void onComplete(
                                        @NonNull Task<AuthResult> task) {

                                    progressDialog.dismiss();

                                    if (task.isSuccessful()) {

                                        Toast.makeText(
                                                login.this,
                                                "Login Successful",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        Intent intent = new Intent(
                                                login.this,
                                                MainActivity.class
                                        );

                                        intent.setFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        );

                                        startActivity(intent);
                                        finish();

                                    } else {

                                        Exception exception =
                                                task.getException();

                                        String errorMessage;

                                        if (exception != null) {
                                            errorMessage =
                                                    exception.getMessage();
                                        } else {
                                            errorMessage =
                                                    "Unknown Firebase login error";
                                        }

                                        Toast.makeText(
                                                login.this,
                                                errorMessage,
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                            }
                    );
        });
    }
}