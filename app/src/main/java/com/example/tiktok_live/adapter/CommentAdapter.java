package com.example.tiktok_live.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tiktok_live.R;
import com.example.tiktok_live.model.Comment;

import java.util.List;

/**
 * 评论列表RecyclerView适配器
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private Context context;
    private List<Comment> commentList; // 评论数据列表

    // 构造方法：传入上下文和数据
    public CommentAdapter(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
    }

    /**
     * 创建ViewHolder：加载列表项布局
     */
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(itemView);
    }

    /**
     * 绑定数据：将评论数据赋值给列表项控件
     */
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        if (comment == null) return;

        // 1. 加载头像（Glide）
        Glide.with(context)
                .load(comment.getAvatar())
                .circleCrop() // 圆形头像
                .into(holder.ivAvatar);

        // 2. 展示姓名、时间、评论内容
        holder.tvName.setText(comment.getName());
        holder.tvTime.setText(comment.getCreatedAt().replace("T", " ").replace("Z", "")); // 格式化时间
        holder.tvContent.setText(comment.getCommentContent());
    }

    /**
     * 返回列表数据总数
     */
    @Override
    public int getItemCount() {
        return commentList == null ? 0 : commentList.size();
    }

    /**
     * 更新列表数据（刷新用）
     */
    public void updateData(List<Comment> newCommentList) {
        this.commentList = newCommentList;
        notifyDataSetChanged(); // 刷新列表
    }

    // 新增：追加单条评论（提交成功后调用）
    public void addComment(Comment newComment) {
        if (newComment != null) {
            commentList.add(newComment); // 追加到列表末尾
            // 只刷新最后一项，性能更优
            notifyItemInserted(commentList.size() - 1);
        }
    }

    /**
     * ViewHolder：绑定列表项控件
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar; // 头像
        TextView tvName; // 姓名
        TextView tvTime; // 时间
        TextView tvContent; // 评论内容

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_comment_avatar);
            tvName = itemView.findViewById(R.id.tv_comment_name);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
        }
    }
}