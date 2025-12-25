package com.example.lostandfound;

import java.io.Serializable;

public class NotificationItem implements Serializable {
    public String id;
    public String toUserId;
    public String fromUserId;
    public String fromEmail;
    public String postId;
    public String commentId;
    public String type;      // COMMENT, REPLY, FOUND, MESSAGE...
    public String content;
    public long timestamp;
    public boolean isRead;

    public NotificationItem() {
    }

    public NotificationItem(String id, String toUserId, String fromUserId, String fromEmail,
                            String postId, String commentId, String type, String content,
                            long timestamp, boolean isRead) {
        this.id = id;
        this.toUserId = toUserId;
        this.fromUserId = fromUserId;
        this.fromEmail = fromEmail;
        this.postId = postId;
        this.commentId = commentId;
        this.type = type;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }
}