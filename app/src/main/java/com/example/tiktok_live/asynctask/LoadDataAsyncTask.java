package com.example.tiktok_live.asynctask;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class LoadDataAsyncTask extends AsyncTask<Object, Void, String[]> {
    private static final String TAG = "LoadDataAsyncTask";
    private Context context;
    private OnGetNetDataListener listener;
    private ProgressDialog dialog;
    private boolean flag = false;

    private void initDialog() {
        dialog = new ProgressDialog(context);
        dialog.setTitle("提示信息");
        dialog.setMessage("正在加载中....");
        dialog.setCancelable(false);
    }

    public LoadDataAsyncTask(Context context, OnGetNetDataListener listener, boolean flag) {
        this.context = context;
        this.listener = listener;
        this.flag = flag;
        if (flag) initDialog();
    }

    public interface OnGetNetDataListener {
        void onSuccess(String apiTag, String json);
        void onFailure(String errorMsg);
    }

    @Override
    protected String[] doInBackground(Object... params) {
        if (params == null || params.length < 2) {
            Log.e(TAG, "参数错误：需传入API标识和URL");
            return new String[]{null, null};
        }

        String apiTag = (String) params[0];
        String url = (String) params[1];
        // 关键修改：将apiTag作为参数传入getJSONFromUrl
        String json = getJSONFromUrl(apiTag, url);

        return new String[]{apiTag, json};
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (flag && dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    @Override
    protected void onPostExecute(String[] result) {
        super.onPostExecute(result);
        if (flag && dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        String apiTag = result[0];
        String json = result[1];
        if (listener != null) {
            if (apiTag != null && json != null && !json.isEmpty()) {
                listener.onSuccess(apiTag, json);
            } else {
                listener.onFailure("获取数据失败（参数错误或网络异常）");
            }
        }
    }

    public void cancelTask() {
        if (!isCancelled()) {
            cancel(true);
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    // 关键修改：增加apiTag参数，接收外部传入的标识
    private String getJSONFromUrl(String apiTag, String path) {
        String json = "";
        HttpURLConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream bos = null;

        try {
            URL url = new URL(path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
            conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(5));
            conn.setDoInput(true);
            conn.setDoOutput(false);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
                bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int hasRead;
                while ((hasRead = is.read(buf)) != -1) {
                    bos.write(buf, 0, hasRead);
                }
                json = bos.toString("UTF-8");
                // 现在apiTag是方法参数，可正常使用
                Log.i(TAG, apiTag + "返回JSON：" + json);
            } else {
                Log.e(TAG, apiTag + "请求失败，响应码：" + conn.getResponseCode());
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, apiTag + "URL格式错误：" + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, apiTag + "网络异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) bos.close();
                if (is != null) is.close();
                if (conn != null) conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return json;
    }
}