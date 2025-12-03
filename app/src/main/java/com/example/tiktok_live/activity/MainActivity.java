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
        WsManager.getInstance();


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
            String webSocketUrl = URLContent.getWebSocketURL();
            if (TextUtils.isEmpty(webSocketUrl)) {
                Log.w(TAG, "WebSocket地址为空");
                return;
            }

            // 创建WebSocket请求（支持Token认证可添加header）
            webSocketRequest = new Request.Builder()
                    .url(webSocketUrl)
                    .addHeader("Authorization", "Bearer " )
                    .build();

            // 建立WebSocket连接（回调在子线程，需切换UI线程更新状态）
            preloadedWebSocket = okHttpClient.newWebSocket(webSocketRequest, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    super.onOpen(webSocket, response);
                    Log.d(TAG, "WebSocket预连接成功");
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    super.onMessage(webSocket, text);
                    // 预加载期间收到的实时消息（如新增评论）可缓存到CacheManager
                    Log.d(TAG, "WebSocket预加载期间收到消息：" + text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    super.onFailure(webSocket, t, response);
                    Log.e(TAG, "WebSocket预连接失败", t);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "WebSocket预加载异常", e);
        }
    }

}