package com.example.tiktok_live.websocket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WsManager {
    private static final String TAG = "WsManager";
    private static final String WS_URL = "wss://echo.websocket.org/"; // 公共回声测试服务器（发送啥返回啥）
    private static volatile WsManager instance; // 单例（双重校验锁）
    private static OkHttpClient okHttpClient;
    private WebSocket webSocket;
    private final List<OnMessageListener> listeners = new CopyOnWriteArrayList<>(); // 线程安全的观察者列表
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程切换

    private volatile boolean isConnected = false;
    // 私有构造函数（禁止外部实例化）
    private WsManager() {
        initOkHttpClient();
    }

    // 单例初始化
    public static WsManager getInstance() {
        if (instance == null) {
            synchronized (WsManager.class) {
                if (instance == null) {
                    instance = new WsManager();
                }
            }
        }
        return instance;
    }

    // 初始化OKHttpClient（设置PING间隔10秒）
    private void initOkHttpClient() {
        okHttpClient = new OkHttpClient.Builder()
                .pingInterval(10, TimeUnit.SECONDS) // 保持连接的PING帧
                .build();
    }

    // 建立WebSocket连接
    public void connect() {
        if (webSocket != null) {
            webSocket.cancel();
        }

        Request request = new Request.Builder()
                .url(WS_URL)
                .build();

        // 建立连接并设置回调
        okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
                WsManager.this.webSocket = webSocket;
                isConnected = true; // 连接已建立
                Log.i(TAG, "连接成功");
            }

            // 接收文本消息
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                Log.i(TAG, "收到文本消息：" + text);
                notifyListeners(new MessageEvent(text)); // 通知所有观察者
            }

            // 接收二进制消息
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                super.onMessage(webSocket, bytes);
                String text = bytes.utf8();
                Log.i(TAG, "收到二进制消息：" + text);
                notifyListeners(new MessageEvent(text)); // 通知所有观察者
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                Log.i(TAG, "连接关闭中：code=" + code + ", reason=" + reason);
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                Log.i(TAG, "连接已关闭：code=" + code + ", reason=" + reason);
                isConnected = false; // 连接已关闭
                WsManager.this.webSocket = null;
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                isConnected = false; // 连接失败
                Log.e(TAG, "连接失败：" + t.getMessage(), t);
                WsManager.this.webSocket = null;
                // 简单重连（可选，原教程下一篇讲重连，这里简化）
                mainHandler.postDelayed(() -> connect(), 3000);
            }
        });
    }

    // 发送文本消息
    public boolean sendText(String text) {
        if (webSocket != null && webSocket.send(text)) {
            Log.i(TAG, "发送文本消息：" + text);
            return true;
        }
        Log.e(TAG, "发送失败：连接未建立");
        return false;
    }

    // 发送二进制消息
    public boolean sendBinary(String text) {
        ByteString byteString = ByteString.encodeUtf8(text);
        if (webSocket != null && webSocket.send(byteString)) {
            Log.i(TAG, "发送二进制消息：" + text);
            return true;
        }
        Log.e(TAG, "发送失败：连接未建立");
        return false;
    }

    // 关闭连接
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "主动关闭");
            webSocket = null;
        }
    }

    // 注册观察者
    public void addOnMessageListener(OnMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // 移除观察者
    public void removeOnMessageListener(OnMessageListener listener) {
        listeners.remove(listener);
    }

    // 通知所有观察者（切换到主线程）
    private void notifyListeners(MessageEvent event) {
        mainHandler.post(() -> {
            for (OnMessageListener listener : listeners) {
                listener.onMessageReceived(event);
            }
        });
    }

    public static OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public boolean isConnected() {
        return isConnected;
    }
}