package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldType
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PinDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val pinRepository: PinRepository,
    private val fieldTemplateRepository: FieldTemplateRepository,
    private val fieldValueRepository: FieldValueRepository
) : ViewModel() {

    private val pinId: Long = checkNotNull(savedStateHandle["pinId"])

    private val _pin = MutableStateFlow<Pin?>(null)
    val pin: StateFlow<Pin?> = _pin.asStateFlow()

    private val _fieldValues = MutableStateFlow<Map<Long, FieldValue>>(emptyMap())
    val fieldValues: StateFlow<Map<Long, FieldValue>> = _fieldValues.asStateFlow()

    private val _refresh = MutableStateFlow(0)

    val fieldTemplates: StateFlow<List<FieldTemplate>> = combine(
        _pin,
        _refresh
    ) { pin, _ ->
        if (pin == null) return@combine emptyList()
        val categoryId = pin.categoryId ?: return@combine emptyList()
        val categoryTemplates = fieldTemplateRepository
            .getFieldTemplatesByCategory(categoryId)
            .first()
        categoryTemplates
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    init {
        loadPin()
        loadFieldValues()
    }

    private fun loadPin() {
        viewModelScope.launch {
            pinRepository.getPinById(pinId)?.let { _pin.value = it }
        }
    }

    private fun loadFieldValues() {
        viewModelScope.launch {
            fieldValueRepository.getFieldValuesByPin(pinId).collect { values ->
                _fieldValues.value = values.associateBy { it.fieldTemplateId ?: it.id }
            }
        }
    }

    fun createTemplateField(fieldName: String, fieldType: FieldType) {
        viewModelScope.launch {
            val categoryId = _pin.value?.categoryId ?: return@launch
            val template = FieldTemplate(
                categoryId = categoryId,
                fieldName = fieldName,
                fieldType = fieldType
            )
            val templateId = fieldTemplateRepository.insertFieldTemplate(template)
            fieldValueRepository.insertFieldValue(
                FieldValue(pinId = pinId, fieldTemplateId = templateId)
            )
            _refresh.value++
        }
    }

    fun createIndependentField(fieldName: String, fieldType: FieldType) {
        viewModelScope.launch {
            fieldValueRepository.insertFieldValue(
                FieldValue(
                    pinId = pinId,
                    fieldTemplateId = null,
                    fieldName = fieldName,
                    fieldType = fieldType
                )
            )
            _refresh.value++
        }
    }

    fun updateFieldValue(key: Long, value: String?) {
        viewModelScope.launch {
            val existing = _fieldValues.value[key]
            if (existing != null) {
                fieldValueRepository.updateFieldValue(existing.copy(value = value))
            }
        }
    }

    fun deleteFieldValue(key: Long) {
        viewModelScope.launch {
            val fieldValue = _fieldValues.value[key]
            if (fieldValue == null) return@launch

            val templateId = fieldValue.fieldTemplateId
            fieldValueRepository.deleteFieldValueById(fieldValue.id)

            if (templateId != null) {
                val remaining = fieldValueRepository.getFieldValuesByTemplate(templateId).first()
                if (remaining.isEmpty()) {
                    fieldTemplateRepository.deleteFieldTemplateById(templateId)
                }
            }
            _refresh.value++
        }
    }
}
