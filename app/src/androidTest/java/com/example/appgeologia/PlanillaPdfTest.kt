package com.example.appgeologia

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appgeologia.data.recorrido.DatosSitio
import com.example.appgeologia.data.recorrido.Horizonte
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.pdf.PlanillaPdf
import org.junit.Assert.assertEquals
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

    private fun horizonte(id: Long, nombre: String, prof: String) = Horizonte(
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
        horizontes = listOf(
            horizonte(1, "A", "0-12"), horizonte(2, "Bt", "12-35"),
            horizonte(3, "Bk", "35-70"), horizonte(4, "C", "70-110"), horizonte(5, "2C", "110+")
        ),
        observaciones = "Perfil descripto en la pared norte de la calicata. Presencia de carbonatos desde los 35 cm."
    )

    private val recorrido = Recorrido(
        id = 1, nombre = "Salida Suelos 2026", proyecto = "Relevamiento Valle Hermoso",
        fechaInicio = 1_790_000_000_000, paradas = listOf(parada)
    )

    @Test
    fun planillaDeParadaConCincoHorizontesOcupaDosPaginas() {
        val pdf = PlanillaPdf(context).generarParada(recorrido, parada)
        assertEquals(2, renderizar(pdf, "parada"))
    }

    @Test
    fun pdfDeRecorridoIncluyeCaratula() {
        val pdf = PlanillaPdf(context).generarRecorrido(recorrido)
        assertEquals(3, renderizar(pdf, "recorrido"))
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
