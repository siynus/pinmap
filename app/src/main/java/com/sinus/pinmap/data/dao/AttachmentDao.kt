package com.sinus.pinmap.data.dao

import androidx.room.*
import com.sinus.pinmap.data.entity.Attachment
import kotlinx.coroutines.flow.Flow

/**
 * 附件数据访问对象
 */
@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE pinId = :pinId")
    fun getAttachmentsByPinId(pinId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: Long): Attachment?

    @Insert
    suspend fun insertAttachment(attachment: Attachment): Long

    @Update
    suspend fun updateAttachment(attachment: Attachment)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE pinId = :pinId")
    suspend fun deleteAttachmentsByPinId(pinId: Long)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: Long)
}