package com.example.vmessenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class setting extends AppCompatActivity {
    ImageView setprofile;
    EditText setname;
    EditText setstatus;
    Button donebut;
    FirebaseAuth auth;
    FirebaseDatabase database;
    Uri setImageUri;
    String email = "";
    String password = "";
    String profilePicUrl = "";
    ProgressDialog progressDialog;
    private static final int IMAGE_REQUEST = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(setting.this, login.class);
            startActivity(intent);
            finish();
            return;
        }

        setprofile = findViewById(R.id.settingprofile);
        setname = findViewById(R.id.settingname);
        setstatus = findViewById(R.id.settingstatus);
        donebut = findViewById(R.id.donebutt);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving...");
        progressDialog.setCancelable(false);

        DatabaseReference reference = database.getReference().child("user").child(auth.getCurrentUser().getUid());

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            return;
                        }

                        if (snapshot.child("mail").getValue() != null) {
                            email = snapshot
                                    .child("mail")
                                    .getValue()
                                    .toString();
                        }

                        if (snapshot.child("password").getValue() != null) {
                            password = snapshot
                                    .child("password")
                                    .getValue()
                                    .toString();
                        }


                        if (snapshot.child("userName").getValue() != null) {
                            String name = snapshot
                                            .child("userName")
                                            .getValue()
                                            .toString();
                            setname.setText(name);
                        }

                        if (snapshot.child("profilepic").getValue() != null) {
                            profilePicUrl = snapshot
                                            .child("profilepic")
                                            .getValue()
                                            .toString();

                            if (!profilePicUrl.isEmpty()) {
                                Picasso.get()
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.photocamera)
                                        .error(R.drawable.photocamera)
                                        .into(setprofile);
                            }
                        }


                        if (snapshot.child("status").getValue() != null) {
                            String status = snapshot
                                            .child("status")
                                            .getValue()
                                            .toString();

                            setstatus.setText(status);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(
                                setting.this,
                                "Unable to load profile",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });

        setprofile.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT
            );

            startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), IMAGE_REQUEST);});

        donebut.setOnClickListener(v -> {

            String name = setname.getText().toString().trim();
            String status = setstatus.getText().toString().trim();

            if (name.isEmpty()) {
                setname.setError("Enter your name");
                return;
            }
            progressDialog.show();

            if (setImageUri != null) {
                CloudinaryUploader.uploadMedia(setting.this, setImageUri, new CloudinaryUploader.UploadCallback() {
                            @Override public void onSuccess(String downloadUrl) {
                                profilePicUrl = downloadUrl;
                                saveUserData(
                                        reference,
                                        name,
                                        status
                                );
                            }
                            @Override public void onFailure(String error) {
                                progressDialog.dismiss();
                                Toast.makeText(setting.this, "Image upload failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        }
                );
            } else {
                saveUserData(reference, name, status
                );
            }
        });
    }
    private void saveUserData(
            DatabaseReference reference,
            String name,
            String status) {

        String userId = auth.getCurrentUser().getUid();

        Users users = new Users(
                userId,
                name,
                email,
                password,
                profilePicUrl,
                status
        );

        reference.setValue(users).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override public void onComplete(@NonNull Task<Void> task) {
                                progressDialog.dismiss();
                                if (task.isSuccessful()) {
                                    Toast.makeText(
                                            setting.this,
                                            "Profile updated successfully",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    Intent intent  = new Intent(setting.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(
                                            setting.this,
                                            "Something went wrong",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                        }
                );
    }

    @Override protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data
        );

        if (requestCode == IMAGE_REQUEST &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null) {

            setImageUri = data.getData();
            setprofile.setImageURI(setImageUri);
        }
    }
}