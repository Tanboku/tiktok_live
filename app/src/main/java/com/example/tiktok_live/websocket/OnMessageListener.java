package com.example.tiktok_live.websocket;

public interface OnMessageListener {
    // 接收消息回调（主线程调用，可直接更新UI）
    void onMessageReceived(MessageEvent event);
}