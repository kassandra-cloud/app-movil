package com.example.proyecto.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ThemeViewModel : ViewModel() {
    // Estado para el modo oscuro (false = claro, true = oscuro)
    var isDarkMode by mutableStateOf(false)
        private set

    // Estado para el tamaño de letra (1.0f = normal, 1.5f = 50% más grande)
    var fontScale by mutableStateOf(1.0f)
        private set

    fun toggleTheme(isDark: Boolean) {
        isDarkMode = isDark
    }

    fun changeFontScale(scale: Float) {
        fontScale = scale
    }
    fun setInitialTheme(dark: Boolean, scale: Float) {
        isDarkMode = dark
        fontScale = scale.coerceIn(0.8f, 1.4f)
    }
}