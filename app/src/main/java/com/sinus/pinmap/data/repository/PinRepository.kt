package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.PinStore
import com.sinus.pinmap.data.entity.Pin
import kotlinx.coroutines.flow.Flow
/**
 * 标记数据仓库
 */
class PinRepository(private val pinStore: PinStore) {
    fun getAllPins(): Flow<List<Pin>> = pinStore.getAllPins()

    suspend fun getPinById(pinId: Long): Pin? = pinStore.getPinById(pinId)

    fun getPinsByCategory(categoryId: Long): Flow<List<Pin>> = pinStore.getPinsByCategory(categoryId)

    fun searchPins(query: String): Flow<List<Pin>> = pinStore.searchPins(query)

    suspend fun insertPin(pin: Pin): Long = pinStore.insertPin(pin)

    suspend fun updatePin(pin: Pin) = pinStore.updatePin(pin)

    suspend fun deletePin(pin: Pin) = pinStore.deletePin(pin)

    suspend fun deletePinById(pinId: Long) = pinStore.deletePinById(pinId)
}