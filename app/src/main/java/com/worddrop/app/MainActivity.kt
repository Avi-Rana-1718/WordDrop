package com.worddrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.worddrop.app.ui.navigation.WordDropNavHost
import com.worddrop.app.ui.theme.WordDropTheme
import com.worddrop.app.ui.theme.wd
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WordDropTheme {
                Surface(modifier = Modifier.fillMaxSize().background(wd.paper), color = wd.paper) {
                    WordDropNavHost()
                }
            }
        }
    }
}
