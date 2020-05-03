package com.svalenciasan.safemap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = new Intent(WelcomeActivity.this, MapsActivity.class);
        setContentView(R.layout.activity_welcome);
        findViewById(R.id.heat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View unused) {
                intent.putExtra("type", "heat");
                startActivity(intent);
            }
        });
        findViewById(R.id.cluster).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View unused) {
                intent.putExtra("type", "cluster");
                startActivity(intent);
            }
        });
    }
}