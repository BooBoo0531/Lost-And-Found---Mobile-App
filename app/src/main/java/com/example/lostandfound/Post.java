package com.example.lostandfound;

import java.io.Serializable;

public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== Schema mới (đang dùng trong PostActivity/PostAdapter mới) =====
    private String id;
    private String userId;
    private String userEmail;
    private String timePosted;
    private String description;
    private String postType;      // "LOST" hoặc "FOUND"
    private String imageBase64;
    private String contact;
    private String address;

    // ===== Schema cũ (để đọc dữ liệu Firebase cũ / code cũ không crash) =====
    private String userName;      // alias của userEmail
    private String content;       // alias của description
    private String status;        // alias của postType
    private String contactPhone;  // alias của contact

    // ✅ BẮT BUỘC: Firebase cần constructor rỗng
    public Post() {}

    // ===== Constructor kiểu CŨ (8 params) =====
    public Post(String id,
                String userName,
                String timePosted,
                String content,
                String status,
                String imageBase64,
                String contactPhone,
                String address) {

        this.id = id;
        this.userName = userName;
        this.timePosted = timePosted;
        this.content = content;
        this.status = status;
        this.imageBase64 = imageBase64;
        this.contactPhone = contactPhone;
        this.address = address;

        // map qua schema mới để adapter mới vẫn dùng được
        this.userId = "";
        this.userEmail = userName;
        this.description = content;
        this.postType = status;
        this.contact = contactPhone;
    }

    // ===== Constructor kiểu MỚI (9 params) =====
    public Post(String id,
                String userId,
                String userEmail,
                String timePosted,
                String description,
                String postType,
                String imageBase64,
                String contact,
                String address) {

        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.timePosted = timePosted;
        this.description = description;
        this.postType = postType;
        this.imageBase64 = imageBase64;
        this.contact = contact;
        this.address = address;

        // map qua schema cũ để code cũ không crash
        this.userName = userEmail;
        this.content = description;
        this.status = postType;
        this.contactPhone = contact;
    }

    // ===== Getters/Setters (NEW) =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() {
        if (userEmail != null && !userEmail.isEmpty()) return userEmail;
        return userName; // fallback dữ liệu cũ
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        this.userName = userEmail; // đồng bộ schema cũ
    }

    public String getTimePosted() { return timePosted; }
    public void setTimePosted(String timePosted) { this.timePosted = timePosted; }

    public String getDescription() {
        if (description != null && !description.isEmpty()) return description;
        return content; // fallback dữ liệu cũ
    }
    public void setDescription(String description) {
        this.description = description;
        this.content = description; // đồng bộ schema cũ
    }

    public String getPostType() {
        if (postType != null && !postType.isEmpty()) return postType;
        return status; // fallback dữ liệu cũ
    }
    public void setPostType(String postType) {
        this.postType = postType;
        this.status = postType; // đồng bộ schema cũ
    }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getContact() {
        if (contact != null && !contact.isEmpty()) return contact;
        return contactPhone; // fallback dữ liệu cũ
    }
    public void setContact(String contact) {
        this.contact = contact;
        this.contactPhone = contact; // đồng bộ schema cũ
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // ===== Alias getters/setters (OLD) để code cũ khỏi đỏ =====
    public String getUserName() { return getUserEmail(); }
    public void setUserName(String userName) { setUserEmail(userName); }

    public String getContent() { return getDescription(); }
    public void setContent(String content) { setDescription(content); }

    public String getStatus() { return getPostType(); }
    public void setStatus(String status) { setPostType(status); }

    public String getContactPhone() { return getContact(); }
    public void setContactPhone(String contactPhone) { setContact(contactPhone); }
}
