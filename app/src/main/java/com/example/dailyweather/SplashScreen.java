package com.example.dailyweather;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.airbnb.lottie.LottieAnimationView;

public class SplashScreen extends AppCompatActivity {
    LottieAnimationView mlottie;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        mlottie = findViewById(R.id.splash_main);
        mlottie.setAnimation("splash.json");
        mlottie.playAnimation();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Intent mianIntent = new Intent(SplashScreen.this,MainActivity.class);
                startActivity(mianIntent);
                finish();
            }
        },3000);

    }
}
