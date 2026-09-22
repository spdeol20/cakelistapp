package com.example.cakelistapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cakelistapp.ui.cakes.CakeListRoute
import com.example.cakelistapp.ui.theme.CakelistappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CakelistappTheme {
                CakeListRoute()
            }
        }
    }
}
