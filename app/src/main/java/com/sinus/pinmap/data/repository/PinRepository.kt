package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.dao.PinDao
import com.sinus.pinmap.data.entity.Pin
import kotlinx.coroutines.flow.Flow

/**
 * 标记数据仓库
 */
class PinRepository(private val pinDao: PinDao) {
    fun getAllPins(): Flow<List<Pin>> = pinDao.getAllPins()

    suspend fun getPinById(pinId: Long): Pin? = pinDao.getPinById(pinId)

    fun getPinsByCategory(categoryId: Long): Flow<List<Pin>> = pinDao.getPinsByCategory(categoryId)

    fun searchPins(query: String): Flow<List<Pin>> = pinDao.searchPins(query)

    suspend fun insertPin(pin: Pin): Long = pinDao.insertPin(pin)

    suspend fun updatePin(pin: Pin) = pinDao.updatePin(pin)

    suspend fun deletePin(pin: Pin) = pinDao.deletePin(pin)

    suspend fun deletePinById(pinId: Long) = pinDao.deletePinById(pinId)
}