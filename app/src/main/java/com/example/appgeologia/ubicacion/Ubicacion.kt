package com.example.appgeologia.ubicacion

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Posición obtenida del GPS. */
data class Posicion(
    val latitud: Double,
    val longitud: Double,
    val altitud: Double?,
    val precisionMetros: Float?
)

sealed interface ResultadoUbicacion {
    data class Exito(val posicion: Posicion) : ResultadoUbicacion
    data object SinPermiso : ResultadoUbicacion
    data object NoDisponible : ResultadoUbicacion
}

fun tienePermisoUbicacion(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Pide una posición nueva al GPS (o a la red si el GPS está apagado).
 * Si no llega en [tiempoMaximoMs], devuelve la última posición conocida.
 */
@SuppressLint("MissingPermission")
suspend fun obtenerPosicionActual(context: Context, tiempoMaximoMs: Long = 30_000): Posicion? {
    if (!tienePermisoUbicacion(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val proveedor = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }

    val ultimaConocida: Location? = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }

    val nueva: Location? = proveedor?.let {
        withTimeoutOrNull(tiempoMaximoMs) {
            suspendCancellableCoroutine { continuacion ->
                val cancelacion = CancellationSignal()
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    proveedor,
                    cancelacion,
                    ContextCompat.getMainExecutor(context)
                ) { location -> continuacion.resume(location) }
                continuacion.invokeOnCancellation { cancelacion.cancel() }
            }
        }
    }

    return (nueva ?: ultimaConocida)?.let { loc ->
        Posicion(
            latitud = loc.latitude,
            longitud = loc.longitude,
            altitud = if (loc.hasAltitude()) loc.altitude else null,
            precisionMetros = if (loc.hasAccuracy()) loc.accuracy else null
        )
    }
}

/** Estado devuelto por [rememberSolicitudUbicacion]. */
class SolicitudUbicacion(val solicitar: () -> Unit, val buscando: Boolean)

/**
 * Devuelve una acción que pide permiso de ubicación (si hace falta), busca la posición
 * actual y entrega el resultado en [onResultado].
 */
@Composable
fun rememberSolicitudUbicacion(onResultado: (ResultadoUbicacion) -> Unit): SolicitudUbicacion {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onResultadoActual by rememberUpdatedState(onResultado)
    var buscando by remember { mutableStateOf(false) }

    val buscar: () -> Unit = {
        buscando = true
        scope.launch {
            val posicion = obtenerPosicionActual(context)
            buscando = false
            onResultadoActual(
                if (posicion != null) ResultadoUbicacion.Exito(posicion) else ResultadoUbicacion.NoDisponible
            )
        }
    }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) buscar() else onResultadoActual(ResultadoUbicacion.SinPermiso)
    }

    return SolicitudUbicacion(
        solicitar = {
            if (tienePermisoUbicacion(context)) {
                buscar()
            } else {
                lanzadorPermiso.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        buscando = buscando
    )
}
