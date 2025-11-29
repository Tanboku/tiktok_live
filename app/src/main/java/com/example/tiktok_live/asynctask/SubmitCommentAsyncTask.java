package com.example.tiktok_live.asynctask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.tiktok_live.model.Comment;
import com.example.tiktok_live.model.URLContent;
import com.example.tiktok_live.utils.HttpUtils;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

// 泛型参数：Params（输入评论内容）、Progress（无）、Result（返回的Comment对象）
public class SubmitCommentAsyncTask extends AsyncTask<String, Void, Comment> {
    private static final String TAG = "SubmitCommentAsyncTask";
    private Context context;
    private OnSubmitCommentListener listener;

    // 回调接口：通知提交结果
    public interface OnSubmitCommentListener {
        void onSuccess(Comment newComment); // 提交成功，返回新评论
        void onFailure(String errorMsg);   // 提交失败
    }

    public SubmitCommentAsyncTask(Context context, OnSubmitCommentListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected Comment doInBackground(String... params) {
        // params[0] 是评论内容
        if (params == null || params.length == 0 || params[0].trim().isEmpty()) {
            return null;
        }
        String commentContent = params[0].trim();

        // 1. 构造POST参数
        Map<String, String> postParams = new HashMap<>();
        postParams.put("comment", commentContent); // 对应接口的 comment 参数

        // 2. 调用POST请求（评论接口URL）
        String json = HttpUtils.postFormData(URLContent.getCommentsURL(), postParams);

        // 3. 解析JSON为Comment对象（接口返回单个Comment）
        if (json != null && !json.isEmpty()) {
            try {
                Gson gson = new Gson();
                return gson.fromJson(json, Comment.class);
            } catch (Exception e) {
                Log.e(TAG, "解析评论失败：" + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Comment comment) {
        super.onPostExecute(comment);
        if (listener != null) {
            if (comment != null && comment.getId() != null) {
                // 提交成功，返回新评论
                listener.onSuccess(comment);
            } else {
                // 提交失败
                listener.onFailure("评论提交失败，请检查网络");
            }
        }
    }
}