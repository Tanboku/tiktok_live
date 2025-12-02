// file: app/src/main/java/com/example/tiktok_live/repository/CommentRepository.java
package com.example.tiktok_live.repository;

import android.content.Context;
import com.example.tiktok_live.asynctask.LoadDataAsyncTask;
import com.example.tiktok_live.model.Comment;
import com.example.tiktok_live.model.URLContent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class CommentRepository {
    private Context context;

    public CommentRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void loadComments(LoadDataCallback callback) {
        new LoadDataAsyncTask(context, new LoadDataAsyncTask.OnGetNetDataListener() {
            @Override
            public void onSuccess(String apiTag, String json) {
                try {
                    if (URLContent.TAG_COMMENTS.equals(apiTag)) {
                        if (json != null && !json.isEmpty()) {
                            Type listType = new TypeToken<List<Comment>>() {}.getType();
                            Gson gson = new Gson();
                            List<Comment> comments = gson.fromJson(json, listType);
                            callback.onSuccess(comments);
                        } else {
                            callback.onError("评论数据为空");
                        }
                    }
                } catch (Exception e) {
                    callback.onError("评论数据解析失败: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                callback.onError(errorMsg);
            }
        }, false).execute(URLContent.TAG_COMMENTS, URLContent.getCommentsURL());
    }
    
    public interface LoadDataCallback {
        void onSuccess(List<Comment> comments);
        void onError(String errorMsg);
    }
}
