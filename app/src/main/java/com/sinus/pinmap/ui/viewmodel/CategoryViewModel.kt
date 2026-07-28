package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val mCategoryRepository: CategoryRepository
) : ViewModel() {

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

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            mCategoryRepository.deleteCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            mCategoryRepository.updateCategory(category)
        }
    }
}
