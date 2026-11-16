package com.example.appgeologia.ui.recorridos

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.osmdroid.mapsforge.MapsForgeTileProvider
import org.osmdroid.mapsforge.MapsForgeTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.BoundingBox
import java.io.File

/**
 * Mapa descargado para usar sin internet (formato Mapsforge, datos de OpenStreetMap).
 *
 * Cubre el departamento Escalante (Chubut) con un margen alrededor: Comodoro Rivadavia,
 * Rada Tilly, Caleta Olivia, Sarmiento, Pico Truncado, Camarones, etc.
 * El archivo viaja dentro de la app (assets/mapas) y la primera vez se copia a la memoria
 * interna, porque la librería necesita leerlo como archivo.
 *
 * Cómo se generó (para actualizarlo o ampliar la zona):
 * 1. Datos de Argentina de Geofabrik: download.geofabrik.de/south-america/argentina-latest.osm.pbf
 * 2. Recorte con Osmosis 0.49.2:
 *    `--rbf file=argentina.osm.pbf --bounding-box top=-44.60 bottom=-47.10 left=-69.80 right=-66.80
 *     completeWays=yes completeRelations=yes --wb file=escalante.osm.pbf`
 * 3. Conversión con mapsforge-map-writer 0.25.0 (jar-with-dependencies en el classpath de Osmosis):
 *    `--rbf file=escalante.osm.pbf --mapfile-writer file=escalante.map
 *     bbox=-47.10,-69.80,-44.60,-66.80 preferred-languages=es type=ram`
 * Si se cambia la zona, actualizar también [ZONA] y [NOMBRE_ZONA].
 */
object MapaSinConexion {
    const val NOMBRE_ZONA = "zona de Escalante (Chubut)"
    private const val ASSET = "mapas/escalante.map"

    /** Límites de la zona incluida en el archivo (los mismos que se usaron para recortarlo). */
    val ZONA = BoundingBox(-44.60, -66.80, -47.10, -69.80)

    private var inicializado = false

    /** true si la app trae el mapa descargado. */
    fun disponible(context: Context): Boolean =
        context.assets.list("mapas")?.contains(ASSET.substringAfter('/')) == true

    /** Devuelve el archivo del mapa, copiándolo desde la app si hace falta (null si no viene). */
    @Synchronized
    fun archivo(context: Context): File? {
        if (!disponible(context)) return null
        val destino = File(context.filesDir, ASSET)
        val tamanoEsperado = context.assets.openFd(ASSET).use { it.length }
        if (!destino.exists() || destino.length() != tamanoEsperado) {
            destino.parentFile?.mkdirs()
            val temporal = File(destino.path + ".tmp")
            context.assets.open(ASSET).use { entrada ->
                temporal.outputStream().use { entrada.copyTo(it) }
            }
            temporal.renameTo(destino)
        }
        return destino
    }

    /** Proveedor de mosaicos que dibuja el mapa en el teléfono a partir del archivo. */
    fun crearProveedor(context: Context): MapsForgeTileProvider? {
        val archivo = archivo(context) ?: return null
        if (!inicializado) {
            MapsForgeTileSource.createInstance(context.applicationContext as Application)
            inicializado = true
        }
        val fuente = MapsForgeTileSource.createFromFiles(arrayOf(archivo))
        return MapsForgeTileProvider(SimpleRegisterReceiver(context), fuente, null)
    }
}

/** Indica si el teléfono tiene una conexión con salida a internet; se actualiza sola. */
@Composable
fun rememberHayInternet(): State<Boolean> {
    val context = LocalContext.current
    val gestor = remember { context.getSystemService(ConnectivityManager::class.java) }
    val estado = remember { mutableStateOf(hayInternet(gestor)) }
    DisposableEffect(gestor) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { estado.value = hayInternet(gestor) }
            override fun onLost(network: Network) { estado.value = hayInternet(gestor) }
            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                estado.value = hayInternet(gestor)
            }
        }
        gestor.registerDefaultNetworkCallback(callback)
        onDispose { gestor.unregisterNetworkCallback(callback) }
    }
    return estado
}

private fun hayInternet(gestor: ConnectivityManager): Boolean {
    val capacidades = gestor.getNetworkCapabilities(gestor.activeNetwork) ?: return false
    return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
