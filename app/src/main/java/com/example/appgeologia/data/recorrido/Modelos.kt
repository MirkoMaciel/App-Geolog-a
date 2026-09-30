package com.example.appgeologia.data.recorrido

import kotlinx.serialization.Serializable

/**
 * Materia sobre la que se toman las anotaciones en la salida de campo.
 * Por ahora solo existe "Suelos"; el enum permite sumar otras más adelante.
 */
@Serializable
enum class TipoSalida(val nombre: String) {
    SUELOS("Suelos")
}

/** Una salida de campo: un conjunto ordenado de paradas. */
@Serializable
data class Recorrido(
    val id: Long,
    val nombre: String,
    val tipoSalida: TipoSalida = TipoSalida.SUELOS,
    val proyecto: String = "",
    val descripcion: String = "",
    val fechaInicio: Long,
    val finalizado: Boolean = false,
    val paradas: List<Parada> = emptyList()
)

/** Punto del recorrido donde se completa la planilla de suelos. */
@Serializable
data class Parada(
    val id: Long,
    val numero: Int,
    val fecha: Long,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val altitud: Double? = null,
    /** Precisión del GPS en metros al momento de tomar la posición (null si se cargó a mano). */
    val precisionMetros: Float? = null,
    val sitio: DatosSitio = DatosSitio(),
    val horizontes: List<Horizonte> = emptyList(),
    val observaciones: String = ""
)

/** Encabezado de la planilla: datos generales del sitio de la calicata. */
@Serializable
data class DatosSitio(
    val calicata: String = "",
    val sitioReferencia: String = "",
    val proyecto: String = "",
    val geomorfologia: String = "",
    val posicionGeomorfica: String = "",
    val relieve: String = "",
    val exposicion: String = "",
    val materialOriginario: String = "",
    val pendiente: String = "",
    val vegetacion: String = "",
    val coberturaVegetal: String = "",
    val claseDrenaje: String = "",
    val anegamiento: String = "",
    val erosion: String = "",
    val salesAlcalis: String = "",
    val usoTierra: String = "",
    val pedregosidad: String = "",
    val clasificacion: String = "",
    val temperatura: String = ""
)

/**
 * Una columna de la tabla de la planilla: la descripción de un horizonte del perfil.
 * Los campos con opciones fijas guardan el código tal cual aparece en la planilla
 * (por ejemplo "MA" o "NoEf"); ver [OpcionesSuelo].
 */
@Serializable
data class Horizonte(
    val id: Long,
    val nombre: String = "",
    val profundidad: String = "",
    // Límite
    val limiteDefinicion: String = "",
    val limiteTopografia: String = "",
    // Color (notación Munsell, ej. 10YR 4/3)
    val colorSeco: String = "",
    val colorHumedo: String = "",
    val textura: String = "",
    // Consistencia
    val resistencia: String = "",
    val adhesividad: String = "",
    val plasticidad: String = "",
    // Estructura
    val estructuraTipo: String = "",
    val estructuraGrado: String = "",
    val estructuraTamano: String = "",
    val reaccionHcl: String = "",
    // Concentraciones
    val concentracionTipo: String = "",
    val concentracionTamano: String = "",
    val concentracionColor: String = "",
    val concentracionCantidad: String = "",
    val concentracionForma: String = "",
    val rasgosRedox: String = "",
    val moteados: String = "",
    val barnices: String = "",
    val humedad: String = "",
    // Raíces
    val raicesCantidad: String = "",
    val raicesTamano: String = "",
    val raicesUbicacion: String = "",
    // Fragmentos gruesos
    val fragmentosTipo: String = "",
    val fragmentosVolumen: String = "",
    val fragmentosRedondez: String = "",
    val fragmentosTamano: String = ""
)
