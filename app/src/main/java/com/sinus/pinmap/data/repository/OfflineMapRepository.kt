package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.dao.OfflineMapDao
import com.sinus.pinmap.data.entity.OfflineMap
import kotlinx.coroutines.flow.Flow

/**
 * 离线地图数据仓库
 */
class OfflineMapRepository(private val offlineMapDao: OfflineMapDao) {
    fun getAllOfflineMaps(): Flow<List<OfflineMap>> = offlineMapDao.getAllOfflineMaps()

    suspend fun getOfflineMapById(mapId: Long): OfflineMap? = offlineMapDao.getOfflineMapById(mapId)

    fun getDownloadedMaps(): Flow<List<OfflineMap>> = offlineMapDao.getDownloadedMaps()

    suspend fun insertOfflineMap(offlineMap: OfflineMap): Long = offlineMapDao.insertOfflineMap(offlineMap)

    suspend fun updateOfflineMap(offlineMap: OfflineMap) = offlineMapDao.updateOfflineMap(offlineMap)

    suspend fun deleteOfflineMap(offlineMap: OfflineMap) = offlineMapDao.deleteOfflineMap(offlineMap)

    suspend fun deleteOfflineMapById(mapId: Long) = offlineMapDao.deleteOfflineMapById(mapId)
}