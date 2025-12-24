package com.example.lostandfound;

import java.io.Serializable;

public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String userId;
    private String userEmail;

    // ✅ timePosted = thời gian đăng bài (auto)
    private String timePosted;

    // ✅ lostFoundTime = thời gian nhặt/mất (user chọn)
    private String lostFoundTime;

    private String description;
    private String postType;
    private String imageBase64;
    private String contact;
    private String address;

    private double lat;
    private double lng;

    // legacy/alias fields
    private String userName;
    private String content;
    private String status;
    private String contactPhone;

    public Post() {}

    // Constructor cũ (giữ để không vỡ code cũ)
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

        this.userId = "";
        this.userEmail = userName;
        this.description = content;
        this.postType = status;
        this.contact = contactPhone;

        this.lat = 0;
        this.lng = 0;

        // ✅ fallback: bài cũ coi timePosted như lostFoundTime nếu chưa có field mới
        this.lostFoundTime = "";
    }

    // Constructor chính (giữ cũ) — timePosted truyền vào (bài cũ)
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

        this.userName = userEmail;
        this.content = description;
        this.status = postType;
        this.contactPhone = contact;

        this.lat = 0;
        this.lng = 0;

        this.lostFoundTime = "";
    }

    // ✅ constructor mới đúng nghiệp vụ
    public Post(String id,
                String userId,
                String userEmail,
                String timePosted,
                String lostFoundTime,
                String description,
                String postType,
                String imageBase64,
                String contact,
                String address) {

        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.timePosted = timePosted;
        this.lostFoundTime = lostFoundTime;
        this.description = description;
        this.postType = postType;
        this.imageBase64 = imageBase64;
        this.contact = contact;
        this.address = address;

        this.userName = userEmail;
        this.content = description;
        this.status = postType;
        this.contactPhone = contact;

        this.lat = 0;
        this.lng = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() {
        if (userEmail != null && !userEmail.isEmpty()) return userEmail;
        return userName;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        this.userName = userEmail;
    }

    // ✅ thời gian đăng bài
    public String getTimePosted() { return timePosted; }
    public void setTimePosted(String timePosted) { this.timePosted = timePosted; }

    // ✅ thời gian nhặt/mất
    // fallback cho bài cũ: nếu lostFoundTime trống thì dùng timePosted (vì trước đây timePosted đang lưu nhặt/mất)
    public String getLostFoundTime() {
        if (lostFoundTime != null && !lostFoundTime.trim().isEmpty()) return lostFoundTime;
        return ""; // để adapter quyết định fallback nếu cần
    }
    public void setLostFoundTime(String lostFoundTime) { this.lostFoundTime = lostFoundTime; }

    public String getDescription() {
        if (description != null && !description.isEmpty()) return description;
        return content;
    }
    public void setDescription(String description) {
        this.description = description;
        this.content = description;
    }

    public String getPostType() {
        if (postType != null && !postType.isEmpty()) return postType;
        return status;
    }
    public void setPostType(String postType) {
        this.postType = postType;
        this.status = postType;
    }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getContact() {
        if (contact != null && !contact.isEmpty()) return contact;
        return contactPhone;
    }
    public void setContact(String contact) {
        this.contact = contact;
        this.contactPhone = contact;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getUserName() { return getUserEmail(); }
    public void setUserName(String userName) { setUserEmail(userName); }

    public String getContent() { return getDescription(); }
    public void setContent(String content) { setDescription(content); }

    public String getStatus() { return getPostType(); }
    public void setStatus(String status) { setPostType(status); }

    public String getContactPhone() { return getContact(); }
    public void setContactPhone(String contactPhone) { setContact(contactPhone); }
}
