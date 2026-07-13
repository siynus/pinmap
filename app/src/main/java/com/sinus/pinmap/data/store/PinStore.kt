package com.sinus.pinmap.data.store

import androidx.room.*
import com.sinus.pinmap.data.entity.Pin
import kotlinx.coroutines.flow.Flow

@Dao
interface PinStore {
    @Query("SELECT * FROM pins ORDER BY updatedAt DESC")
    fun getAllPins(): Flow<List<Pin>>

    @Query("SELECT * FROM pins WHERE id = :pinId")
    suspend fun getPinById(pinId: Long): Pin?

    @Query("SELECT * FROM pins WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun getPinsByCategory(categoryId: Long): Flow<List<Pin>>

    @Query("SELECT * FROM pins WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchPins(query: String): Flow<List<Pin>>

    @Insert
    suspend fun insertPin(pin: Pin): Long

    @Update
    suspend fun updatePin(pin: Pin)

    @Delete
    suspend fun deletePin(pin: Pin)

    @Query("DELETE FROM pins WHERE id = :pinId")
    suspend fun deletePinById(pinId: Long)
}
