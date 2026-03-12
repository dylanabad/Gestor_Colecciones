package com.example.gestor_colecciones.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Tag")
data class Tag(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String
)