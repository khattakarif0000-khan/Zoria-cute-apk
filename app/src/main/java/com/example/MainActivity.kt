package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.theme.ZoriaTheme
import com.example.zoria.ui.ZoriaScreen
import com.example.zoria.ui.ZoriaViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: ZoriaViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ZoriaTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          ZoriaScreen(viewModel = viewModel)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.checkPermissions()
  }

  override fun onPause() {
    super.onPause()
    // Gracefully stop audio playback and listening when app is paused
    viewModel.stopAll()
  }
}

