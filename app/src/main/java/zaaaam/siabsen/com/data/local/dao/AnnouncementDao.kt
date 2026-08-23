package zaaaam.siabsen.com.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import zaaaam.siabsen.com.data.local.entity.AnnouncementEntity

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY createdAt DESC LIMIT 50")
    fun observeAll(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements ORDER BY createdAt DESC LIMIT 3")
    fun observeLatest(): Flow<List<AnnouncementEntity>>

    @Insert suspend fun insert(a: AnnouncementEntity): Long

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM announcements")
    suspend fun clearAll()
}
