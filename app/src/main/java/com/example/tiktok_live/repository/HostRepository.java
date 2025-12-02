// file: app/src/main/java/com/example/tiktok_live/repository/HostRepository.java
package com.example.tiktok_live.repository;

import android.content.Context;
import com.example.tiktok_live.asynctask.LoadDataAsyncTask;
import com.example.tiktok_live.model.Host;
import com.example.tiktok_live.model.URLContent;
import com.google.gson.Gson;

public class HostRepository {
    private Context context;

    public HostRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void loadHostInfo(LoadDataCallback callback) {
        new LoadDataAsyncTask(context, new LoadDataAsyncTask.OnGetNetDataListener() {
            @Override
            public void onSuccess(String apiTag, String json) {
                try {
                    if (URLContent.TAG_HOST.equals(apiTag)) {
                        Host host = new Gson().fromJson(json, Host.class);
                        callback.onSuccess(host);
                    }
                } catch (Exception e) {
                    callback.onError("主播信息解析失败: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                callback.onError(errorMsg);
            }
        }, false).execute(URLContent.TAG_HOST, URLContent.getHostInfoURL());
    }

    public interface LoadDataCallback {
        void onSuccess(Host host);
        void onError(String errorMsg);
    }
}
