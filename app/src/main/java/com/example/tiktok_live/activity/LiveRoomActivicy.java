package com.example.tiktok_live.activity;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktok_live.R;
import com.example.tiktok_live.adapter.CommentAdapter;
import com.example.tiktok_live.asynctask.LoadDataAsyncTask;
import com.example.tiktok_live.asynctask.SubmitCommentAsyncTask;
import com.example.tiktok_live.model.Comment;
import com.example.tiktok_live.model.URLContent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class LiveRoomActivicy extends AppCompatActivity implements LoadDataAsyncTask.OnGetNetDataListener,
        SubmitCommentAsyncTask.OnSubmitCommentListener{

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

    // Comment列表相关控件
    private RecyclerView rvComments;

    // 按钮
    private Button btnRefreshAll;

    // 新增：评论输入控件
    private EditText etCommentInput;
    private Button btnSubmitComment;

    // 异步任务（两个API各一个，或共用一个，这里用两个更清晰）
    private LoadDataAsyncTask hostTask;
    private LoadDataAsyncTask commentsTask;
    private SubmitCommentAsyncTask submitCommentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_room);
        // 1. 初始化视频背景（之前的代码，保持不变）
        initVideoBackground();
        // 初始化控件
        initView();
        // 同时加载两个API数据
        loadAllData();
    }


    private void initView() {
        // 1. 初始化Host控件
        ivHostAvatar = findViewById(R.id.iv_host_avatar);
        tvHostName = findViewById(R.id.tv_host_name);
//        tvHostRoom = findViewById(R.id.tv_host_room);

        // 2. 初始化Comment列表
        rvComments = findViewById(R.id.rv_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, new ArrayList<>());
        rvComments.setAdapter(commentAdapter);

        // 3. 初始化刷新按钮
//        btnRefreshAll = findViewById(R.id.btn_refresh_all);
//        btnRefreshAll.setOnClickListener(v -> loadAllData());

        // 4. 新增：初始化评论输入控件
        etCommentInput = findViewById(R.id.et_comment_input);
        btnSubmitComment = findViewById(R.id.btn_submit_comment);

        // 5. 新增：提交评论按钮点击事件
        btnSubmitComment.setOnClickListener(v -> submitComment());
    }


    /**
     * 同时加载两个API数据
     */
    private void loadAllData() {
        // 取消之前的任务（避免重复请求）
        if (hostTask != null) hostTask.cancelTask();
        if (commentsTask != null) commentsTask.cancelTask();

        // 1. 加载Host API（传入标识TAG_HOST + URL）
        hostTask = new LoadDataAsyncTask(this, this, true);
        hostTask.execute(URLContent.TAG_HOST, URLContent.getHostInfoURL());

        // 2. 加载Comments API（传入标识TAG_COMMENTS + URL）
        commentsTask = new LoadDataAsyncTask(this, this, false); // 不重复显示加载框
        commentsTask.execute(URLContent.TAG_COMMENTS, URLContent.getCommentsURL());
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

        // 5. 执行提交任务
        submitCommentTask = new SubmitCommentAsyncTask(this, new SubmitCommentAsyncTask.OnSubmitCommentListener() {
            @Override
            public void onSuccess(Comment newComment) {
                // 恢复按钮状态
                btnSubmitComment.setEnabled(true);
                btnSubmitComment.setText("提交评论");

                // 原有逻辑
                commentAdapter.addComment(newComment);
                rvComments.scrollToPosition(commentAdapter.getItemCount() - 1);
                etCommentInput.setText("");
                Toast.makeText(LiveRoomActivicy.this, "评论提交成功！", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMsg) {
                // 恢复按钮状态
                btnSubmitComment.setEnabled(true);
                btnSubmitComment.setText("提交评论");

                Toast.makeText(LiveRoomActivicy.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
        submitCommentTask.execute(commentContent);
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
                com.example.tiktok_live.model.Host host = gson.fromJson(json, com.example.tiktok_live.model.Host.class);
                showHostData(host);
            } else if (URLContent.TAG_COMMENTS.equals(apiTag)) {
                // 解析Comment数组
                List<Comment> commentList = gson.fromJson(
                        json,
                        new TypeToken<List<Comment>>() {}.getType()
                );
                showCommentData(commentList);
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
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
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
        videoView = findViewById(R.id.vv_live_background);
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.live_party_ing);
        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // 1. 基础配置：循环+静音
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0, 0);

                // 2. 获取父布局（ConstraintLayout），等待其完成测量后再调整视频尺寸
                View parentLayout = (View) videoView.getParent(); // 父布局是ConstraintLayout
                parentLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // 监听一次后移除，避免重复触发
                        parentLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        // 3. 获取父布局的实际宽高（此时已完成测量）
                        int parentWidth = parentLayout.getWidth();
                        int parentHeight = parentLayout.getHeight();
                        if (parentWidth == 0 || parentHeight == 0) {
                            return; // 异常情况直接返回
                        }

                        // 4. 计算视频的宽高比
                        int videoWidth = mediaPlayer.getVideoWidth();
                        int videoHeight = mediaPlayer.getVideoHeight();
                        float videoRatio = (float) videoWidth / videoHeight;
                        float parentRatio = (float) parentWidth / parentHeight;

                        // 5. 创建 ConstraintLayout 的布局参数（父布局是ConstraintLayout，必须用对应LayoutParams）
                        ConstraintLayout.LayoutParams videoParams = new ConstraintLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT
                        );

                        // 6. 根据宽高比调整视频尺寸（保持视频比例，避免拉伸）
                        if (videoRatio > parentRatio) {
                            // 视频更宽 → 宽度匹配父布局，高度按比例缩放
                            videoParams.width = parentWidth;
                            videoParams.height = (int) (parentWidth / videoRatio);
                        } else {
                            // 视频更高 → 高度匹配父布局，宽度按比例缩放
                            videoParams.height = parentHeight;
                            videoParams.width = (int) (parentHeight * videoRatio);
                        }

                        // 7. 关键：设置视频在父布局中【水平+垂直居中】
                        videoParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID; // 左对齐父布局
                        videoParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;     // 右对齐父布局
                        videoParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;     // 上对齐父布局
                        videoParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID; // 下对齐父布局

                        // 8. 应用布局参数并启动播放
                        videoView.setLayoutParams(videoParams);
                        videoView.start();
                    }
                });
            }
        });
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

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!videoView.isPlaying()) {
            videoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消所有任务，避免内存泄漏
        if (hostTask != null) hostTask.cancelTask();
        if (commentsTask != null) commentsTask.cancelTask();
    }
}
