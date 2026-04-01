package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.dao.AttachmentDao
import com.sinus.pinmap.data.entity.Attachment
import kotlinx.coroutines.flow.Flow

/**
 * 附件数据仓库
 */
class AttachmentRepository(private val attachmentDao: AttachmentDao) {
    fun getAttachmentsByPinId(pinId: Long): Flow<List<Attachment>> = attachmentDao.getAttachmentsByPinId(pinId)

    suspend fun getAttachmentById(attachmentId: Long): Attachment? = attachmentDao.getAttachmentById(attachmentId)

    suspend fun insertAttachment(attachment: Attachment): Long = attachmentDao.insertAttachment(attachment)

    suspend fun updateAttachment(attachment: Attachment) = attachmentDao.updateAttachment(attachment)

    suspend fun deleteAttachment(attachment: Attachment) = attachmentDao.deleteAttachment(attachment)

    suspend fun deleteAttachmentsByPinId(pinId: Long) = attachmentDao.deleteAttachmentsByPinId(pinId)

    suspend fun deleteAttachmentById(attachmentId: Long) = attachmentDao.deleteAttachmentById(attachmentId)
}