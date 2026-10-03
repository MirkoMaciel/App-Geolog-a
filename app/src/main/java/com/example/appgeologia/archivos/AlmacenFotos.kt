package com.example.appgeologia.archivos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Guarda las fotos de los items dentro del almacenamiento privado de la app.
 *
 * Al importar una foto (de la cámara o de la galería) se endereza según los datos EXIF
 * y se achica a [LADO_MAXIMO] píxeles, para que todas las fotos se vean derechas en la
 * app y en el PDF sin ocupar demasiado espacio.
 */
class AlmacenFotos(private val context: Context) {

    val carpeta: File = File(context.filesDir, "fotos").apply { mkdirs() }

    fun archivo(nombre: String): File = File(carpeta, nombre)

    /**
     * Archivo temporal donde la cámara escribe la foto, y su Uri para pasarle a la cámara.
     * Después de sacarla hay que llamar a [importarArchivo].
     */
    fun nuevoArchivoCamara(): Pair<File, Uri> {
        val carpetaCamara = File(context.cacheDir, "camara").apply { mkdirs() }
        val archivo = File(carpetaCamara, "foto_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        return archivo to uri
    }

    /** Importa la foto sacada con la cámara y borra el temporal. Devuelve el nombre guardado. */
    fun importarArchivo(temporal: File): String? {
        if (!temporal.exists() || temporal.length() == 0L) return null
        val orientacion = runCatching {
            ExifInterface(temporal.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val nombre = guardar({ temporal.inputStream() }, orientacion)
        temporal.delete()
        return nombre
    }

    /** Importa una foto elegida de la galería o de los archivos. Devuelve el nombre guardado. */
    fun importarUri(uri: Uri): String? {
        val resolver = context.contentResolver
        val orientacion = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        return guardar({ resolver.openInputStream(uri) ?: error("No se pudo abrir la imagen") }, orientacion)
    }

    fun eliminar(nombres: Collection<String>) {
        nombres.forEach { archivo(it).delete() }
    }

    /** Borra las fotos que ya no pertenecen a ningún item. */
    fun eliminarNoUsadas(enUso: Set<String>) {
        carpeta.listFiles()?.filter { it.name !in enUso }?.forEach { it.delete() }
    }

    /**
     * Lee una foto achicada a [ladoMaximo] (para miniaturas o para el PDF) sin cargar
     * la imagen completa en memoria.
     */
    fun cargarReducida(nombre: String, ladoMaximo: Int): Bitmap? {
        val archivo = archivo(nombre)
        if (!archivo.exists()) return null
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.absolutePath, opciones)
        opciones.inSampleSize = factorReduccion(opciones.outWidth, opciones.outHeight, ladoMaximo)
        opciones.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(archivo.absolutePath, opciones)
    }

    private fun guardar(abrir: () -> java.io.InputStream, orientacion: Int): String? = runCatching {
        // 1) Medir sin cargar la imagen entera
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        abrir().use { BitmapFactory.decodeStream(it, null, opciones) }
        if (opciones.outWidth <= 0) return null

        // 2) Cargarla reducida
        opciones.inSampleSize = factorReduccion(opciones.outWidth, opciones.outHeight, LADO_MAXIMO)
        opciones.inJustDecodeBounds = false
        var bitmap = abrir().use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null

        // 3) Escalar al tamaño final y enderezar
        val escala = LADO_MAXIMO.toFloat() / max(bitmap.width, bitmap.height)
        val matriz = Matrix().apply {
            if (escala < 1f) postScale(escala, escala)
            when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }
        if (!matriz.isIdentity) {
            val transformado = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
            if (transformado !== bitmap) bitmap.recycle()
            bitmap = transformado
        }

        // 4) Guardar como JPEG
        val nombre = "${UUID.randomUUID()}.jpg"
        archivo(nombre).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, it) }
        bitmap.recycle()
        nombre
    }.getOrNull()

    private fun factorReduccion(ancho: Int, alto: Int, ladoMaximo: Int): Int {
        var factor = 1
        while (max(ancho, alto) / (factor * 2) >= ladoMaximo) factor *= 2
        return factor
    }

    private companion object {
        const val LADO_MAXIMO = 2048
        const val CALIDAD_JPEG = 85
    }
}
