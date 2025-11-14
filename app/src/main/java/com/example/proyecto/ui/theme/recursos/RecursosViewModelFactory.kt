// app/src/main/java/com/example/proyecto/ui/theme/recursos/RecursosViewModelFactory.kt
package com.example.proyecto.ui.theme.recursos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.proyecto.api.RecursosApi

class RecursosViewModelFactory(private val recursosApi: RecursosApi) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RecursosViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        // Pasa el objeto RecursosApi al constructor del ViewModel
        return RecursosViewModel(recursosApi) as T
    }
}