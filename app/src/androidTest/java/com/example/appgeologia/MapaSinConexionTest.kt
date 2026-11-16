package com.example.appgeologia

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.appgeologia.ui.recorridos.MapaSinConexion
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.TileSystemWebMercator
import java.io.File

/**
 * Comprueba que el mapa descargado (assets/mapas/escalante.map) se pueda leer y dibujar
 * sin internet. Guarda los mosaicos dibujados en Android/data/<app>/files/TestMapa.
 */
@RunWith(AndroidJUnit4::class)
class MapaSinConexionTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun dibujaLaZonaDeEscalante() {
        assertTrue("La app no trae el mapa descargado", MapaSinConexion.disponible(context))
        val archivo = MapaSinConexion.archivo(context)
        assertNotNull(archivo)

        MapsForgeTileSource.createInstance(context.applicationContext as android.app.Application)
        val fuente = MapsForgeTileSource.createFromFiles(arrayOf(archivo!!))
        val salida = File(context.getExternalFilesDir("TestMapa"), "").apply { mkdirs() }
        val sistema = TileSystemWebMercator()

        // Lugares dentro de la zona: Comodoro Rivadavia, Sarmiento y Caleta Olivia.
        val lugares = listOf(
            Triple("comodoro", -45.8641, -67.4966),
            Triple("sarmiento", -45.5880, -69.0700),
            Triple("caleta_olivia", -46.4393, -67.5281)
        )
        for ((nombre, lat, lon) in lugares) {
            for (zoom in listOf(10, 13, 15)) {
                val x = sistema.getTileXFromLongitude(lon, zoom)
                val y = sistema.getTileYFromLatitude(lat, zoom)
                val dibujo = fuente.renderTile(MapTileIndex.getTileIndex(zoom, x, y))
                assertNotNull("No se dibujó $nombre z$zoom", dibujo)
                val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                dibujo!!.setBounds(0, 0, 256, 256)
                dibujo.draw(Canvas(bitmap))
                val colores = HashSet<Int>()
                for (i in 0 until 256 step 8) for (j in 0 until 256 step 8) colores += bitmap.getPixel(i, j)
                assertTrue("$nombre z$zoom salió vacío", colores.size > 3)
                File(salida, "${nombre}_z$zoom.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        fuente.dispose()
    }
}
