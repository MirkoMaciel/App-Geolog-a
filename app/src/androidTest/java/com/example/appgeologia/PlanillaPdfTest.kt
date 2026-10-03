package com.example.appgeologia

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appgeologia.data.recorrido.DatosSitio
import com.example.appgeologia.archivos.AlmacenFotos
import com.example.appgeologia.data.recorrido.ItemParada
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.pdf.PlanillaPdf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Genera la planilla con datos de ejemplo y verifica la cantidad de páginas.
 * También guarda cada página como PNG en /sdcard/Android/data/<app>/files/pdf-test/
 * para poder revisarla a ojo.
 */
@RunWith(AndroidJUnit4::class)
class PlanillaPdfTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun horizonte(id: Long, nombre: String, prof: String) = ItemParada(
        titulo = "Horizonte $nombre", descripcion = "Descripción del horizonte $nombre.", fecha = 1_790_000_000_000,
        id = id, nombre = nombre, profundidad = prof,
        limiteDefinicion = "C", limiteTopografia = "Ond",
        colorSeco = "10YR 5/3", colorHumedo = "10YR 3/2", textura = "Franco arenoso",
        resistencia = "Blando", adhesividad = "Lig adhesivo", plasticidad = "Mod plástico",
        estructuraTipo = "Bloques subangulares", estructuraGrado = "Mod", estructuraTamano = "Medio",
        reaccionHcl = "SEf", concentracionTipo = "N", concentracionTamano = "Fino",
        concentracionColor = "Blanco", concentracionCantidad = "Comunes", concentracionForma = "Esférica",
        rasgosRedox = "No", moteados = "Escasos", barnices = "Arcilla, débiles", humedad = "Fres.",
        raicesCantidad = "Comunes", raicesTamano = "Finas", raicesUbicacion = "Entre agregados",
        fragmentosTipo = "Rodados basálticos", fragmentosVolumen = "15", fragmentosRedondez = "Subredondeado",
        fragmentosTamano = "Grava"
    )

    private val parada = Parada(
        id = 1, numero = 3, fecha = 1_790_000_000_000,
        latitud = -45.864123, longitud = -67.496612, altitud = 85.0,
        sitio = DatosSitio(
            calicata = "C-07", sitioReferencia = "Ruta 3 km 1850", proyecto = "Relevamiento Valle Hermoso",
            geomorfologia = "Terraza fluvial", posicionGeomorfica = "Loma", relieve = "Normal",
            exposicion = "NE", materialOriginario = "Sedimentos aluviales", pendiente = "2 %",
            vegetacion = "Estepa arbustiva con coirón y neneo, cobertura irregular", coberturaVegetal = "40 %",
            claseDrenaje = "Bien drenado", anegamiento = "No", erosion = "Leve eólica",
            salesAlcalis = "No", usoTierra = "Ganadería ovina", pedregosidad = "Moderada",
            clasificacion = "Haplocalcid típico", temperatura = "14 °C"
        ),
        items = listOf(
            ItemParada(
                id = 10, titulo = "Afloramiento de basalto", fecha = 1_790_000_100_000,
                descripcion = "Afloramiento en el corte de la ruta, a unos 30 m al norte de la calicata. " +
                    "Basalto vesicular con disyunción columnar; se tomaron fotos de detalle y de contexto.",
                fotos = listOf("prueba_1.jpg", "prueba_2.jpg", "prueba_3.jpg")
            ),
            horizonte(1, "A", "0-12"), horizonte(2, "Bt", "12-35"),
            horizonte(3, "Bk", "35-70"), horizonte(4, "C", "70-110"), horizonte(5, "2C", "110+"),
            ItemParada(id = 11, titulo = "Vista general del sitio", fecha = 1_790_000_200_000, fotos = listOf("prueba_2.jpg"))
        ),
        observaciones = "Perfil descripto en la pared norte de la calicata. Presencia de carbonatos desde los 35 cm."
    )

    private val recorrido = Recorrido(
        id = 1, nombre = "Salida Suelos 2026", proyecto = "Relevamiento Valle Hermoso",
        fechaInicio = 1_790_000_000_000, paradas = listOf(parada)
    )

    /** Fotos de prueba (una apaisada y una vertical) en la carpeta de fotos de la app. */
    @Before
    fun crearFotos() {
        val almacen = AlmacenFotos(context)
        listOf("prueba_1.jpg" to (1600 to 1200), "prueba_2.jpg" to (900 to 1600), "prueba_3.jpg" to (1600 to 1000))
            .forEach { (nombre, tam) ->
                val bitmap = Bitmap.createBitmap(tam.first, tam.second, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawPaint(Paint().apply {
                    shader = LinearGradient(0f, 0f, tam.first.toFloat(), tam.second.toFloat(),
                        Color.rgb(139, 74, 43), Color.rgb(201, 162, 39), Shader.TileMode.CLAMP)
                })
                canvas.drawText(nombre, 40f, 160f, Paint().apply { color = Color.WHITE; textSize = 120f })
                almacen.archivo(nombre).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            }
    }

    /** 5 items con datos de suelo = 2 páginas de tabla; después 2 de fichas con fotos. */
    @Test
    fun planillaDeParadaConTablaYFichas() {
        val pdf = PlanillaPdf(context).generarParada(recorrido, parada)
        assertEquals(4, renderizar(pdf, "parada"))
    }

    @Test
    fun pdfDeRecorridoIncluyeCaratula() {
        val pdf = PlanillaPdf(context).generarRecorrido(recorrido)
        assertEquals(5, renderizar(pdf, "recorrido"))
    }

    /** Dibuja cada página en un PNG (a 2x) y devuelve la cantidad de páginas. */
    private fun renderizar(pdf: File, prefijo: String): Int {
        val salida = File(context.getExternalFilesDir(null), "pdf-test").apply { mkdirs() }
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { pagina ->
                        val bitmap = Bitmap.createBitmap(pagina.width * 2, pagina.height * 2, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        File(salida, "$prefijo-${i + 1}.png").outputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        }
                    }
                }
                return renderer.pageCount
            }
        }
    }
}
