package com.lomigoo.classworkmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.lomigoo.classworkmanager.data.ClassworkDbHelper
import com.lomigoo.classworkmanager.data.ThemePreference
import com.lomigoo.classworkmanager.data.UpdateManager
import com.lomigoo.classworkmanager.ui.ClassworkApp
import com.lomigoo.classworkmanager.ui.ClassworkViewModel
import com.lomigoo.classworkmanager.ui.ClassworkViewModelFactory
import com.lomigoo.classworkmanager.ui.theme.ClassworkManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbHelper = ClassworkDbHelper(applicationContext)
        val updateManager = UpdateManager(applicationContext)
        val themePreference = ThemePreference(applicationContext)
        val factory = ClassworkViewModelFactory(dbHelper, updateManager, themePreference)
        val viewModel = ViewModelProvider(this, factory).get(ClassworkViewModel::class.java)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            ClassworkManagerTheme(darkTheme = isDarkMode) {
                ClassworkApp(viewModel)
            }
        }
    }
}
