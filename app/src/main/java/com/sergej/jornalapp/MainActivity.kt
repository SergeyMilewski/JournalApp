package com.sergej.jornalapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sergej.jornalapp.presentation.journal.VideoJournalScreen
import com.sergej.jornalapp.ui.theme.JornalAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JornalAppTheme {
                VideoJournalScreen()
            }
        }
    }
}
