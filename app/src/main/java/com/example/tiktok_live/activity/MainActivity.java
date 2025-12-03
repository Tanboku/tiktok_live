package com.example.tiktok_live.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tiktok_live.R;
import com.example.tiktok_live.model.URLContent;
import com.example.tiktok_live.websocket.WsManager;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity_Preload";
    private LinearLayout btnLiveRoom;
    private OkHttpClient okHttpClient;
    private WebSocket preloadedWebSocket; // 预加载的WebSocket连接
    private Request webSocketRequest;    // WebSocket请求对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btnLiveRoom = findViewById(R.id.live_button_text);


        setupNavigationListeners();
        // 添加预加载功能
        preloadLiveRoomResources();
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


    /**
     * 预加载直播间资源
     */
    private void preloadLiveRoomResources() {
        new Thread(() -> {
            try {
                // 预加载WebSocket连接
                preloadWebSocketConnection();

            } catch (Exception e) {
                Log.e(TAG, "预加载总异常", e);
            }
        }).start();
    }

    /**
     * 预加载WebSocket连接
     */
    private void preloadWebSocketConnection() {
        try {
            // 确保 WsManager 被初始化，从而初始化 okHttpClient
            WsManager wsManager = WsManager.getInstance();
            OkHttpClient okHttpClient = WsManager.getOkHttpClient();

            if (okHttpClient != null) {
                // 可以进行预加载准备工作
                Log.d("MainActivity_Preload", "WebSocket客户端初始化成功");
            } else {
                Log.e("MainActivity_Preload", "OkHttpClient实例为null");
            }
        } catch (Exception e) {
            Log.e("MainActivity_Preload", "WebSocket预加载异常", e);
        }
    }

}