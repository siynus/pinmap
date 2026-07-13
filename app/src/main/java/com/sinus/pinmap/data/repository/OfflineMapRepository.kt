package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.OfflineMapStore
import com.sinus.pinmap.data.entity.OfflineMap
import kotlinx.coroutines.flow.Flow
/**
 * 离线地图数据仓库
 */
class OfflineMapRepository(private val offlineMapStore: OfflineMapStore) {
    fun getAllOfflineMaps(): Flow<List<OfflineMap>> = offlineMapStore.getAllOfflineMaps()

    suspend fun getOfflineMapById(mapId: Long): OfflineMap? = offlineMapStore.getOfflineMapById(mapId)

    fun getDownloadedMaps(): Flow<List<OfflineMap>> = offlineMapStore.getDownloadedMaps()

    suspend fun insertOfflineMap(offlineMap: OfflineMap): Long = offlineMapStore.insertOfflineMap(offlineMap)

    suspend fun updateOfflineMap(offlineMap: OfflineMap) = offlineMapStore.updateOfflineMap(offlineMap)

    suspend fun deleteOfflineMap(offlineMap: OfflineMap) = offlineMapStore.deleteOfflineMap(offlineMap)

    suspend fun deleteOfflineMapById(mapId: Long) = offlineMapStore.deleteOfflineMapById(mapId)
}