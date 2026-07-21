package com.lomigoo.classworkmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lomigoo.classworkmanager.data.AppPreferences
import com.lomigoo.classworkmanager.data.Classwork
import com.lomigoo.classworkmanager.data.ClassworkDbHelper
import com.lomigoo.classworkmanager.data.UpdateInfo
import com.lomigoo.classworkmanager.data.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class ClassworkViewModel(
    private val dbHelper: ClassworkDbHelper,
    private val updateManager: UpdateManager,
    private val appPreferences: AppPreferences,
) : ViewModel() {
    private val _classworks = MutableStateFlow<List<Classwork>>(emptyList())
    val classworks: StateFlow<List<Classwork>> = _classworks.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(value = false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    private val _isDownloading = MutableStateFlow(value = false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isDarkMode = MutableStateFlow(appPreferences.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _dateFormat = MutableStateFlow(appPreferences.getDateFormat())
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _whatsNewInfo = MutableStateFlow<UpdateInfo?>(null)
    val whatsNewInfo: StateFlow<UpdateInfo?> = _whatsNewInfo.asStateFlow()

    val currentVersion: String = updateManager.getCurrentVersion()

    init {
        loadData()
        checkForUpdates()
    }

    fun loadData() {
        viewModelScope.launch {
            _classworks.value = dbHelper.getAllClasswork()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                _isCheckingUpdates.value = true
                val result = updateManager.checkForUpdates()
                _updateInfo.value = result
                // Auto pop-up trigger removed as requested.
                // It will only show via the floating notice icon if versions match.
            } catch (e: Exception) {
                // Global safety: catch any unexpected network or parsing issues
            } finally {
                _isCheckingUpdates.value = false
            }
        }
    }

    fun dismissWhatsNew() {
        _whatsNewInfo.value?.let { info ->
            updateManager.markVersionAsSeen(info.latestVersion)
        }
        _whatsNewInfo.value = null
    }

    fun showWhatsNew() {
        _updateInfo.value?.let { info ->
            if (info.isWhatsNewAvailable) {
                _whatsNewInfo.value = info
            }
        }
    }

    fun startDownloadApk(url: String) {
        viewModelScope.launch {
            _isDownloading.value = true
            updateManager.downloadAndInstallApk(url).collect { progress ->
                if (progress >= 100) {
                    _isDownloading.value = false
                }
            }
        }
    }

    fun triggerNotificationTest() {
        updateManager.triggerNotificationTest()
    }

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        appPreferences.setDarkMode(newValue)
    }

    fun setDateFormat(format: String) {
        _dateFormat.value = format
        appPreferences.setDateFormat(format)
    }

    fun createClasswork(course: String, action: String, target: String) {
        viewModelScope.launch {
            val dateCreated = LocalDate.now(ZoneId.of("Asia/Manila")).toString()
            dbHelper.insertClasswork(course, action, dateCreated, target)
            loadData()
        }
    }

    fun updateClasswork(classwork: Classwork) {
        viewModelScope.launch {
            dbHelper.updateClasswork(classwork)
            loadData()
        }
    }

    fun deleteClasswork(classwork: Classwork) {
        viewModelScope.launch {
            dbHelper.deleteClasswork(classwork.id)
            loadData()
        }
    }
}

class ClassworkViewModelFactory(
    private val dbHelper: ClassworkDbHelper,
    private val updateManager: UpdateManager,
    private val appPreferences: AppPreferences,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassworkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClassworkViewModel(dbHelper, updateManager, appPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
