package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.model.FieldData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val mPinRepository: PinRepository,
    private val mFieldTemplateRepository: FieldTemplateRepository,
    private val mFieldValueRepository: FieldValueRepository
) : ViewModel() {

    private val mPins = MutableStateFlow<List<Pin>>(emptyList())
    val _pins: StateFlow<List<Pin>> = mPins.asStateFlow()

    private val mSelectedPin = MutableStateFlow<Pin?>(null)
    val _selectedPin: StateFlow<Pin?> = mSelectedPin.asStateFlow()

    init {
        loadPins()
    }

    private fun loadPins() {
        viewModelScope.launch {
            mPinRepository.getAllPins().collect { pinList ->
                mPins.value = pinList
            }
        }
    }

    fun selectPin(pin: Pin) {
        mSelectedPin.value = pin
    }

    fun clearSelectedPin() {
        mSelectedPin.value = null
    }

    suspend fun getPinById(pinId: Long): Pin? {
        return mPinRepository.getPinById(pinId)
    }

    fun createPin(
        latitude: Double,
        longitude: Double,
        title: String,
        categoryId: Long,
        fields: List<FieldData> = emptyList()
    ) {
        viewModelScope.launch {
            val pin = Pin(
                latitude = latitude,
                longitude = longitude,
                title = title,
                categoryId = categoryId
            )
            val pinId = mPinRepository.insertPin(pin)

            fields.forEach { fieldData ->
                val order = mFieldTemplateRepository.nextSortOrder(categoryId)
                val template = FieldTemplate(
                    categoryId = categoryId,
                    fieldName = fieldData.name,
                    fieldType = fieldData.type,
                    sortOrder = order
                )
                val templateId = mFieldTemplateRepository.insertFieldTemplate(template)
                mFieldValueRepository.insertFieldValue(
                    FieldValue(pinId = pinId, fieldTemplateId = templateId, value = fieldData.value)
                )
            }
        }
    }

    fun updatePin(pin: Pin) {
        viewModelScope.launch {
            mPinRepository.updatePin(pin)
        }
    }

    fun deletePin(pin: Pin) {
        viewModelScope.launch {
            mPinRepository.deletePin(pin)
            clearSelectedPin()
        }
    }
}
