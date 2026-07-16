package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.AttachmentStore
import com.sinus.pinmap.data.entity.Attachment
import kotlinx.coroutines.flow.Flow
/**
 * 附件数据仓库
 */
class AttachmentRepository(private val attachmentStore: AttachmentStore) {
    fun getAttachmentsByPinId(pinId: Long): Flow<List<Attachment>> = attachmentStore.getAttachmentsByPinId(pinId)

    suspend fun getAttachmentById(attachmentId: Long): Attachment? = attachmentStore.getAttachmentById(attachmentId)

    suspend fun insertAttachment(attachment: Attachment): Long = attachmentStore.insertAttachment(attachment)

    suspend fun updateAttachment(attachment: Attachment) = attachmentStore.updateAttachment(attachment)

    suspend fun deleteAttachment(attachment: Attachment) = attachmentStore.deleteAttachment(attachment)

    suspend fun deleteAttachmentsByPinId(pinId: Long) = attachmentStore.deleteAttachmentsByPinId(pinId)

    suspend fun deleteAttachmentById(attachmentId: Long) = attachmentStore.deleteAttachmentById(attachmentId)
}