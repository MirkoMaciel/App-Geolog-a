package com.example.appgeologia.archivos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** De qué salida (y parada) es un PDF exportado y qué contiene. */
@Serializable
data class DatosExportacion(
    val recorridoId: Long,
    /** Nombre de la salida al momento de exportar (se muestra aunque la salida se borre). */
    val nombreSalida: String,
    /** "Parada 3" o "Salida completa". */
    val contenido: String,
    /** Id de la parada; null = PDF de la salida completa (o exportación anterior a la v1.2). */
    val paradaId: Long? = null,
    /** true = copia guardada aparte; false = el PDF vigente de esa parada o salida. */
    val copia: Boolean = false
)

/** Un PDF de la carpeta de exportaciones, con sus datos si se conocen. */
data class Exportacion(val archivo: File, val datos: DatosExportacion?)

/**
 * Carpeta donde quedan los PDF generados (pantalla "Mis exportaciones").
 *
 * Cada parada y cada salida tiene **un solo PDF vigente**, con nombre fijo, dentro de la
 * carpeta de su salida (`exportaciones/<id salida>/Parada_2_Cerro_Chenque.pdf`). Volver a
 * exportar lo reemplaza; si se quiere conservar una versión, se guarda como copia aparte.
 *
 * Junto a los PDF hay un índice (indice.json) que dice a qué salida y parada pertenece
 * cada archivo. Las claves del índice son rutas relativas a la carpeta.
 */
class AlmacenExportaciones(val carpeta: File) {

    constructor(context: Context) : this(File(context.filesDir, "exportaciones"))

    private val archivoIndice = File(carpeta, "indice.json")
    private val json = Json { ignoreUnknownKeys = true }

    init {
        carpeta.mkdirs()
    }

    /** Todos los PDF (incluidas las subcarpetas de cada salida), del más nuevo al más viejo. */
    fun listar(): List<File> =
        carpeta.walkTopDown()
            .filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
            .sortedByDescending { it.lastModified() }
            .toList()

    /** Igual que [listar], pero con los datos de salida y parada de cada PDF. */
    fun listarConDatos(): List<Exportacion> {
        val indice = leerIndice()
        return listar().map { Exportacion(it, indice[relativo(it)]) }
    }

    /** El PDF vigente de una parada (o de la salida completa si [paradaId] es null), si existe. */
    @Synchronized
    fun vigente(recorridoId: Long, paradaId: Long?): Exportacion? =
        leerIndice().entries
            .filter { (_, d) ->
                d.recorridoId == recorridoId && d.paradaId == paradaId && !d.copia &&
                    (paradaId != null || d.contenido == "Salida completa")
            }
            .map { (ruta, d) -> Exportacion(File(carpeta, ruta), d) }
            .filter { it.archivo.exists() }
            .maxByOrNull { it.archivo.lastModified() }

    /**
     * Genera un PDF con [escribir]. Si no es [copia], reemplaza al PDF vigente anterior de
     * la misma parada o salida. El archivo nuevo se escribe aparte y recién al final
     * reemplaza al anterior: si algo falla, el anterior queda intacto.
     */
    @Synchronized
    fun exportar(datos: DatosExportacion, nombreBase: String, copia: Boolean, escribir: (File) -> Unit): File {
        val carpetaSalida = File(carpeta, datos.recorridoId.toString()).apply { mkdirs() }
        val nombre = if (copia) "${limpiar(nombreBase)}_copia_${sello()}.pdf" else "${limpiar(nombreBase)}.pdf"
        val destino = File(carpetaSalida, nombre)
        val temporal = File(carpetaSalida, "$nombre.tmp")

        escribir(temporal)

        if (!copia) {
            // El vigente anterior podría tener otro nombre (por ejemplo, si cambió el nombre de la salida).
            vigente(datos.recorridoId, datos.paradaId)?.archivo
                ?.takeIf { it.canonicalPath != destino.canonicalPath }
                ?.let { eliminar(it) }
        }
        destino.delete()
        if (!temporal.renameTo(destino)) {
            temporal.copyTo(destino, overwrite = true)
            temporal.delete()
        }
        modificarIndice { it + (relativo(destino) to datos.copy(copia = copia)) }
        return destino
    }

    /**
     * true si el PDF se generó después de la última modificación de su parada o salida
     * ([modificado]); en ese caso no hace falta volver a generarlo.
     */
    fun estaAlDia(exportacion: Exportacion, modificado: Long): Boolean =
        exportacion.archivo.lastModified() >= modificado

    fun eliminar(archivo: File) {
        archivo.delete()
        modificarIndice { it - relativo(archivo) }
    }

    /**
     * Limpieza de las exportaciones hechas con versiones anteriores (que creaban un PDF nuevo
     * con fecha cada vez). Por cada parada y salida se queda con el **más reciente**, lo pasa
     * a la carpeta de su salida como PDF vigente, y borra los duplicados.
     * Los PDF que no se pueden asociar a ninguna salida se dejan como están.
     * Se puede llamar en cada inicio: si no hay nada viejo, no hace nada.
     */
    @Synchronized
    fun limpiarExportacionesAnteriores(recorridos: List<Recorrido>) {
        val indice = leerIndice().toMutableMap()
        val viejos = carpeta.listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }.orEmpty()
        if (viejos.isEmpty()) return

        // Datos de cada PDF viejo: del índice o deducidos del nombre del archivo.
        val identificados = viejos.mapNotNull { archivo ->
            val datos = indice[archivo.name] ?: deducirDelNombre(archivo, recorridos) ?: return@mapNotNull null
            val recorrido = recorridos.find { it.id == datos.recorridoId }
            val paradaId = datos.paradaId ?: numeroDeParada(datos.contenido)?.let { numero ->
                recorrido?.paradas?.find { it.numero == numero }?.id
            }
            Triple(archivo, datos.copy(paradaId = paradaId), recorrido)
        }

        identificados
            .groupBy { (_, datos, _) -> Triple(datos.recorridoId, datos.paradaId, datos.contenido) }
            .values
            .forEach { grupo ->
                val ordenados = grupo.sortedByDescending { it.first.lastModified() }
                // Duplicados: se borran todos menos el más reciente.
                ordenados.drop(1).forEach { (archivo, _, _) ->
                    archivo.delete()
                    indice.remove(archivo.name)
                }
                val (archivo, datos, recorrido) = ordenados.first()
                val parada = recorrido?.paradas?.find { it.id == datos.paradaId }
                val esSalidaCompleta = numeroDeParada(datos.contenido) == null
                if (recorrido == null || (!esSalidaCompleta && parada == null)) {
                    // Salida o parada borrada: se deja donde está, ya sin duplicados.
                    indice[archivo.name] = datos
                    return@forEach
                }
                val nuevos = if (parada != null) datosParada(recorrido, parada) else datosSalida(recorrido)
                val destino = File(File(carpeta, recorrido.id.toString()).apply { mkdirs() }, "${limpiar(nuevos.second)}.pdf")
                val yaHayVigente = indice.entries.any { (ruta, d) ->
                    d.recorridoId == recorrido.id && d.paradaId == nuevos.first.paradaId && !d.copia &&
                        ruta.contains('/') && File(carpeta, ruta).exists()
                }
                indice.remove(archivo.name)
                if (yaHayVigente || destino.exists()) {
                    archivo.delete()   // ya hay un vigente con la estructura nueva
                } else if (archivo.renameTo(destino)) {
                    indice[relativo(destino)] = nuevos.first
                }
            }
        guardarIndice(indice)
    }

    private fun leerIndice(): Map<String, DatosExportacion> =
        if (!archivoIndice.exists()) emptyMap()
        else runCatching { json.decodeFromString<Map<String, DatosExportacion>>(archivoIndice.readText()) }
            .getOrDefault(emptyMap())

    private fun guardarIndice(indice: Map<String, DatosExportacion>) {
        // De paso se quitan las entradas de PDFs que ya no existen.
        archivoIndice.writeText(json.encodeToString(indice.filterKeys { File(carpeta, it).exists() }))
    }

    @Synchronized
    private fun modificarIndice(cambio: (Map<String, DatosExportacion>) -> Map<String, DatosExportacion>) =
        guardarIndice(cambio(leerIndice()))

    private fun relativo(archivo: File): String = archivo.relativeTo(carpeta).invariantSeparatorsPath

    companion object {
        /** Datos y nombre de archivo del PDF de una parada. */
        fun datosParada(recorrido: Recorrido, parada: Parada) =
            DatosExportacion(recorrido.id, recorrido.nombre, "Parada ${parada.numero}", parada.id) to
                "Parada_${parada.numero}_${recorrido.nombre}"

        /** Datos y nombre de archivo del PDF de la salida completa. */
        fun datosSalida(recorrido: Recorrido) =
            DatosExportacion(recorrido.id, recorrido.nombre, "Salida completa") to "Salida_${recorrido.nombre}"

        fun limpiar(nombre: String) = nombre.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifEmpty { "planilla" }

        private fun sello() = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())

        private fun numeroDeParada(contenido: String) =
            Regex("^Parada (\\d+)$").find(contenido)?.groupValues?.get(1)?.toIntOrNull()

        /**
         * Para PDFs de versiones anteriores sin índice: deduce la salida del nombre del archivo
         * ("Parada_2_Cerro_Chenque_2026-…" o "Salida_Cerro_Chenque_2026-…"). Solo si el nombre
         * coincide con exactamente una salida.
         */
        fun deducirDelNombre(archivo: File, recorridos: List<Recorrido>): DatosExportacion? {
            val sinSello = archivo.nameWithoutExtension.replace(Regex("_\\d{4}-\\d{2}-\\d{2}_\\d{6}$"), "")
            val parada = Regex("^Parada_(\\d+)_(.+)$").find(sinSello)
            val (contenido, nombre) = when {
                parada != null -> "Parada ${parada.groupValues[1]}" to parada.groupValues[2]
                sinSello.startsWith("Salida_") -> "Salida completa" to sinSello.removePrefix("Salida_")
                sinSello.startsWith("Recorrido_") -> "Salida completa" to sinSello.removePrefix("Recorrido_")
                else -> return null
            }
            val coincidencia = recorridos.singleOrNull { limpiar(it.nombre) == nombre } ?: return null
            return DatosExportacion(coincidencia.id, coincidencia.nombre, contenido)
        }
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
