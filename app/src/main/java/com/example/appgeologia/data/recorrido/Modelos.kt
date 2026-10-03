package com.example.appgeologia.data.recorrido

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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

/**
 * Punto del recorrido: guarda la hora, la posición y los datos generales del sitio.
 * Lo observado en el lugar se registra como [items].
 */
@OptIn(ExperimentalSerializationApi::class)
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
    // "horizontes" es el nombre que tenía en la primera versión de la app.
    @JsonNames("horizontes") val items: List<ItemParada> = emptyList(),
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
 * Una observación hecha en la parada: título, descripción, fotos y, si corresponde,
 * los datos de la planilla de suelos. Cada item con datos de suelo es una columna de
 * la tabla del PDF.
 *
 * Los campos con opciones fijas guardan el código tal cual aparece en la planilla
 * (por ejemplo "MA" o "NoEf"); ver [OpcionesSuelo].
 */
@Serializable
data class ItemParada(
    val id: Long,
    val titulo: String = "",
    val descripcion: String = "",
    /** Nombres de archivo dentro de la carpeta de fotos de la app (ver AlmacenFotos). */
    val fotos: List<String> = emptyList(),
    val fecha: Long = 0,
    // ----- Datos de la planilla de suelos -----
    /** Nombre del horizonte (A, Bt1, C...). */
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

/** true si el item tiene cargado algún dato de la planilla de suelos. */
fun ItemParada.tieneDatosSuelo(): Boolean =
    copy(id = 0, titulo = "", descripcion = "", fotos = emptyList(), fecha = 0) != ItemParada(id = 0)
