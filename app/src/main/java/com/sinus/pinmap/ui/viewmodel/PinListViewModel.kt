package com.sinus.pinmap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.LocationManager
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

    private val mPins = MutableStateFlow<List<Pin>>(emptyList())
    val _pins: StateFlow<List<Pin>> = mPins.asStateFlow()

    private val mCategories = MutableStateFlow<List<Category>>(emptyList())
    val _categories: StateFlow<List<Category>> = mCategories.asStateFlow()

    private val mSearchQuery = MutableStateFlow("")
    val _searchQuery: StateFlow<String> = mSearchQuery.asStateFlow()

    private val mSelectedCategoryId = MutableStateFlow<Long?>(null)
    val _selectedCategoryId: StateFlow<Long?> = mSelectedCategoryId.asStateFlow()

    private val mSortMode = MutableStateFlow(SortMode.CREATED_DESC)
    val _sortMode: StateFlow<SortMode> = mSortMode.asStateFlow()

    private var mCurrentLat = LocationManager.DEFAULT_LATITUDE
    private var mCurrentLng = LocationManager.DEFAULT_LONGITUDE
    val _currentLat: Double get() = mCurrentLat
    val _currentLng: Double get() = mCurrentLng

    val filteredPins: StateFlow<List<Pin>> = combine(
        mPins,
        mSearchQuery,
        mSelectedCategoryId,
        mSortMode
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

        val lat = mCurrentLat
        val lng = mCurrentLng
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
                mPins.value = pinList
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            mCategoryRepository.getAllCategories().collect { categoryList ->
                mCategories.value = categoryList
            }
        }
    }

    fun setSearchQuery(query: String) {
        mSearchQuery.value = query
    }

    fun setSelectedCategory(categoryId: Long?) {
        mSelectedCategoryId.value = categoryId
    }

    fun setSortMode(mode: SortMode) {
        mSortMode.value = mode
    }

    fun setCurrentLocation(lat: Double, lng: Double) {
        mCurrentLat = lat
        mCurrentLng = lng
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
