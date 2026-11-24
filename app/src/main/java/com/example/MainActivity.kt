package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.navigation.RajeevDigitalApp
import com.example.util.NotificationHelper
import com.example.util.ReminderWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)
        ReminderWorker.schedulePeriodic(this)
        setContent {
            RajeevDigitalApp()
        }
    }
}
