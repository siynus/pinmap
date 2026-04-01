package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sinus.pinmap.data.repository.*

/**
 * ViewModel 工厂
 */
class ViewModelFactory(
    private val pinRepository: PinRepository,
    private val categoryRepository: CategoryRepository? = null,
    private val customFieldRepository: CustomFieldRepository? = null,
    private val attachmentRepository: AttachmentRepository? = null,
    private val offlineMapRepository: OfflineMapRepository? = null,
    private val fieldTemplateRepository: FieldTemplateRepository? = null,
    private val fieldValueRepository: FieldValueRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MapViewModel::class.java -> MapViewModel(
                pinRepository,
                fieldTemplateRepository ?: throw IllegalArgumentException("fieldTemplateRepository is required for MapViewModel"),
                fieldValueRepository ?: throw IllegalArgumentException("fieldValueRepository is required for MapViewModel")
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}