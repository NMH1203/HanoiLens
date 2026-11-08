package vn.edu.usth.hanoilens.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Cached heritage destination entity for offline exploration.
 * Privacy & Security Notice: Exact live user GPS coordinates are NEVER persisted here.
 */
@Entity(tableName = "destinations")
public class DestinationEntity {

    @PrimaryKey
    @NonNull
    private String id;

    private String slug;
    private String name;
    private String area;
    private String district;
    private String summary;
    private String coverImageUrl;
    private String bestTime;
    private int estimatedMinutes;
    private int landmarkCount;
    private boolean isFeatured;
    private long cachedAt;

    public DestinationEntity(@NonNull String id, String slug, String name, String area,
                             String district, String summary, String coverImageUrl,
                             String bestTime, int estimatedMinutes, int landmarkCount,
                             boolean isFeatured, long cachedAt) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.area = area;
        this.district = district;
        this.summary = summary;
        this.coverImageUrl = coverImageUrl;
        this.bestTime = bestTime;
        this.estimatedMinutes = estimatedMinutes;
        this.landmarkCount = landmarkCount;
        this.isFeatured = isFeatured;
        this.cachedAt = cachedAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
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

    public boolean isFeatured() {
        return isFeatured;
    }

    public void setFeatured(boolean featured) {
        isFeatured = featured;
    }

    public long getCachedAt() {
        return cachedAt;
    }

    public void setCachedAt(long cachedAt) {
        this.cachedAt = cachedAt;
    }
}
