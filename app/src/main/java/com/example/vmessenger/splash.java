package com.example.vmessenger;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class splash extends AppCompatActivity {
    ImageView logo;
    TextView name;
    TextView own1;
    TextView own2;
    Animation topAnim;
    Animation bottomAnim;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        logo = findViewById(R.id.logoimg);
        name = findViewById(R.id.logonameimg);
        own1 = findViewById(R.id.ownone);
        own2 = findViewById(R.id.owntwo);

        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);

        if (logo != null) {
            logo.setAnimation(topAnim);
        }

        if (name != null) {
            name.setAnimation(bottomAnim);
        }

        if (own1 != null) {
            own1.setAnimation(bottomAnim);
        }

        if (own2 != null) {
            own2.setAnimation(bottomAnim);
        }

        new Handler().postDelayed(new Runnable() {

                    @Override public void run() {
                        Intent intent = new Intent(splash.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                },
                2500
        );
    }
}