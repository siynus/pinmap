package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.PinStore
import com.sinus.pinmap.data.entity.Pin
import kotlinx.coroutines.flow.Flow
/**
 * 标记数据仓库
 */
class PinRepository(private val mPinStore: PinStore) {
    fun getAllPins(): Flow<List<Pin>> = mPinStore.getAllPins()

    suspend fun getPinById(pinId: Long): Pin? = mPinStore.getPinById(pinId)

    fun getPinsByCategory(categoryId: Long): Flow<List<Pin>> = mPinStore.getPinsByCategory(categoryId)

    fun searchPins(query: String): Flow<List<Pin>> = mPinStore.searchPins(query)

    suspend fun insertPin(pin: Pin): Long = mPinStore.insertPin(pin)

    suspend fun updatePin(pin: Pin) = mPinStore.updatePin(pin)

    suspend fun deletePin(pin: Pin) = mPinStore.deletePin(pin)

    suspend fun deletePinById(pinId: Long) = mPinStore.deletePinById(pinId)
}