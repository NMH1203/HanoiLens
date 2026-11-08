package vn.edu.usth.hanoilens.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface DestinationDao {

    @Query("SELECT * FROM destinations ORDER BY isFeatured DESC, name ASC")
    LiveData<List<DestinationEntity>> getAllDestinations();

    @Query("SELECT * FROM destinations WHERE id = :id LIMIT 1")
    LiveData<DestinationEntity> getDestinationById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DestinationEntity> destinations);

}
