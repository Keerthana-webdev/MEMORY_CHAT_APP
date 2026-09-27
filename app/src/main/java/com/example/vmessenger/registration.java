package com.example.vmessenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import de.hdodenhof.circleimageview.CircleImageView;

public class registration extends AppCompatActivity {

    TextView loginbut;

    EditText rg_username;
    EditText rg_email;
    EditText rg_password;
    EditText rg_repassword;

    Button rg_signup;

    CircleImageView rg_profileImg;

    FirebaseAuth auth;

    FirebaseDatabase database;

    ProgressDialog progressDialog;

    Uri imageURI;

    String imageuri;

    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";


    @Override
    protected void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating your MemoryChat account...");
        progressDialog.setCancelable(false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        database = FirebaseDatabase.getInstance();

        auth = FirebaseAuth.getInstance();

        loginbut = findViewById(R.id.loginbut);

        rg_username = findViewById(R.id.rgusername);

        rg_email = findViewById(R.id.rgemail);

        rg_password = findViewById(R.id.rgpassword);

        rg_repassword = findViewById(R.id.rgrepassword);

        rg_profileImg = findViewById(R.id.profilerg0);

        rg_signup = findViewById(R.id.signupbutton);

        loginbut.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(registration.this, login.class);
                        startActivity(intent);
                        finish();
                    }
                }
        );

        rg_profileImg.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), 10);
                    }
                }
        );

        rg_signup.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        String namee = rg_username
                                        .getText()
                                        .toString()
                                        .trim();

                        String emaill = rg_email
                                        .getText()
                                        .toString()
                                        .trim();

                        String Password = rg_password
                                        .getText()
                                        .toString();

                        String cPassword = rg_repassword
                                        .getText()
                                        .toString();

                        String status = "Hey I'm Using MemoryChat";

                        if (TextUtils.isEmpty(namee) || TextUtils.isEmpty(emaill) || TextUtils.isEmpty(Password) || TextUtils.isEmpty(cPassword)) {
                            Toast.makeText(registration.this, "Please Enter Valid Information", Toast.LENGTH_SHORT).show();
                            return;
                        }


                        if (!emaill.matches(emailPattern)) {rg_email.setError("Type a valid email address");
                            return;
                        }


                        if (Password.length() < 6) {rg_password.setError("Password must be 6 characters or more");
                            return;
                        }


                        if (!Password.equals(cPassword)) {rg_repassword.setError("Password doesn't match");
                            return;
                        }


                        // ------------------------------------------------
                        // SHOW PROGRESS
                        // ------------------------------------------------

                        progressDialog.setMessage(
                                "Creating account..."
                        );

                        progressDialog.show();


                        // =================================================
                        // CREATE FIREBASE AUTH USER
                        // =================================================

                        auth.createUserWithEmailAndPassword(
                                emaill,
                                Password
                        ).addOnCompleteListener(
                                new OnCompleteListener<AuthResult>() {

                                    @Override
                                    public void onComplete(
                                            @NonNull Task<AuthResult> task
                                    ) {

                                        if (task.isSuccessful()) {

                                            String id =
                                                    task.getResult()
                                                            .getUser()
                                                            .getUid();


                                            DatabaseReference reference =
                                                    database
                                                            .getReference()
                                                            .child("user")
                                                            .child(id);


                                            // =================================================
                                            // IF USER SELECTED PROFILE PHOTO
                                            // =================================================

                                            if (imageURI != null) {

                                                progressDialog.setMessage(
                                                        "Uploading profile picture..."
                                                );


                                                CloudinaryUploader.uploadMedia(
                                                        registration.this,
                                                        imageURI,
                                                        new CloudinaryUploader.UploadCallback() {

                                                            @Override
                                                            public void onSuccess(
                                                                    String downloadUrl
                                                            ) {

                                                                imageuri =
                                                                        downloadUrl;


                                                                saveUserToFirebase(
                                                                        reference,
                                                                        id,
                                                                        namee,
                                                                        emaill,
                                                                        Password,
                                                                        imageuri,
                                                                        status
                                                                );
                                                            }


                                                            @Override
                                                            public void onFailure(
                                                                    String error
                                                            ) {

                                                                progressDialog.dismiss();

                                                                Toast.makeText(
                                                                        registration.this,
                                                                        "Image upload failed: "
                                                                                + error,
                                                                        Toast.LENGTH_LONG
                                                                ).show();
                                                            }
                                                        }
                                                );


                                            } else {

                                                // =================================================
                                                // NO PROFILE PHOTO
                                                // =================================================

                                                imageuri =
                                                        "";

                                                saveUserToFirebase(
                                                        reference,
                                                        id,
                                                        namee,
                                                        emaill,
                                                        Password,
                                                        imageuri,
                                                        status
                                                );
                                            }


                                        } else {

                                            progressDialog.dismiss();

                                            String error =
                                                    "Registration failed";

                                            if (task.getException() != null) {

                                                error =
                                                        task.getException()
                                                                .getMessage();
                                            }

                                            Toast.makeText(
                                                    registration.this,
                                                    error,
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                                }
                        );
                    }
                }
        );
    }


    // ============================================================
    // SAVE USER TO FIREBASE REALTIME DATABASE
    // ============================================================

    private void saveUserToFirebase(
            DatabaseReference reference,
            String id,
            String name,
            String email,
            String password,
            String profilePic,
            String status
    ) {

        Users users =
                new Users(
                        id,
                        name,
                        email,
                        password,
                        profilePic,
                        status
                );


        reference.setValue(users)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {

                            @Override
                            public void onComplete(
                                    @NonNull Task<Void> task
                            ) {

                                progressDialog.dismiss();

                                if (task.isSuccessful()) {

                                    Toast.makeText(
                                            registration.this,
                                            "Account Created Successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();


                                    Intent intent =
                                            new Intent(
                                                    registration.this,
                                                    MainActivity.class
                                            );

                                    startActivity(intent);

                                    finish();

                                } else {

                                    Toast.makeText(
                                            registration.this,
                                            "Error creating user",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                        }
                );
    }


    // ============================================================
    // IMAGE PICKER RESULT
    // ============================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {

        super.onActivityResult(
                requestCode, resultCode, data
        );


        if (
                requestCode == 10 && resultCode == RESULT_OK && data != null
        ) {
            imageURI = data.getData();

            if (imageURI != null) {
                rg_profileImg.setImageURI(
                        imageURI
                );
            }
        }
    }
}