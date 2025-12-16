package com.example.lostandfound;

public class Post {
    public String id;
    public String userId;
    public String userEmail;
    public String timePosted;
    public String description;
    public String postType;
    public String imageBase64;
    public String contact;
    public String address;

    // No-arg constructor required by Firebase
    public Post() { }

    // Constructor matching: (postId, userId, userEmail, timePosted, description, postType, imageBase64, contact, address)
    public Post(String id, String userId, String userEmail, String timePosted,
                String description, String postType, String imageBase64,
                String contact, String address) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.timePosted = timePosted;
        this.description = description;
        this.postType = postType;
        this.imageBase64 = imageBase64;
        this.contact = contact;
        this.address = address;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getTimePosted() { return timePosted; }
    public void setTimePosted(String timePosted) { this.timePosted = timePosted; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
