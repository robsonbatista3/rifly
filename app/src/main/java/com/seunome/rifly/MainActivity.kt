package com.seunome.rifly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.seunome.rifly.navigation.AppNavigation
import com.seunome.rifly.ui.theme.RiflyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RiflyTheme {
                AppNavigation()
            }
        }
    }
}
