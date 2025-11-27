package com.example.tiktok_live.activity;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktok_live.R;
import com.example.tiktok_live.adapter.CommentAdapter;
import com.example.tiktok_live.model.Comment;

import java.util.ArrayList;
import java.util.List;

public class LiveRoomActivicy extends AppCompatActivity {

    private VideoView videoView;
    private RecyclerView rvChat;
    private EditText etComment;
    private Button btnSend;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>(); // 评论数据源

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_room);
        // 1. 初始化视频背景（之前的代码，保持不变）
        initVideoBackground();
        // 2. 初始化公屏聊天和评论发送功能
        initChatFunction();
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
        rvChat = findViewById(R.id.rv_chat);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);

        // 1. 初始化 RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL); // 垂直方向滚动
        rvChat.setLayoutManager(layoutManager);

        // 2. 初始化适配器（可添加测试评论）
        commentAdapter = new CommentAdapter(commentList);
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
        videoView.stopPlayback();
    }
}
