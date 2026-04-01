package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 标记详情页面 ViewModel
 */
class PinDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val pinRepository: PinRepository,
    private val fieldTemplateRepository: FieldTemplateRepository,
    private val fieldValueRepository: FieldValueRepository
) : ViewModel() {

    private val pinId: Long = checkNotNull(savedStateHandle["pinId"])

    private val _pin = MutableStateFlow<Pin?>(null)
    val pin: StateFlow<Pin?> = _pin.asStateFlow()

    // 字段值
    private val _fieldValues = MutableStateFlow<Map<Long, FieldValue>>(emptyMap())
    val fieldValues: StateFlow<Map<Long, FieldValue>> = _fieldValues.asStateFlow()

    // 用于刷新字段模板的触发器
    private val _refreshFieldTemplates = MutableStateFlow(0)

    // 所有字段模板（当前类别的模板字段 + 当前标记的自定义字段）
    val fieldTemplates: StateFlow<List<FieldTemplate>> = combine(
        _pin,
        _fieldValues,
        _refreshFieldTemplates
    ) { pin, fieldValues, _ ->
        if (pin == null) {
            emptyList()
        } else {
            // 获取该标记所属类别的所有模板字段
            val categoryTemplates = fieldTemplateRepository.getTemplateFieldsByCategory(pin.categoryId ?: 0L).first()
            
            // 获取当前标记的自定义字段（通过 FieldValue 关联）
            val customFieldTemplateIds = fieldValues.keys
            val customTemplates = customFieldTemplateIds.mapNotNull { id ->
                runBlocking { fieldTemplateRepository.getFieldTemplateById(id) }
            }.filter { it.categoryId == null } // 只保留自定义字段（categoryId 为 null）
            
            // 合并并去重
            val allTemplates = categoryTemplates + customTemplates
            allTemplates.distinctBy { it.id }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadPin()
        loadFieldValues()
    }

    private fun loadPin() {
        viewModelScope.launch {
            val pinData = pinRepository.getPinById(pinId)
            _pin.value = pinData
        }
    }

    private fun loadFieldValues() {
        viewModelScope.launch {
            fieldValueRepository.getFieldValuesByPin(pinId).collect { values ->
                _fieldValues.value = values.associateBy { it.fieldTemplateId }
            }
        }
    }

    fun createFieldTemplate(fieldName: String, fieldType: com.sinus.pinmap.data.entity.FieldType, isTemplate: Boolean) {
        viewModelScope.launch {
            val categoryId = _pin.value?.categoryId
            val fieldTemplate = FieldTemplate(
                fieldName = fieldName,
                fieldType = fieldType,
                isTemplate = isTemplate,
                categoryId = if (isTemplate) categoryId else null
            )
            fieldTemplateRepository.insertFieldTemplate(fieldTemplate)
            _refreshFieldTemplates.value++
        }
    }

    fun updateFieldValue(fieldTemplateId: Long, value: String?) {
        viewModelScope.launch {
            val existingValue = _fieldValues.value[fieldTemplateId]
            if (existingValue != null) {
                fieldValueRepository.updateFieldValue(
                    existingValue.copy(value = value)
                )
            } else {
                fieldValueRepository.insertFieldValue(
                    FieldValue(
                        pinId = pinId,
                        fieldTemplateId = fieldTemplateId,
                        value = value
                    )
                )
            }
        }
    }

    fun deleteFieldTemplate(fieldTemplate: FieldTemplate, keepFieldValues: Boolean = false) {
        viewModelScope.launch {
            if (keepFieldValues) {
                // 保留字段值，但将该字段从模板中移除（isTemplate 设为 false）
                fieldTemplateRepository.updateFieldTemplate(
                    fieldTemplate.copy(isTemplate = false)
                )
            } else {
                // 删除字段模板和所有相关字段值
                fieldTemplateRepository.deleteFieldTemplate(fieldTemplate)
                // 删除所有使用此模板的字段值
                fieldValueRepository.deleteFieldValuesByTemplate(fieldTemplate.id)
            }
            _refreshFieldTemplates.value++
        }
    }
}