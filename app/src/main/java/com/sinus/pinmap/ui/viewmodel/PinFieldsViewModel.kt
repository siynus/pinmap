package com.sinus.pinmap.ui.viewmodel

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

class PinFieldsViewModel(
    private val pinId: Long,
    private val pinRepository: PinRepository,
    private val fieldTemplateRepository: FieldTemplateRepository,
    private val fieldValueRepository: FieldValueRepository
) : ViewModel() {

    private val _pin = MutableStateFlow<Pin?>(null)
    val pin: StateFlow<Pin?> = _pin.asStateFlow()

    private val _fieldValues = MutableStateFlow<Map<Long, FieldValue>>(emptyMap())
    val fieldValues: StateFlow<Map<Long, FieldValue>> = _fieldValues.asStateFlow()

    private val _templates = MutableStateFlow<List<FieldTemplate>>(emptyList())
    val templates: StateFlow<List<FieldTemplate>> = _templates.asStateFlow()

    private val _refresh = MutableStateFlow(0)

    init {
        loadPin()
        loadFields()
    }

    private fun loadPin() {
        viewModelScope.launch {
            val p = pinRepository.getPinById(pinId) ?: return@launch
            val cid = p.categoryId ?: return@launch
            _pin.value = p
            _templates.value = fieldTemplateRepository.getFieldTemplatesByCategory(cid).first()
        }
    }

    private fun loadFields() {
        viewModelScope.launch {
            fieldValueRepository.getFieldValuesByPin(pinId).collect { values ->
                _fieldValues.value = values.associateBy { it.fieldTemplateId ?: it.id }
            }
        }
    }

    fun addField(fieldName: String, fieldType: FieldType) {
        viewModelScope.launch {
            val pin = _pin.value ?: return@launch
            val cid = pin.categoryId ?: return@launch
            val template = FieldTemplate(
                categoryId = cid,
                fieldName = fieldName,
                fieldType = fieldType
            )
            val templateId = fieldTemplateRepository.insertFieldTemplate(template)
            fieldValueRepository.insertFieldValue(
                FieldValue(pinId = pinId, fieldTemplateId = templateId)
            )
            _templates.value = fieldTemplateRepository.getFieldTemplatesByCategory(cid).first()
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

    fun deleteField(key: Long) {
        viewModelScope.launch {
            val fieldValue = _fieldValues.value[key] ?: return@launch
            val templateId = fieldValue.fieldTemplateId
            fieldValueRepository.deleteFieldValueById(fieldValue.id)
            if (templateId != null) {
                fieldTemplateRepository.deleteFieldTemplateById(templateId)
            }
            val pin = _pin.value ?: return@launch
            val cid = pin.categoryId ?: return@launch
            _templates.value = fieldTemplateRepository.getFieldTemplatesByCategory(cid).first()
            _refresh.value++
        }
    }
}
