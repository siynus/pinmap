package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.PinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 地图页面 ViewModel
 */
class MapViewModel(
    private val pinRepository: PinRepository,
    private val fieldTemplateRepository: com.sinus.pinmap.data.repository.FieldTemplateRepository,
    private val fieldValueRepository: com.sinus.pinmap.data.repository.FieldValueRepository
) : ViewModel() {

    private val _pins = MutableStateFlow<List<Pin>>(emptyList())
    val pins: StateFlow<List<Pin>> = _pins.asStateFlow()

    private val _selectedPin = MutableStateFlow<Pin?>(null)
    val selectedPin: StateFlow<Pin?> = _selectedPin.asStateFlow()

    init {
        loadPins()
    }

    private fun loadPins() {
        viewModelScope.launch {
            pinRepository.getAllPins().collect { pinList ->
                _pins.value = pinList
            }
        }
    }

    fun selectPin(pin: Pin) {
        _selectedPin.value = pin
    }

    fun clearSelectedPin() {
        _selectedPin.value = null
    }

    suspend fun getPinById(pinId: Long): Pin? {
        return pinRepository.getPinById(pinId)
    }

    fun createPin(
        latitude: Double,
        longitude: Double,
        title: String,
        categoryId: Long,
        fields: List<com.sinus.pinmap.ui.model.FieldData> = emptyList()
    ) {
        viewModelScope.launch {
            val pin = Pin(
                latitude = latitude,
                longitude = longitude,
                title = title,
                categoryId = categoryId
            )
            val pinId = pinRepository.insertPin(pin)

            // 创建字段模板和字段值
            fields.forEach { fieldData ->
                // 创建字段模板
                val fieldTemplate = com.sinus.pinmap.data.entity.FieldTemplate(
                    categoryId = if (fieldData.isTemplate) categoryId else null,
                    fieldName = fieldData.name,
                    fieldType = fieldData.type,
                    isTemplate = fieldData.isTemplate
                )
                val fieldTemplateId = fieldTemplateRepository.insertFieldTemplate(fieldTemplate)

                // 创建字段值
                val fieldValue = com.sinus.pinmap.data.entity.FieldValue(
                    pinId = pinId,
                    fieldTemplateId = fieldTemplateId,
                    value = fieldData.value.ifBlank { null }
                )
                fieldValueRepository.insertFieldValue(fieldValue)
            }
        }
    }

    fun updatePin(pin: Pin) {
        viewModelScope.launch {
            pinRepository.updatePin(pin)
        }
    }

    fun deletePin(pin: Pin) {
        viewModelScope.launch {
            pinRepository.deletePin(pin)
            clearSelectedPin()
        }
    }
}