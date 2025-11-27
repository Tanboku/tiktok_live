package com.example.tiktok_live.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// 评论数据模型
public class Comment {
    private String userName; // 用户名（可自定义，这里用固定名称演示）
    private String content;  // 评论内容
    private String time;     // 发送时间

    // 构造方法（传入评论内容，自动填充用户名和时间）
    public Comment(String content) {
        this.userName = "观众" + (int) (Math.random() * 1000); // 随机生成用户名（示例）
        this.content = content;
        // 格式化当前时间（如：14:35:22）
        this.time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    // Getter 方法（Adapter 需要获取数据）
    public String getUserName() {
        return userName;
    }

    public String getContent() {
        return content;
    }

    public String getTime() {
        return time;
    }
}