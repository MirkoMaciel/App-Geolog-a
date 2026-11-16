package com.example.appgeologia.ui.recorridos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.MapTileProviderBasic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.ubicacion.tienePermisoUbicacion
import com.example.appgeologia.ui.theme.OxidoHierro
import com.example.appgeologia.ui.theme.Pizarra
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/** Centro por defecto: Comodoro Rivadavia (sede de la UNPSJB). */
private val CENTRO_POR_DEFECTO = GeoPoint(-45.8641, -67.4966)

/**
 * Mosaicos de OpenStreetMap. No se usa TileSourceFactory.MAPNIK porque osmdroid reemplaza
 * el User-Agent por el nombre del paquete, y OSM bloquea "com.example..." (error 403).
 * Esta versión envía el User-Agent configurado en [configurarOsmdroid].
 */
private val MOSAICOS_OSM = XYTileSource(
    "OpenStreetMap", 0, 19, 256, ".png",
    arrayOf("https://tile.openstreetmap.org/"),
    "© OpenStreetMap contributors",
    TileSourcePolicy(
        2,
        TileSourcePolicy.FLAG_NO_BULK or
            TileSourcePolicy.FLAG_NO_PREVENTIVE or
            TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
    )
)

/**
 * Mapa de OpenStreetMap con las paradas del recorrido numeradas y unidas por una línea.
 * Mantener presionado sobre el mapa llama a [onMantenerPresionado] con esa coordenada.
 */
@Composable
fun MapaRecorrido(
    paradas: List<Parada>,
    onParadaClick: (Parada) -> Unit,
    onMantenerPresionado: (latitud: Double, longitud: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onParadaClickActual by rememberUpdatedState(onParadaClick)
    val onMantenerActual by rememberUpdatedState(onMantenerPresionado)

    val estado = remember { EstadoMapa() }
    val mapa = remember {
        configurarOsmdroid(context)
        MapView(context).apply {
            setTileSource(MOSAICOS_OSM)
            setMultiTouchControls(true)
            // El mapa está dentro de una pantalla con scroll: mientras se toca el mapa,
            // se le pide a la pantalla que no robe el gesto (así se puede mover el mapa).
            setOnTouchListener { vista, evento ->
                when (evento.actionMasked) {
                    MotionEvent.ACTION_DOWN -> vista.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        vista.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(13.0)
            controller.setCenter(CENTRO_POR_DEFECTO)
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    p?.let { onMantenerActual(it.latitude, it.longitude) }
                    return true
                }
            }))
        }
    }

    // El mapa necesita enterarse de las pausas de la pantalla para liberar recursos.
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observador)
            mapa.onDetach()
        }
    }

    // ----- Mapa en línea o mapa descargado -----
    // Sin internet (o si el usuario lo elige) se usa el mapa descargado de la zona,
    // que se dibuja en el teléfono sin necesitar conexión.
    val hayInternet by rememberHayInternet()
    val hayMapaDescargado = remember { MapaSinConexion.disponible(context) }
    var forzarDescargado by rememberSaveable { mutableStateOf(false) }
    val usarDescargado = hayMapaDescargado && (forzarDescargado || !hayInternet)

    LaunchedEffect(usarDescargado) {
        if (usarDescargado == estado.usandoDescargado) return@LaunchedEffect
        val proveedor = if (usarDescargado) {
            // La primera vez se copia el archivo del mapa: se hace fuera de la pantalla.
            withContext(Dispatchers.IO) { MapaSinConexion.archivo(context) }
            MapaSinConexion.crearProveedor(context)
        } else {
            MapTileProviderBasic(context, MOSAICOS_OSM)
        }
        if (proveedor != null) {
            mapa.setTileProvider(proveedor)
            estado.usandoDescargado = usarDescargado
            mapa.invalidate()
        }
    }

    Column {
    AndroidView(
        factory = { mapa },
        // Sin esto el mapa se dibuja por fuera de su espacio y tapa el resto de la pantalla.
        modifier = modifier.clipToBounds(),
        update = { vista ->
            // Punto azul con la posición del usuario (solo si ya dio el permiso).
            if (estado.miUbicacion == null && tienePermisoUbicacion(context)) {
                estado.miUbicacion = MyLocationNewOverlay(GpsMyLocationProvider(context), vista).also {
                    it.enableMyLocation()
                    vista.overlays.add(it)
                }
            }

            vista.overlays.removeAll { it is Marker || it is Polyline }
            val conPosicion = paradas
                .filter { it.latitud != null && it.longitud != null }
                .sortedBy { it.numero }
            val puntos = conPosicion.map { GeoPoint(it.latitud!!, it.longitud!!) }

            if (puntos.size > 1) {
                vista.overlays.add(Polyline(vista).apply {
                    setPoints(puntos)
                    outlinePaint.color = Pizarra.toArgb()
                    outlinePaint.strokeWidth = 6f
                })
            }
            conPosicion.forEachIndexed { i, parada ->
                vista.overlays.add(Marker(vista).apply {
                    position = puntos[i]
                    title = "Parada ${parada.numero}"
                    snippet = parada.sitio.calicata
                    icon = iconoNumerado(vista.context, parada.numero)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        onParadaClickActual(parada)
                        true
                    }
                })
            }

            // Centrar al abrir y cada vez que se agrega una parada nueva.
            if (puntos.size != estado.cantidadCentrada) {
                puntos.lastOrNull()?.let {
                    if (estado.cantidadCentrada < 0) vista.controller.setZoom(15.0)
                    vista.controller.animateTo(it)
                }
                estado.cantidadCentrada = puntos.size
            }
            vista.invalidate()
        }
    )
    AvisoMapa(
        hayInternet = hayInternet,
        hayMapaDescargado = hayMapaDescargado,
        forzarDescargado = forzarDescargado,
        onForzarDescargado = { forzarDescargado = it }
    )
    }
}

/** Aviso debajo del mapa sobre qué mapa se está viendo y qué pasa sin conexión. */
@Composable
private fun AvisoMapa(
    hayInternet: Boolean,
    hayMapaDescargado: Boolean,
    forzarDescargado: Boolean,
    onForzarDescargado: (Boolean) -> Unit
) {
    val zona = MapaSinConexion.NOMBRE_ZONA
    val aviso = when {
        !hayInternet && hayMapaDescargado ->
            "Sin conexión: se usa el mapa descargado de la $zona. Fuera de esa zona el mapa " +
                "se ve vacío. Las paradas y el GPS funcionan igual."
        !hayInternet ->
            "Sin conexión: el mapa muestra solo las zonas que ya se vieron con internet. " +
                "Las paradas y el GPS funcionan igual."
        forzarDescargado ->
            "Usando el mapa descargado de la $zona (funciona sin internet)."
        else -> null
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (aviso != null) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(aviso, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (hayInternet && hayMapaDescargado) {
            FilterChip(
                selected = forzarDescargado,
                onClick = { onForzarDescargado(!forzarDescargado) },
                label = { Text("Usar mapa descargado") },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

private class EstadoMapa {
    var miUbicacion: MyLocationNewOverlay? = null
    var cantidadCentrada = -1
    var usandoDescargado = false
}

private fun configurarOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        // OpenStreetMap exige identificar la app que descarga los mosaicos.
        userAgentValue = "AppGeologia/1.0 (Android; UNPSJB Desarrollo de Apps Moviles)"
    }
}

/** Círculo color óxido con el número de la parada. */
private fun iconoNumerado(context: Context, numero: Int): BitmapDrawable {
    val densidad = context.resources.displayMetrics.density
    val tam = (30 * densidad).toInt()
    val bitmap = Bitmap.createBitmap(tam, tam, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radio = tam / 2f
    canvas.drawCircle(radio, radio, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    canvas.drawCircle(radio, radio, radio - 2 * densidad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OxidoHierro.toArgb()
    })
    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13 * densidad
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawText(numero.toString(), radio, radio - (texto.descent() + texto.ascent()) / 2, texto)
    return BitmapDrawable(context.resources, bitmap)
}
