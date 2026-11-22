package vn.edu.usth.hanoilens.modules.destination.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "landmarks")
public class Landmark {

    @Id
    private UUID id;

    @Column(name = "destination_id", nullable = false)
    private UUID destinationId;

    @Column(nullable = false, length = 160)
    private String name;

    private String description;

    @Column(name = "is_checkpoint", nullable = false)
    private boolean checkpoint;

    protected Landmark() {
    }

    public UUID getId() { return id; }
    public UUID getDestinationId() { return destinationId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isCheckpoint() { return checkpoint; }
}
