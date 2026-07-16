package com.sinus.pinmap.ui.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object AuthState {
    private const val PREFS_NAME = "amap_auth"
    private const val KEY_API = "amap_api_key"

    var mAuthFailed = false
        private set

    private lateinit var mPrefs: SharedPreferences

    fun init(context: Context) {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun markFailed() {
        mAuthFailed = true
    }

    fun markSuccess() {
        mAuthFailed = false
    }

    fun getSavedKey(): String? = if (::mPrefs.isInitialized) mPrefs.getString(KEY_API, null) else null

    fun saveKey(key: String) {
        if (::mPrefs.isInitialized) {
            mPrefs.edit { putString(KEY_API, key) }
        }
    }
}
