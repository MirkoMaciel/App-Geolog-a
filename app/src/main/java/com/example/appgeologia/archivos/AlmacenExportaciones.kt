package com.example.appgeologia.archivos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Carpeta donde quedan guardados todos los PDF generados (pantalla "Mis exportaciones").
 */
class AlmacenExportaciones(context: Context) {

    val carpeta: File = File(context.filesDir, "exportaciones").apply { mkdirs() }

    /** PDFs exportados, del más nuevo al más viejo. */
    fun listar(): List<File> =
        carpeta.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    /**
     * Archivo nuevo para un PDF. Se le agrega la fecha y hora para no pisar
     * exportaciones anteriores de la misma parada o recorrido.
     */
    fun nuevoArchivo(nombre: String): File {
        val limpio = nombre.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifEmpty { "planilla" }
        val sello = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
        return File(carpeta, "${limpio}_$sello.pdf")
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
