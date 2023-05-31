package kr.co.iksung.hce_example;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;

public class loading extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(loading.this, Fingerprint.class);
                startActivity(intent);
                finish();
            }
        }, 1000); // 1초(1000밀리초) 후에 전환
    }
}