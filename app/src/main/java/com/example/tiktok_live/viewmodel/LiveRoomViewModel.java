// file: app/src/main/java/com/example/tiktok_live/viewmodel/LiveRoomViewModel.java
package com.example.tiktok_live.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.tiktok_live.model.Comment;
import com.example.tiktok_live.model.Host;
import com.example.tiktok_live.repository.CommentRepository;
import com.example.tiktok_live.repository.CommentSubmitRepository;
import com.example.tiktok_live.repository.HostRepository;
import com.example.tiktok_live.repository.WebSocketRepository;
import com.example.tiktok_live.websocket.MessageEvent;
import java.util.List;

public class LiveRoomViewModel extends AndroidViewModel {
    private MutableLiveData<Host> hostLiveData = new MutableLiveData<>();
    private MutableLiveData<List<Comment>> commentListLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private MutableLiveData<Integer> onlineCountLiveData = new MutableLiveData<>(12);
    private CommentSubmitRepository commentSubmitRepository;
    private MutableLiveData<Boolean> isSubmitting = new MutableLiveData<>(false);

    private HostRepository hostRepository;
    private CommentRepository commentRepository;
    private WebSocketRepository webSocketRepository;

    public LiveRoomViewModel(@NonNull Application application) {
        super(application);
        hostRepository = new HostRepository(application);
        commentRepository = new CommentRepository(application);
        webSocketRepository = new WebSocketRepository();
        webSocketRepository.getMessageLiveData().observeForever(this::handleWebSocketMessage);
        commentSubmitRepository = new CommentSubmitRepository(application);
        // 初始化加载数据
        loadHostInfo();
        loadComments();
    }

    public void loadHostInfo() {
        hostRepository.loadHostInfo(new HostRepository.LoadDataCallback() {
            @Override
            public void onSuccess(Host host) {
                hostLiveData.postValue(host);
            }

            @Override
            public void onError(String errorMsg) {
                errorLiveData.postValue(errorMsg);
            }
        });
    }

    public void loadComments() {
        commentRepository.loadComments(new CommentRepository.LoadDataCallback() {
            @Override
            public void onSuccess(List<Comment> comments) {
                commentListLiveData.postValue(comments);
            }

            @Override
            public void onError(String errorMsg) {
                errorLiveData.postValue(errorMsg);
            }
        });
    }

    private void handleWebSocketMessage(MessageEvent event) {
        String newMsg = event.getMessage();
        if (newMsg != null) {
            Integer currentCount = onlineCountLiveData.getValue();
            int newCount = (currentCount != null ? currentCount : 12) + 1;
            onlineCountLiveData.setValue(newCount);
        }
    }

    public void submitComment(String commentContent) {
        if (isSubmitting.getValue() != null && isSubmitting.getValue()) {
            return; // 防止重复提交
        }

        isSubmitting.setValue(true);
        commentSubmitRepository.submitComment(commentContent, new CommentSubmitRepository.SubmitCommentCallback() {
            @Override
            public void onSuccess(Comment newComment) {
                isSubmitting.postValue(false);
                // 评论提交成功后，可以更新评论列表
                List<Comment> currentComments = commentListLiveData.getValue();
                if (currentComments != null) {
                    currentComments.add(newComment);
                    commentListLiveData.postValue(currentComments);
                }
            }

            @Override
            public void onError(String errorMsg) {
                isSubmitting.postValue(false);
                errorLiveData.postValue(errorMsg);
            }
        });
    }

    public LiveData<Boolean> getIsSubmitting() {
        return isSubmitting;
    }

    public LiveData<Host> getHostInfo() {
        return hostLiveData;
    }

    public LiveData<List<Comment>> getCommentList() {
        return commentListLiveData;
    }

    public LiveData<String> getErrorMsg() {
        return errorLiveData;
    }

    public LiveData<Integer> getOnlineCount() {
        return onlineCountLiveData;
    }

    public void setHostInfo(Host host) {
        hostLiveData.setValue(host);
    }

    public void setCommentList(List<Comment> comments) {
        commentListLiveData.setValue(comments);
    }

    public void setErrorMsg(String errorMsg) {
        errorLiveData.setValue(errorMsg);
    }

    public void setOnlineCount(int count) {
        onlineCountLiveData.setValue(count);
    }

    public void connectWebSocket() {
        webSocketRepository.connect();
    }

    public void disconnectWebSocket() {
        webSocketRepository.disconnect();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        webSocketRepository.removeListener();
    }
}
