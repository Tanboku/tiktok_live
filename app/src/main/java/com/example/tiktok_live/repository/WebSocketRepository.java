// file: app/src/main/java/com/example/tiktok_live/repository/WebSocketRepository.java
package com.example.tiktok_live.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.tiktok_live.websocket.MessageEvent;
import com.example.tiktok_live.websocket.OnMessageListener;
import com.example.tiktok_live.websocket.WsManager;

public class WebSocketRepository implements OnMessageListener {
    private MutableLiveData<MessageEvent> messageLiveData = new MutableLiveData<>();
    private MutableLiveData<Integer> onlineCountLiveData = new MutableLiveData<>();
    
    public WebSocketRepository() {
        // 注册WebSocket观察者
        WsManager.getInstance().addOnMessageListener(this);
    }
    
    public LiveData<MessageEvent> getMessageLiveData() {
        return messageLiveData;
    }
    
    public LiveData<Integer> getOnlineCountLiveData() {
        return onlineCountLiveData;
    }
    
    @Override
    public void onMessageReceived(MessageEvent event) {
        messageLiveData.postValue(event);
        // 处理在线人数更新逻辑
        String newMsg = event.getMessage();
        if (newMsg != null) {
            // 这里可以处理特定的消息类型
            // 在ViewModel中处理具体的在线人数逻辑
        }
    }
    
    public void connect() {
        WsManager.getInstance().connect();
    }
    
    public void disconnect() {
        WsManager.getInstance().disconnect();
    }
    
    public void removeListener() {
        WsManager.getInstance().removeOnMessageListener(this);
    }
}
