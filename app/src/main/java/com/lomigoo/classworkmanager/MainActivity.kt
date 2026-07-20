package com.lomigoo.classworkmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import com.lomigoo.classworkmanager.data.ClassworkDbHelper
import com.lomigoo.classworkmanager.data.UpdateManager
import com.lomigoo.classworkmanager.ui.ClassworkApp
import com.lomigoo.classworkmanager.ui.ClassworkViewModel
import com.lomigoo.classworkmanager.ui.ClassworkViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbHelper = ClassworkDbHelper(applicationContext)
        val updateManager = UpdateManager(applicationContext)
        val factory = ClassworkViewModelFactory(dbHelper, updateManager)
        val viewModel = ViewModelProvider(this, factory).get(ClassworkViewModel::class.java)

        setContent {
            MaterialTheme {
                ClassworkApp(viewModel)
            }
        }
    }
}
