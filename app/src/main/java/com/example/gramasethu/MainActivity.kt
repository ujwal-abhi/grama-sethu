package com.example.gramasethu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.gramasethu.ui.navigation.AppNavigation
import com.example.gramasethu.ui.theme.GramaSethuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GramaSethuTheme {
                AppNavigation()
            }
        }
    }
}