package com.example.tiktok_live.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpUtils {
    private static final String TAG = "HttpUtils";
    private static final int CONNECT_TIMEOUT = 5; // 连接超时（秒）
    private static final int READ_TIMEOUT = 5;    // 读取超时（秒）

    // 获取网络JSON数据（GET请求，适配目标API）
    public static String getJSON(String path){
        String json = "";
        HttpURLConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream bos = null;

        try {
            URL url = new URL(path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(CONNECT_TIMEOUT));
            conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(READ_TIMEOUT));
            conn.setDoInput(true);
            conn.setDoOutput(false);

            int responseCode = conn.getResponseCode();
            // 目标API是HTTPS，无需额外配置，系统自动处理证书
            if (responseCode >= 200 && responseCode < 300) {
                is = conn.getInputStream();
                bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int hasRead;
                while ((hasRead = is.read(buf)) != -1) {
                    bos.write(buf, 0, hasRead);
                }
                json = bos.toString("UTF-8"); // 避免中文乱码
                Log.i(TAG, "API返回JSON：" + json);
            } else {
                Log.e(TAG, "请求失败，响应码：" + conn.getResponseCode());
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, "URL格式错误：" + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "网络异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源，避免内存泄漏
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

    // 新增：POST 请求（form-urlencoded 格式，修复 400 错误）
    public static String postFormData(String path, Map<String, String> params) {
        String json = "";
        HttpURLConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream bos = null;
        OutputStream os = null;

        try {
            URL url = new URL(path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST"); // 必须大写 POST（避免小写导致的异常）
            conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(CONNECT_TIMEOUT));
            conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(READ_TIMEOUT));
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false); // 禁用缓存（POST 请求禁用缓存更稳妥）
            conn.setRequestProperty("Connection", "Keep-Alive"); // 保持连接
            conn.setRequestProperty("Charset", "UTF-8"); // 明确字符集
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // 固定格式

            // 1. 构造请求参数（key=value&key=value 格式）
            String paramStr = "";
            if (params != null && !params.isEmpty()) {
                StringBuilder paramBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    // 确保 key 和 value 都经过 UTF-8 编码（处理特殊字符）
                    String key = URLEncoder.encode(entry.getKey(), "UTF-8");
                    String value = URLEncoder.encode(entry.getValue(), "UTF-8");
                    paramBuilder.append(key).append("=").append(value).append("&");
                }
                // 去掉最后一个 &（避免参数格式错误）
                paramStr = paramBuilder.substring(0, paramBuilder.length() - 1);
            }

            // 2. 计算请求体长度，设置 Content-Length（关键修复！）
            byte[] requestBody = paramStr.getBytes("UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(requestBody.length));

            // 3. 写入请求体（必须在设置完所有请求头后执行）
            os = conn.getOutputStream();
            os.write(requestBody);
            os.flush(); // 确保数据全部发送

            // 4. 读取响应（区分成功/失败响应流）
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                is = conn.getInputStream();
            } else {
                // 错误响应也读取流（便于排查问题）
                is = conn.getErrorStream();
                Log.e(TAG, "POST 请求失败，响应码：" + responseCode + "，错误信息：" + streamToString(is));
                return json;
            }

            // 5. 解析响应数据
            bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int hasRead;
            while ((hasRead = is.read(buf)) != -1) {
                bos.write(buf, 0, hasRead);
            }
            json = bos.toString("UTF-8");
            Log.i(TAG, "POST API返回JSON：" + json);

        } catch (MalformedURLException e) {
            Log.e(TAG, "URL格式错误：" + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "网络异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭所有资源（避免内存泄漏）
            try {
                if (os != null) os.close();
                if (bos != null) bos.close();
                if (is != null) is.close();
                if (conn != null) conn.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    // 新增：将输入流转为字符串（用于读取错误信息）
    private static String streamToString(InputStream is) {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}