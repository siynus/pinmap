package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.MediaFileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val mCategoryRepository: CategoryRepository,
    private val mDatabase: PinmapDatabase
) : ViewModel() {

    private val mPinRepository = PinRepository(mDatabase.pinStore())
    private val mTemplateRepository = FieldTemplateRepository(mDatabase.fieldTemplateStore())
    private val mValueRepository = FieldValueRepository(mDatabase.fieldValueStore())

    private val mCategories = MutableStateFlow<List<Category>>(emptyList())
    val _categories: StateFlow<List<Category>> = mCategories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            mCategoryRepository.getAllCategories().collect { categoryList ->
                mCategories.value = categoryList
            }
        }
    }

    fun createCategory(name: String, color: Int) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                color = color
            )
            mCategoryRepository.insertCategory(category)
        }
    }

    fun deleteCategory(category: Category, context: android.content.Context) {
        viewModelScope.launch {
            val refs = mDatabase.withTransaction {
                val pins = mPinRepository.getPinsByCategory(category.id).first()
                val allRefs = mutableListOf<String>()
                pins.forEach { pin ->
                    pin.avatarPath?.let { allRefs.add(it) }
                    mValueRepository.getFieldValuesByPin(pin.id).first().forEach { fv ->
                        fv.value?.let { allRefs.add(it) }
                    }
                    mValueRepository.deleteFieldValuesByPin(pin.id)
                    mPinRepository.deletePinById(pin.id)
                }
                val templates = mTemplateRepository.getFieldTemplatesByCategory(category.id).first()
                templates.forEach { template ->
                    mValueRepository.deleteFieldValuesByTemplate(template.id)
                    mTemplateRepository.deleteFieldTemplateById(template.id)
                }
                mCategoryRepository.deleteCategory(category)
                allRefs
            }
            if (refs.isNotEmpty()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    MediaFileHelper.deleteFiles(context, refs)
                }
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            mCategoryRepository.updateCategory(category)
        }
    }
}
