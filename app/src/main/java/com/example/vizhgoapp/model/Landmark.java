package com.example.vizhgoapp.model;

import java.io.Serializable;
import java.util.Date;

public class Landmark implements Serializable {
    private String id, userId;
    private String name;
    private String pictureUrl;
    private String description;
    private String link;
    private double longitude, latitude;
    private String category, subCategory;
    private Date createdAt;
    private String createdBy;

    // Ratings: maintain running totals for average(totalRatings)
    private int totalRatings = 0;
    private int totalScore = 0;

    public Landmark(){};

    public Landmark(String id, String userId, String name, String pictureUrl, String description, String link, double longitude, double latitude, String category, String subCategory, Date createdAt, String createdBy){
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.description = description;
        this.link = link;
        this.longitude = longitude;
        this.latitude = latitude;
        this.category = category;
        this.subCategory = subCategory;
        this.createdAt = createdAt;
        this.createdBy = createdBy;

    }
    public double getAverageRating() {
        return totalRatings > 0 ? (double) totalScore / totalRatings : 0.0;
    }

    public String getRatingDisplay() {
        return String.format("⭐%.1f (%d гласа)", getAverageRating(), totalRatings);
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
