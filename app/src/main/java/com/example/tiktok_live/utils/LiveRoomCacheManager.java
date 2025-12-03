package com.example.tiktok_live.utils;

import com.example.tiktok_live.model.Comment;
import com.example.tiktok_live.model.Host;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 直播间状态缓存管理器（30秒TTL）
 */
public class LiveRoomCacheManager {
    private static final String TAG = "LiveRoomCacheManager";
    private static final long CACHE_TTL_MS = 30 * 1000; // 30秒缓存有效期
    private static volatile LiveRoomCacheManager instance;

    // 缓存状态（轻量级数据，不持有Activity/Context引用）
    public static class LiveRoomCache {
        // 播放器状态
        String videoUrl; // 播放地址
        long playPosition; // 播放进度（毫秒）
        boolean isPlaying; // 退出时是否在播放

        // 页面数据
        Host hostInfo; // 主播信息
        List<Comment> commentList; // 评论列表
        int onlineCount; // 在线人数
        int likeCount; // 点赞数
        String commentInputContent; // 评论输入框内容

        // 缓存时间戳（用于判断是否过期）
        long cacheTimestamp;

        public LiveRoomCache(String videoUrl, long playPosition, boolean isPlaying,
                            Host hostInfo, List<Comment> commentList, int onlineCount,
                            int likeCount, String commentInputContent) {
            this.videoUrl = videoUrl;
            this.playPosition = playPosition;
            this.isPlaying = isPlaying;
            this.hostInfo = hostInfo;
            this.commentList = new ArrayList<>(commentList); // 深拷贝避免外部修改
            this.onlineCount = onlineCount;
            this.likeCount = likeCount;
            this.commentInputContent = commentInputContent;
            this.cacheTimestamp = System.currentTimeMillis();
        }

        // 判断缓存是否过期
        public boolean isExpired() {
            return System.currentTimeMillis() - cacheTimestamp > CACHE_TTL_MS;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public long getPlayPosition() {
            return playPosition;
        }

        public boolean isPlaying() {
            return isPlaying;
        }

        public Host getHostInfo() {
            return hostInfo;
        }

        public List<Comment> getCommentList() {
            return commentList;
        }

        public int getOnlineCount() {
            return onlineCount;
        }

        public int getLikeCount() {
            return likeCount;
        }

        public String getCommentInputContent() {
            return commentInputContent;
        }

        public long getCacheTimestamp() {
            return cacheTimestamp;
        }
    }

    private LiveRoomCache cache;
    private final AtomicBoolean isCacheValid = new AtomicBoolean(false);

    private LiveRoomCacheManager() {}

    // 双重校验锁单例
    public static LiveRoomCacheManager getInstance() {
        if (instance == null) {
            synchronized (LiveRoomCacheManager.class) {
                if (instance == null) {
                    instance = new LiveRoomCacheManager();
                }
            }
        }
        return instance;
    }

    /**
     * 存入缓存
     */
    public void putCache(String videoUrl, long playPosition, boolean isPlaying,
                         Host hostInfo, List<Comment> commentList, int onlineCount,
                         int likeCount, String commentInputContent) {
        this.cache = new LiveRoomCache(
                videoUrl, playPosition, isPlaying,
                hostInfo, commentList, onlineCount,
                likeCount, commentInputContent
        );
        isCacheValid.set(true);
    }

    /**
     * 获取有效缓存（过期则自动清理）
     */
    public LiveRoomCache getValidCache() {
        if (!isCacheValid.get() || cache == null) {
            return null;
        }
        if (cache.isExpired()) {
            clearCache();
            return null;
        }
        return cache;
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        cache = null;
        isCacheValid.set(false);
    }
}