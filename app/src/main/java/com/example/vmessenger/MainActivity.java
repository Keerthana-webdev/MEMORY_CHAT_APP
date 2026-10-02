package com.example.vmessenger;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    FirebaseAuth auth;
    RecyclerView mainUserRecyclerView;
    UserAdpter adapter;
    FirebaseDatabase database;
    ArrayList<Users> usersArrayList;
    ImageView imglogout;
    ImageView camBut;
    ImageView chatBut;
    ImageView settingBut;

    private static final int CAMERA_REQUEST = 100;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, login.class);
            startActivity(intent);
            finish();
            return;
        }

        imglogout = findViewById(R.id.logoutimg);
        camBut = findViewById(R.id.camBut);
        chatBut = findViewById(R.id.chatBut);
        settingBut = findViewById(R.id.settingBut);
        mainUserRecyclerView = findViewById(R.id.mainUserRecyclerView);
        mainUserRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        usersArrayList = new ArrayList<>();
        adapter = new UserAdpter(MainActivity.this, usersArrayList
        );

        mainUserRecyclerView.setAdapter(adapter);

        DatabaseReference reference = database.getReference().child("user");
        reference.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Users users = dataSnapshot.getValue(Users.class);

                    if (users != null) {
                        if (users.getUserId() == null || users.getUserId().isEmpty()) {
                            users.setUserId(dataSnapshot.getKey());
                        }

                        if (auth.getCurrentUser() != null &&
                                users.getUserId() != null &&
                                !users.getUserId().equals(auth.getCurrentUser().getUid())) {
                            usersArrayList.add(users);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(
                        MainActivity.this,
                        "Failed to load users",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // ---------------------------------------------------
        // CAMERA BUTTON
        // ---------------------------------------------------

        camBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {

                    Intent cameraIntent =
                            new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    if (cameraIntent.resolveActivity(
                            getPackageManager()) != null) {

                        startActivityForResult(
                                cameraIntent,
                                CAMERA_REQUEST
                        );

                    } else {

                        Toast.makeText(
                                MainActivity.this,
                                "Camera is not available",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "Unable to open camera",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });

        // ---------------------------------------------------
        // CHAT BUTTON
        // ---------------------------------------------------

        chatBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(
                        MainActivity.this,
                        SelectUserActivity.class
                );

                startActivity(intent);
            }
        });

        // ---------------------------------------------------
        // SETTINGS BUTTON
        // ---------------------------------------------------

        settingBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(
                        MainActivity.this,
                        setting.class
                );

                startActivity(intent);
            }
        });

        // ---------------------------------------------------
        // LOGOUT BUTTON
        // ---------------------------------------------------

        imglogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Dialog dialog = new Dialog(
                        MainActivity.this,
                        R.style.dialoge
                );

                dialog.setContentView(R.layout.dialog_layout);

                Button no;
                Button yes;

                yes = dialog.findViewById(R.id.yesbnt);
                no = dialog.findViewById(R.id.nobnt);

                yes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        FirebaseAuth.getInstance().signOut();

                        dialog.dismiss();

                        Intent intent = new Intent(
                                MainActivity.this,
                                login.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    }
                });

                no.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == CAMERA_REQUEST) {

            if (resultCode == RESULT_OK) {

                Toast.makeText(
                        MainActivity.this,
                        "Photo captured",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}