package com.sinus.pinmap.data.store

import androidx.room.*
import com.sinus.pinmap.data.entity.OfflineMap
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineMapStore {
    @Query("SELECT * FROM offline_maps ORDER BY downloadDate DESC")
    fun getAllOfflineMaps(): Flow<List<OfflineMap>>

    @Query("SELECT * FROM offline_maps WHERE id = :mapId")
    suspend fun getOfflineMapById(mapId: Long): OfflineMap?

    @Query("SELECT * FROM offline_maps WHERE isDownloaded = 1")
    fun getDownloadedMaps(): Flow<List<OfflineMap>>

    @Insert
    suspend fun insertOfflineMap(offlineMap: OfflineMap): Long

    @Update
    suspend fun updateOfflineMap(offlineMap: OfflineMap)

    @Delete
    suspend fun deleteOfflineMap(offlineMap: OfflineMap)

    @Query("DELETE FROM offline_maps WHERE id = :mapId")
    suspend fun deleteOfflineMapById(mapId: Long)
}
