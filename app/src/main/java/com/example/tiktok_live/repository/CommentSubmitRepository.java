// file: app/src/main/java/com/example/tiktok_live/repository/CommentSubmitRepository.java
package com.example.tiktok_live.repository;

import android.content.Context;
import com.example.tiktok_live.asynctask.SubmitCommentAsyncTask;
import com.example.tiktok_live.model.Comment;

public class CommentSubmitRepository {
    private Context context;

    public CommentSubmitRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void submitComment(String commentContent, SubmitCommentCallback callback) {
        new SubmitCommentAsyncTask(context, new SubmitCommentAsyncTask.OnSubmitCommentListener() {
            @Override
            public void onSuccess(Comment newComment) {
                callback.onSuccess(newComment);
            }

            @Override
            public void onFailure(String errorMsg) {
                callback.onError(errorMsg);
            }
        }).execute(commentContent);
    }

    public interface SubmitCommentCallback {
        void onSuccess(Comment newComment);
        void onError(String errorMsg);
    }
}
