package com.lomigoo.classworkmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lomigoo.classworkmanager.data.Classwork
import com.lomigoo.classworkmanager.data.ClassworkDbHelper
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
) : ViewModel() {
    private val _classworks = MutableStateFlow<List<Classwork>>(emptyList())
    val classworks: StateFlow<List<Classwork>> = _classworks.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateManager.UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateManager.UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(value = false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    private val _isDownloading = MutableStateFlow(value = false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

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
            _isCheckingUpdates.value = true
            _updateInfo.value = updateManager.checkForUpdates()
            _isCheckingUpdates.value = false
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
    private val updateManager: UpdateManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassworkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClassworkViewModel(dbHelper, updateManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
