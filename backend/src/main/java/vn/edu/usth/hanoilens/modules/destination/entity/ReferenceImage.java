package vn.edu.usth.hanoilens.modules.destination.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "reference_images")
public class ReferenceImage {

    public static final String ROLE_COVER = "COVER";

    @Id
    private UUID id;

    @Column(name = "destination_id")
    private UUID destinationId;

    @Column(name = "artifact_id")
    private UUID artifactId;

    @Column(name = "asset_url", nullable = false)
    private String assetUrl;

    private String caption;

    @Column(name = "image_role", nullable = false, length = 24)
    private String imageRole;

    protected ReferenceImage() {
    }

    public UUID getId() { return id; }
    public UUID getDestinationId() { return destinationId; }
    public String getAssetUrl() { return assetUrl; }
    public String getCaption() { return caption; }
    public String getImageRole() { return imageRole; }
}
