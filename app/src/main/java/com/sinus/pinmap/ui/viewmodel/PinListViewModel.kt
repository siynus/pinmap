package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.PinRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 标记列表页面 ViewModel
 */
class PinListViewModel(
    private val pinRepository: PinRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // 所有标记
    private val _pins = MutableStateFlow<List<Pin>>(emptyList())
    val pins: StateFlow<List<Pin>> = _pins.asStateFlow()

    // 所有分类
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 选中的分类ID（null表示所有分类）
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // 过滤后的标记
    val filteredPins: StateFlow<List<Pin>> = combine(
        _pins,
        _searchQuery,
        _selectedCategoryId
    ) { pins, query, categoryId ->
        var result = pins

        // 按分类筛选
        if (categoryId != null) {
            result = result.filter { it.categoryId == categoryId }
        }

        // 按搜索关键词筛选
        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                (it.description?.contains(query, ignoreCase = true) == true)
            }
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadPins()
        loadCategories()
    }

    private fun loadPins() {
        viewModelScope.launch {
            pinRepository.getAllPins().collect { pinList ->
                _pins.value = pinList
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categoryList ->
                _categories.value = categoryList
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun deletePin(pin: Pin) {
        viewModelScope.launch {
            pinRepository.deletePin(pin)
        }
    }

    suspend fun getPinById(pinId: Long): Pin? {
        return pinRepository.getPinById(pinId)
    }
}