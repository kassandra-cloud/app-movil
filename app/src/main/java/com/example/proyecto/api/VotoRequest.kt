package com.example.proyecto.api

import com.squareup.moshi.JsonClass

/**
 * Clase que representa el cuerpo (body) de la solicitud para registrar un voto.
 * * Se requiere la anotación @JsonClass(generateAdapter = true) para que Moshi,
 * junto con el generador de código (moshi-kotlin-codegen), pueda crear
 * automáticamente el adaptador necesario para la conversión a JSON.
 */
@JsonClass(generateAdapter = true)
data class VotoRequest(val opcion_id: Int)