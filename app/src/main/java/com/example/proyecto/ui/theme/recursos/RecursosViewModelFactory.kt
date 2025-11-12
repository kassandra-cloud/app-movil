// app/src/main/java/com/example/proyecto/ui/recursos/RecursosViewModelFactory.kt
package com.example.proyecto.ui.recursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RecursosViewModelFactory(private val token: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RecursosViewModel::class.java))
        return RecursosViewModel(token) as T
    }
}
