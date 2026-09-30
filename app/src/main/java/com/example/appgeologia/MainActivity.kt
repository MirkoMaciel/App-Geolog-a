package com.example.appgeologia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appgeologia.ui.AppGeologia
import com.example.appgeologia.ui.theme.AppGeologiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppGeologiaTheme {
                AppGeologia()
            }
        }
    }
}
