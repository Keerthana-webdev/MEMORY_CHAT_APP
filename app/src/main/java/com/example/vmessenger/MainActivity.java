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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    FirebaseAuth auth;
    FirebaseDatabase database;
    RecyclerView mainUserRecyclerView;
    UserAdpter adapter;
    ArrayList<Users> usersArrayList;
    ImageView imglogout;
    ImageView camBut;
    ImageView chatBut;
    ImageView settingBut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Firebase
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

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
        usersArrayList = new ArrayList<>();
        mainUserRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        adapter = new UserAdpter(MainActivity.this, usersArrayList);
        mainUserRecyclerView.setAdapter(adapter);

        DatabaseReference reference =
                database.getReference().child("user");

        String currentUserId = auth.getCurrentUser().getUid();

        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                usersArrayList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    Users users =
                            dataSnapshot.getValue(Users.class);

                    if (users == null) {
                        continue;
                    }

                    // DO NOT SHOW CURRENT LOGGED-IN USER
                    if (users.getUserId() != null &&
                            users.getUserId().equals(currentUserId)) {

                        continue;
                    }

                    usersArrayList.add(users);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {

                Toast.makeText(
                        MainActivity.this,
                        "Unable to load users",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // -------------------------------------------------
        // LOGOUT
        // -------------------------------------------------

        imglogout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Dialog dialog = new Dialog(
                        MainActivity.this,
                        R.style.dialoge
                );

                dialog.setContentView(R.layout.dialog_layout);

                Button no = dialog.findViewById(R.id.nobnt);
                Button yes = dialog.findViewById(R.id.yesbnt);

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

        // -------------------------------------------------
        // SETTINGS BUTTON
        // -------------------------------------------------

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

        // -------------------------------------------------
        // CAMERA BUTTON
        // -------------------------------------------------

        camBut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                try {

                    Intent cameraIntent =
                            new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    startActivity(cameraIntent);

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "Camera is not available",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        });

        // -------------------------------------------------
        // CHAT BUTTON
        // -------------------------------------------------

        chatBut.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Toast.makeText(
                        MainActivity.this,
                        "Select a user above to start chatting",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}

