package com.example.gestor_colecciones.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestor_colecciones.entities.Persona
import com.example.gestor_colecciones.repository.PersonaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PersonaViewModel(private val repository: PersonaRepository) : ViewModel() {

    val personas: StateFlow<List<Persona>> =
        repository.allPersonas.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun insert(persona: Persona) {
        viewModelScope.launch {
            repository.insert(persona)
        }
    }

    fun update(persona: Persona) {
        viewModelScope.launch {
            repository.update(persona)
        }
    }

    fun delete(persona: Persona) {
        viewModelScope.launch {
            repository.delete(persona)
        }
    }
}