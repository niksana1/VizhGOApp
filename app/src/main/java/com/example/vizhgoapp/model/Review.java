package com.example.vizhgoapp.model;

import java.util.Date;

public class Review {
    private String userId;
    private int rating;
    private String text;
    private String createdBy;
    private Date createdAt;

    public Review(){}

    public Review(String userId, int rating, String text, String createdBy, Date createdAt) {
        this.userId = userId;
        this.rating = rating;
        this.text = text;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
