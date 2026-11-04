package com.example.appgeologia.archivos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** De qué salida es un PDF exportado y qué contiene. */
@Serializable
data class DatosExportacion(
    val recorridoId: Long,
    /** Nombre de la salida al momento de exportar (se muestra aunque la salida se borre). */
    val nombreSalida: String,
    /** "Parada 3" o "Salida completa". */
    val contenido: String
)

/** Un PDF de la carpeta de exportaciones, con sus datos si se conocen. */
data class Exportacion(val archivo: File, val datos: DatosExportacion?)

/**
 * Carpeta donde quedan guardados todos los PDF generados (pantalla "Mis exportaciones").
 *
 * Junto a los PDF se guarda un índice (indice.json) que dice a qué salida pertenece
 * cada archivo, para poder agruparlos. Los PDF exportados antes de que existiera el
 * índice aparecen sin salida asociada.
 */
class AlmacenExportaciones(context: Context) {

    val carpeta: File = File(context.filesDir, "exportaciones").apply { mkdirs() }
    private val archivoIndice = File(carpeta, "indice.json")
    private val json = Json { ignoreUnknownKeys = true }

    /** PDFs exportados, del más nuevo al más viejo. */
    fun listar(): List<File> =
        carpeta.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    /** Igual que [listar], pero con los datos de salida de cada PDF. */
    fun listarConDatos(): List<Exportacion> {
        val indice = leerIndice()
        return listar().map { Exportacion(it, indice[it.name]) }
    }

    /**
     * Archivo nuevo para un PDF de la salida indicada. Se le agrega la fecha y hora al
     * nombre para no pisar exportaciones anteriores, y se anota en el índice.
     */
    fun nuevoArchivo(nombre: String, datos: DatosExportacion): File {
        val limpio = nombre.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifEmpty { "planilla" }
        val sello = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
        val archivo = File(carpeta, "${limpio}_$sello.pdf")
        modificarIndice { it + (archivo.name to datos) }
        return archivo
    }

    fun eliminar(archivo: File) {
        archivo.delete()
        modificarIndice { it - archivo.name }
    }

    private fun leerIndice(): Map<String, DatosExportacion> =
        if (!archivoIndice.exists()) emptyMap()
        else runCatching { json.decodeFromString<Map<String, DatosExportacion>>(archivoIndice.readText()) }
            .getOrDefault(emptyMap())

    @Synchronized
    private fun modificarIndice(cambio: (Map<String, DatosExportacion>) -> Map<String, DatosExportacion>) {
        val anterior = leerIndice()
        // De paso se limpian las entradas de PDFs que ya no existen (la entrada recién
        // agregada se conserva aunque su PDF todavía se esté generando).
        val nuevo = cambio(anterior).filterKeys { nombre ->
            nombre !in anterior || File(carpeta, nombre).exists()
        }
        archivoIndice.writeText(json.encodeToString(nuevo))
    }
}

fun uriCompartible(context: Context, archivo: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

fun verPdf(context: Context, archivo: File) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uriCompartible(context, archivo), "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No hay una app para ver PDF instalada", Toast.LENGTH_LONG).show()
    }
}

fun compartirPdf(context: Context, archivo: File) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, uriCompartible(context, archivo))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
}
