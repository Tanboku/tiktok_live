package com.example.tiktok_live.model;

public class URLContent {
    // API标识常量（用于区分不同API）
    public static final String TAG_HOST = "HOST_API";
    public static final String TAG_COMMENTS = "COMMENTS_API";
    private static final String WS_URL = "wss://echo.websocket.org/";

    // 单个Host API（返回对象{}）
    public static String getHostInfoURL() {
        return "https://691ec8ffbb52a1db22bf1066.mockapi.io/api/v1/hosts/5";
    }

    // 评论列表API（返回数组[]）
    public static String getCommentsURL() {
        return "https://691ec8ffbb52a1db22bf1066.mockapi.io/api/v1/comments_1";
    }
    public static String getWebSocketURL() {
        return WS_URL;
    }
}