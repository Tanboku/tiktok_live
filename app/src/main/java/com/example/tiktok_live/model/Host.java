package com.example.tiktok_live.model;

import com.google.gson.annotations.SerializedName;

public class Host {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("roomName")
    private String roomName;

    @SerializedName("followerNum")
    private int followerNum;

    @SerializedName("createdAt")
    private String createdAt;

    // Getter方法
    public String getId() { return id; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public String getRoomName() { return roomName; }
    public int getFollowerNum() { return followerNum; }
    public String getCreatedAt() { return createdAt; }
}