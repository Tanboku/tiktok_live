package com.example.tiktok_live.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tiktok_live.R;
import com.example.tiktok_live.model.Comment;

import java.util.ArrayList;
import java.util.List;

// 公屏评论适配器
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    // 评论数据源（初始为空，动态添加）
    private List<Comment> commentList = new ArrayList<>();

    // 构造方法（可选：传入初始评论列表）
    public CommentAdapter() {}

    public CommentAdapter(List<Comment> initialComments) {
        if (initialComments != null) {
            commentList.addAll(initialComments);
        }
    }

    // 创建 ViewHolder（加载评论项布局）
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false); // 后续创建评论项布局
        return new CommentViewHolder(itemView);
    }

    // 绑定数据（将评论显示到 UI 控件）
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvUserName.setText(comment.getUserName()); // 用户名
        holder.tvContent.setText(comment.getContent());   // 评论内容
        holder.tvTime.setText(comment.getTime());         // 发送时间
    }

    // 获取评论总数
    @Override
    public int getItemCount() {
        return commentList.size();
    }

    // 新增一条评论（发送评论后调用）
    public void addComment(Comment comment) {
        if (comment != null && !comment.getContent().isEmpty()) {
            commentList.add(comment);
            // 只刷新最后一条，优化性能（避免整个列表重绘）
            notifyItemInserted(commentList.size() - 1);
        }
    }

    // 评论项 ViewHolder（持有单条评论的 UI 控件）
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName; // 用户名
        TextView tvContent;  // 评论内容
        TextView tvTime;     // 发送时间

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            // 绑定控件（从评论项布局中获取）
            tvUserName = itemView.findViewById(R.id.tv_comment_username);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
        }
    }
}