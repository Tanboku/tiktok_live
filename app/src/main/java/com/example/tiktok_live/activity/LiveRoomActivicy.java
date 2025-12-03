package com.example.tiktok_live.activity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bytedance.rheatrace.RheaTrace3;
import com.example.tiktok_live.R;
import com.example.tiktok_live.adapter.CommentAdapter;
import com.example.tiktok_live.asynctask.LoadDataAsyncTask;
import com.example.tiktok_live.asynctask.SubmitCommentAsyncTask;
import com.example.tiktok_live.model.Comment;
import com.example.tiktok_live.model.URLContent;
import com.example.tiktok_live.plugin.LikePlugin;
import com.example.tiktok_live.viewmodel.LiveRoomViewModel;
import com.example.tiktok_live.websocket.MessageEvent;
import com.example.tiktok_live.websocket.OnMessageListener;
import com.example.tiktok_live.websocket.WsManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class LiveRoomActivicy extends AppCompatActivity implements LoadDataAsyncTask.OnGetNetDataListener,
        SubmitCommentAsyncTask.OnSubmitCommentListener {

    private VideoView videoView;
    private RecyclerView rvChat;
    private EditText etComment;
    private Button btnSend;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>(); // 评论数据源
    // Host相关控件
    private ImageView ivHostAvatar;
    private TextView tvHostName;
    private TextView tvHostRoom;
    private TextView tvOnlionineCount;

    // Comment列表相关控件
    private RecyclerView rvComments;

    // 按钮
    private Button btnRefreshAll;
    private ImageView btnreturn;

    // 新增：评论输入控件
    private EditText etCommentInput;
    private Button btnSubmitComment;

    // 异步任务（两个API各一个，或共用一个，这里用两个更清晰）
    private LoadDataAsyncTask hostTask;
    private LoadDataAsyncTask commentsTask;
    private SubmitCommentAsyncTask submitCommentTask;
    private WebView webView;

    private PlayerView playerView;
    private ExoPlayer player;
    private static final String VIDEO_URL = "https://livesim2.dashif.org/livesim2/chunkdur_1/ato_7/testpic4_8s/Manifest300.mpd";
    private LiveRoomViewModel viewModel;
    // 添加 LikePlugin 成员变量
    private LikePlugin likePlugin;
    private ImageView btnLike;
    private TextView tvLikeCount;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 当 enable_btrace=false 时集成的是 rhea-inhouse-noop，此时的 init() 方法里没有任何逻辑
        RheaTrace3.init(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_room);

        // 初始化 ViewModel
        viewModel = new ViewModelProvider(this).get(LiveRoomViewModel.class);
        // 初始化 LikePlugin
        likePlugin = new LikePlugin();
        likePlugin.onInit(getApplication());

        // 初始化控件
        initView();
        // 注册观察者
        observeData();
        // 同时加载两个API数据
        loadAllData();
        // 注册WebSocket观察者
//        WsManager.getInstance().addOnMessageListener(this);
        // 建立WebSocket连接
//        WsManager.getInstance().connect();
        // 建立WebSocket连接（通过ViewModel）
        viewModel.connectWebSocket();
        // 1. 初始化视频背景（之前的代码，保持不变）
        initVideoBackground();
        setupNavigationListeners();
    }

    // 添加观察者方法
    private void observeData() {
        // 观察评论列表
        viewModel.getCommentList().observe(this, comments -> {
            if (comments != null) {
                commentAdapter.updateData(comments);
                rvComments.scrollToPosition(comments.size() - 1);
            }
        });

        // 观察主播信息
        viewModel.getHostInfo().observe(this, host -> {
            if (host != null) {
                showHostData(host);
            }
        });

        // 观察错误信息
        viewModel.getErrorMsg().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // 观察在线人数
        viewModel.getOnlineCount().observe(this, count -> {
            if (count != null) {
                tvOnlionineCount.setText(String.valueOf(count));
            }
        });

        // 观察提交状态
        viewModel.getIsSubmitting().observe(this, isSubmitting -> {
            if (isSubmitting != null) {
                btnSubmitComment.setEnabled(!isSubmitting);
                btnSubmitComment.setText(isSubmitting ? "提交中..." : "提交评论");
            }
        });

        // 观察点赞数变化
        if (likePlugin != null) {
            likePlugin.getTotalLikes().observe(this, likes -> {
                if (likes != null) {
                    tvLikeCount.setText(String.valueOf(likes));
                }
            });
        }
    }

    private void initView() {
        // 1. 初始化Host控件
        ivHostAvatar = findViewById(R.id.iv_host_avatar);
        tvHostName = findViewById(R.id.tv_host_name);
//        tvHostRoom = findViewById(R.id.tv_host_room);
        tvOnlionineCount = findViewById(R.id.tv_online_num);

        // 2. 初始化Comment列表
        rvComments = findViewById(R.id.rv_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, new ArrayList<>());
        rvComments.setAdapter(commentAdapter);

        btnreturn = findViewById(R.id.iv_close);

        //发送文本消息
        WsManager.getInstance().sendText("1");

        // 3. 初始化刷新按钮
//        btnRefreshAll = findViewById(R.id.btn_refresh_all);
//        btnRefreshAll.setOnClickListener(v -> loadAllData());

        // 4. 新增：初始化评论输入控件
        etCommentInput = findViewById(R.id.et_comment_input);
        btnSubmitComment = findViewById(R.id.btn_submit_comment);

        // 5. 新增：提交评论按钮点击事件
        btnSubmitComment.setOnClickListener(v -> submitComment());

        playerView = findViewById(R.id.player_view);

        // 初始化点赞相关控件
        btnLike = findViewById(R.id.btn_like);
        tvLikeCount = findViewById(R.id.tv_like_count);

        // 设置点赞按钮点击事件
        btnLike.setOnClickListener(v -> {
            if (likePlugin.like()) {
                // 点赞成功，可以添加动画效果
            }
        });

    }

    // 接收WebSocket消息（主线程回调，可直接更新UI）
//    @Override
//    public void onMessageReceived(MessageEvent event) {
//        String newMsg = event.getMessage();
//        if (newMsg != null) {
//            // 从UI控件中获取当前显示的人数
//            String currentText = tvOnlionineCount.getText().toString();
//            try {
//                int currentCount = Integer.parseInt(currentText);
//                int newCount = currentCount + 1;
//                viewModel.setOnlineCount(newCount);
//            } catch (NumberFormatException e) {
//                // 如果解析失败，默认从12开始
//                viewModel.setOnlineCount(13);
//            }
//        }
////            tvOnlionineCount.setText((Integer.parseInt(tvOnlionineCount.getText().toString()) + 1) + "");
//    }

    private void setupNavigationListeners() {
        btnreturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 首页点击事件
                Intent intent = new Intent(LiveRoomActivicy.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * 同时加载两个API数据
     */
    private void loadAllData() {
        // 取消之前的任务（避免重复请求）
        if (hostTask != null) hostTask.cancelTask();
        if (commentsTask != null) commentsTask.cancelTask();

        // 1. 加载Host API（传入标识TAG_HOST + URL）
//        hostTask = new LoadDataAsyncTask(this, this, true);
//        hostTask.execute(URLContent.TAG_HOST, URLContent.getHostInfoURL());

        // 2. 加载Comments API（传入标识TAG_COMMENTS + URL）
//        commentsTask = new LoadDataAsyncTask(this, this, false); // 不重复显示加载框
//        commentsTask.execute(URLContent.TAG_COMMENTS, URLContent.getCommentsURL());
    }


    // 新增：提交评论逻辑（优化重复提交）
    private void submitComment() {
        // 1. 获取输入内容
        String commentContent = etCommentInput.getText().toString().trim();

        // 2. 非空验证
        if (commentContent.isEmpty()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. 防止重复提交：置灰按钮
        btnSubmitComment.setEnabled(false);
        btnSubmitComment.setText("提交中...");

        // 4. 取消之前的提交任务
        if (submitCommentTask != null && !submitCommentTask.isCancelled()) {
            submitCommentTask.cancel(true);
        }

        viewModel.submitComment(commentContent);
        etCommentInput.setText("");

//        // 5. 执行提交任务
//        submitCommentTask = new SubmitCommentAsyncTask(this, new SubmitCommentAsyncTask.OnSubmitCommentListener() {
//            @Override
//            public void onSuccess(Comment newComment) {
//                // 恢复按钮状态
//                btnSubmitComment.setEnabled(true);
//                btnSubmitComment.setText("提交评论");
//
//                // 原有逻辑
//                commentAdapter.addComment(newComment);
//                rvComments.scrollToPosition(commentAdapter.getItemCount() - 1);
//                etCommentInput.setText("");
//                Toast.makeText(LiveRoomActivicy.this, "评论提交成功！", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onFailure(String errorMsg) {
//                // 恢复按钮状态
//                btnSubmitComment.setEnabled(true);
//                btnSubmitComment.setText("提交评论");
//
//                Toast.makeText(LiveRoomActivicy.this, errorMsg, Toast.LENGTH_SHORT).show();
//            }
//        });
//        submitCommentTask.execute(commentContent);
    }


    /**
     * 回调：根据API标识区分解析逻辑
     */
    @Override
    public void onSuccess(String apiTag, String json) {
        try {
            Gson gson = new Gson();
            // 根据标识判断解析哪种数据
            if (URLContent.TAG_HOST.equals(apiTag)) {
                // 解析Host单个对象
//                com.example.tiktok_live.model.Host host = gson.fromJson(json, com.example.tiktok_live.model.Host.class);
//                viewModel.setHostInfo(host); // 通过ViewModel更新数据
            } else if (URLContent.TAG_COMMENTS.equals(apiTag)) {
//                // 解析Comment数组
//                List<Comment> commentList = gson.fromJson(
//                        json,
//                        new TypeToken<List<Comment>>() {}.getType()
//                );
//                viewModel.setCommentList(commentList); // 通过ViewModel更新数据
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, apiTag + "解析失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 展示Host数据（头像+姓名+房间名）
     */
    private void showHostData(com.example.tiktok_live.model.Host host) {
        if (host == null) return;
        // 加载头像
        Glide.with(this)
                .load(host.getAvatar())
                .circleCrop()
                .into(ivHostAvatar);
        // 展示文本信息（添加空值检查）
        if (tvHostName != null) {
            tvHostName.setText("姓名：" + host.getName());
        }

        if (tvHostRoom != null) {
            tvHostRoom.setText("房间名：" + host.getRoomName());
        }
    }

    /**
     * 展示Comment列表数据
     */
    private void showCommentData(List<Comment> commentList) {
        if (commentList != null && !commentList.isEmpty()) {
            commentAdapter.updateData(commentList);
            Toast.makeText(this, "评论加载成功：" + commentList.size() + "条", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "未获取到评论数据", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onFailure(String errorMsg) {
        viewModel.setErrorMsg(errorMsg); // 通过ViewModel更新错误信息
    }


    // 新增：提交评论成功回调（实现OnSubmitCommentListener）
    @Override
    public void onSuccess(Comment newComment) {
        // 1. 追加评论到列表
        commentAdapter.addComment(newComment);
        // 2. 滚动到最后一条（最新评论）
        rvComments.scrollToPosition(commentAdapter.getItemCount() - 1);
        // 3. 清空输入框
        etCommentInput.setText("");
        // 4. 提示成功
        Toast.makeText(this, "评论提交成功！", Toast.LENGTH_SHORT).show();
    }

    private void initVideoBackground() {
        // 初始化 ExoPlayer（Media3 标准 Builder 模式）
        player = new ExoPlayer.Builder(this)
                .setHandleAudioBecomingNoisy(true) // 音频中断时暂停（如来电）
                .build();

        playerView.setPlayer(player);

        // 设置播放源
        MediaItem mediaItem = MediaItem.fromUri(VIDEO_URL);
        player.setMediaItem(mediaItem);

        // 预加载
        player.prepare();

        // 监听播放状态变化（修正异常回调）
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        Toast.makeText(LiveRoomActivicy.this, "缓冲中...", Toast.LENGTH_SHORT).show();
                        break;
                    case Player.STATE_READY:
                        player.play(); // 自动播放
                        break;
                    case Player.STATE_ENDED:
                        Toast.makeText(LiveRoomActivicy.this, "播放完成", Toast.LENGTH_SHORT).show();
                        player.seekTo(0); // 回到开头
                        break;
                }
            }

            // 关键修正：替换 PlayerException 为 PlaybackException，且回调方法签名简化
            @Override
            public void onPlayerError(PlaybackException error) {
                // 可通过 error.getErrorCode() 获取具体错误类型（网络错误/格式不支持等）
                String errorMsg = "播放失败：" + error.getMessage();
                Toast.makeText(LiveRoomActivicy.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });

//        // 绑定WebView
//        webView = findViewById(R.id.webview);
//        WebSettings webSettings = webView.getSettings();
//
//        // --------------- 核心配置：确保无播放限制 ---------------
//        // 1. 允许JavaScript执行（必须，播放器基于JS开发）
//        webSettings.setJavaScriptEnabled(true);
//
//        // 2. 允许访问本地文件（加载assets中的脚本）
//        webSettings.setAllowFileAccess(true);
//        webSettings.setAllowFileAccessFromFileURLs(true);
//        webSettings.setAllowUniversalAccessFromFileURLs(true);
//
//        // 3. 允许跨域网络请求（访问DASH直播源）
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            webSettings.setAllowFileAccessFromFileURLs(true);
//        }
//
//        // 4. 启用硬件加速（提升视频渲染性能，避免卡顿）
//        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
//
//        // 5. 启用DOM存储（播放器依赖本地存储）
//        webSettings.setDomStorageEnabled(true);
//        webSettings.setDatabaseEnabled(true);
//
//        // 6. 禁用缓存（避免旧脚本/配置干扰）
//        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
//
//        // 7. 允许JS打开窗口（插件可能依赖）
//        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
//
//        // 8. 调试模式（可选，Chrome可 inspect 调试）
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            webView.setWebContentsDebuggingEnabled(true);
//        }
//
//        // --------------- 加载前端页面 ---------------
//        webView.loadUrl("file:///android_asset/player.html"); // 路径必须与assets目录对应

//        videoView = findViewById(R.id.vv_live_background);
//        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.live_party_ing);
//        videoView.setVideoURI(uri);
//
//        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer mediaPlayer) {
//                // 1. 基础配置：循环+静音
//                mediaPlayer.setLooping(true);
////                mediaPlayer.setVolume(0, 0);
//                // 2. 获取父布局（ConstraintLayout），等待其完成测量后再调整视频尺寸
//                View parentLayout = (View) videoView.getParent(); // 父布局是ConstraintLayout
//                parentLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                    @Override
//                    public void onGlobalLayout() {
//                        // 监听一次后移除，避免重复触发
//                        parentLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//
//                        // 3. 获取父布局的实际宽高（此时已完成测量）
//                        int parentWidth = parentLayout.getWidth();
//                        int parentHeight = parentLayout.getHeight();
//                        if (parentWidth == 0 || parentHeight == 0) {
//                            return; // 异常情况直接返回
//                        }
//
//                        // 4. 计算视频的宽高比
//                        int videoWidth = mediaPlayer.getVideoWidth();
//                        int videoHeight = mediaPlayer.getVideoHeight();
//                        float videoRatio = (float) videoWidth / videoHeight;
//                        float parentRatio = (float) parentWidth / parentHeight;
//
//                        // 5. 创建 ConstraintLayout 的布局参数（父布局是ConstraintLayout，必须用对应LayoutParams）
//                        ConstraintLayout.LayoutParams videoParams = new ConstraintLayout.LayoutParams(
//                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
//                                ConstraintLayout.LayoutParams.WRAP_CONTENT
//                        );
//
//                        // 6. 根据宽高比调整视频尺寸（保持视频比例，避免拉伸）
//                        if (videoRatio > parentRatio) {
//                            // 视频更宽 → 宽度匹配父布局，高度按比例缩放
//                            videoParams.width = parentWidth;
//                            videoParams.height = (int) (parentWidth / videoRatio);
//                        } else {
//                            // 视频更高 → 高度匹配父布局，宽度按比例缩放
//                            videoParams.height = parentHeight;
//                            videoParams.width = (int) (parentHeight * videoRatio);
//                        }
//
//                        // 7. 关键：设置视频在父布局中【水平+垂直居中】
//                        videoParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID; // 左对齐父布局
//                        videoParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;     // 右对齐父布局
//                        videoParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;     // 上对齐父布局
//                        videoParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID; // 下对齐父布局
//
//                        // 8. 应用布局参数并启动播放
//                        videoView.setLayoutParams(videoParams);
//                        videoView.start();
//                    }
//                });
//            }
//        });
    }

    // ---------------------- 公屏聊天功能初始化（核心新增代码） ----------------------
    private void initChatFunction() {
        // 绑定控件
        rvChat = findViewById(R.id.rv_comments);
        etComment = findViewById(R.id.layout_comment);
        btnSend = findViewById(R.id.btn_submit_comment);

        // 1. 初始化 RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL); // 垂直方向滚动
        rvChat.setLayoutManager(layoutManager);

        // 2. 初始化适配器（可添加测试评论）
        commentAdapter = new CommentAdapter(this,commentList);
        rvChat.setAdapter(commentAdapter);

        // 3. 发送按钮点击事件
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendComment();
            }
        });

        // 可选：输入框回车发送（增强用户体验）
        etComment.setOnEditorActionListener((v, actionId, event) -> {
            sendComment();
            return true;
        });
    }

    // ---------------------- 发送评论逻辑 ----------------------
    private void sendComment() {
        // 1. 获取输入的评论内容（去空格）
        String commentContent = etComment.getText().toString().trim();
        if (commentContent.isEmpty()) {
            etComment.setHint("请输入评论内容～");
            return;
        }

        // 2. 创建评论对象（自动填充用户名和时间）
        Comment newComment = new Comment(commentContent);

        // 3. 添加到数据源并刷新适配器
        commentAdapter.addComment(newComment);

        // 4. 滚动到公屏底部（显示最新评论）
        rvChat.scrollToPosition(commentAdapter.getItemCount() - 1);

        // 5. 清空输入框
        etComment.setText("");
    }

    // 生命周期管理（不变，确保资源释放）
    @Override
    protected void onStart() {
        super.onStart();
        if (likePlugin != null) {
            likePlugin.onActivate(this);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (likePlugin != null) {
            likePlugin.onDeactivate(this);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
//        if (videoView.isPlaying()) {
//            videoView.pause();
//        }
//        if (webView != null) {
//            webView.onPause(); // 暂停播放
//        }
        if (playerView != null) playerView.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (!videoView.isPlaying()) {
//            videoView.start();
//        }
//        if (webView != null) {
//            webView.onResume(); // 恢复播放
//        }
        if (playerView != null && !player.isPlaying()) {
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消所有任务，避免内存泄漏
        if (hostTask != null) hostTask.cancelTask();
        if (commentsTask != null) commentsTask.cancelTask();
//        if (webView != null) {
//            webView.destroy(); // 销毁WebView，释放资源
//        }
        if (player != null) {
            player.release(); // 必须释放，避免内存泄漏
            player = null;
        }
        playerView = null;
        // 移除观察者（避免内存泄漏）
//        WsManager.getInstance().removeOnMessageListener(this);
        // 关闭WebSocket连接
        WsManager.getInstance().disconnect();
    }

    // 自定义控制方法（不变）
    public void playVideo() {
        if (player != null && !player.isPlaying()) player.play();
    }

    public void pauseVideo() {
        if (player != null && player.isPlaying()) player.pause();
    }

    public void switchVideoSource(String newVideoUrl) {
        if (player != null) {
            MediaItem newMediaItem = MediaItem.fromUri(newVideoUrl);
            player.setMediaItem(newMediaItem);
            player.prepare();
            player.play();
        }
    }

    public void seekVideo(long milliseconds) {
        if (player != null) {
            long newPosition = player.getCurrentPosition() + milliseconds;
            player.seekTo(Math.max(0, Math.min(newPosition, player.getDuration())));
        }
    }
}
