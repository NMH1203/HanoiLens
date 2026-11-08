package vn.edu.usth.hanoilens.data.model;

import com.google.gson.annotations.SerializedName;

public class DestinationDto {

    @SerializedName("id")
    private String id;

    @SerializedName("slug")
    private String slug;

    @SerializedName("name")
    private String name;

    @SerializedName("area")
    private String area;

    @SerializedName("district")
    private String district;

    @SerializedName("summary")
    private String summary;

    @SerializedName("coverImageUrl")
    private String coverImageUrl;

    @SerializedName("bestTime")
    private String bestTime;

    @SerializedName("estimatedMinutes")
    private int estimatedMinutes;

    @SerializedName("landmarkCount")
    private int landmarkCount;

    @SerializedName("distanceMeters")
    private Integer distanceMeters;

    @SerializedName("isFeatured")
    private boolean isFeatured;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getBestTime() {
        return bestTime;
    }

    public void setBestTime(String bestTime) {
        this.bestTime = bestTime;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public int getLandmarkCount() {
        return landmarkCount;
    }

    public void setLandmarkCount(int landmarkCount) {
        this.landmarkCount = landmarkCount;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public boolean isFeatured() {
        return isFeatured;
    }

    public void setFeatured(boolean featured) {
        isFeatured = featured;
    }
}
