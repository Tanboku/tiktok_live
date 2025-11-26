package com.example.tiktok_live.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tiktok_live.R;

public class MainActivity extends AppCompatActivity {

    private LinearLayout btnLiveRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnLiveRoom = findViewById(R.id.live_button_text);

        setupNavigationListeners();
    }

    private void setupNavigationListeners() {
        btnLiveRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 首页点击事件
                Intent intent = new Intent(MainActivity.this, LiveRoomActivicy.class);
                startActivity(intent);
            }
        });
    }
}