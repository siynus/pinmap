package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.haversineDistance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortMode {
    NAME_ASC, NAME_DESC, DISTANCE_ASC, CREATED_DESC
}

class PinListViewModel(
    private val mPinRepository: PinRepository,
    private val mCategoryRepository: CategoryRepository
) : ViewModel() {

    private val _pins = MutableStateFlow<List<Pin>>(emptyList())
    val pins: StateFlow<List<Pin>> = _pins.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.CREATED_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private var _currentLat = 39.9042
    private var _currentLng = 116.4074
    val currentLat: Double get() = _currentLat
    val currentLng: Double get() = _currentLng

    val filteredPins: StateFlow<List<Pin>> = combine(
        _pins,
        _searchQuery,
        _selectedCategoryId,
        _sortMode
    ) { pins, query, categoryId, sort ->
        var result = pins

        if (categoryId != null) {
            result = result.filter { it.categoryId == categoryId }
        }

        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                        (it.description?.contains(query, ignoreCase = true) == true)
            }
        }

        val lat = _currentLat
        val lng = _currentLng
        when (sort) {
            SortMode.NAME_ASC -> result = result.sortedBy { it.title }
            SortMode.NAME_DESC -> result = result.sortedByDescending { it.title }
            SortMode.DISTANCE_ASC -> result = result.sortedBy {
                haversineDistance(lat, lng, it.latitude, it.longitude)
            }

            SortMode.CREATED_DESC -> result = result.sortedByDescending { it.createdAt }
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
            mPinRepository.getAllPins().collect { pinList ->
                _pins.value = pinList
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            mCategoryRepository.getAllCategories().collect { categoryList ->
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

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun setCurrentLocation(lat: Double, lng: Double) {
        _currentLat = lat
        _currentLng = lng
    }

    fun deletePin(pin: Pin) {
        viewModelScope.launch {
            mPinRepository.deletePin(pin)
        }
    }

    suspend fun getPinById(pinId: Long): Pin? {
        return mPinRepository.getPinById(pinId)
    }
}
