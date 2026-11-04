package com.example.appgeologia.ui.exportaciones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appgeologia.archivos.AlmacenExportaciones
import com.example.appgeologia.archivos.DatosExportacion
import com.example.appgeologia.archivos.Exportacion
import com.example.appgeologia.archivos.compartirPdf
import com.example.appgeologia.archivos.verPdf
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.ui.componentes.SeccionDesplegable
import com.example.appgeologia.ui.componentes.formatearFechaHora
import java.io.File

/** Los PDF de una misma salida, como se muestran en un desplegable. */
private class GrupoExportaciones(
    val clave: String,
    val titulo: String,
    val exportaciones: List<Exportacion>
)

/**
 * Historial de los PDF generados, agrupados por salida en desplegables. Solo aparecen
 * las salidas que tienen al menos una exportación. Los PDF de salidas borradas se
 * conservan con el nombre que tenía la salida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportacionesScreen(recorridos: List<Recorrido>, barraInferior: @Composable () -> Unit) {
    val context = LocalContext.current
    val almacen = remember { AlmacenExportaciones(context) }
    var exportaciones by remember { mutableStateOf(almacen.listarConDatos()) }
    var aEliminar by remember { mutableStateOf<File?>(null) }
    // Claves de los grupos desplegados (se recuerdan al girar la pantalla).
    var abiertos by rememberSaveable { mutableStateOf(listOf<String>()) }

    // Se vuelve a leer la carpeta cada vez que se entra a la pantalla.
    LaunchedEffect(Unit) { exportaciones = almacen.listarConDatos() }

    val grupos = agrupar(exportaciones, recorridos)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis exportaciones") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = barraInferior
    ) { innerPadding ->
        if (exportaciones.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Todavía no exportaste ningún PDF.\nAbrí una parada o una salida y tocá el ícono de compartir.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(grupos, key = { it.clave }) { grupo ->
                    SeccionDesplegable(
                        titulo = grupo.titulo,
                        abierta = grupo.clave in abiertos,
                        onCambiar = { abrir -> abiertos = if (abrir) abiertos + grupo.clave else abiertos - grupo.clave },
                        icono = Icons.Filled.Place,
                        detalle = "${grupo.exportaciones.size} PDF · último: " +
                            formatearFechaHora(grupo.exportaciones.first().archivo.lastModified())
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grupo.exportaciones.forEach { exportacion ->
                                TarjetaPdf(
                                    exportacion,
                                    onVer = { verPdf(context, exportacion.archivo) },
                                    onCompartir = { compartirPdf(context, exportacion.archivo) },
                                    onEliminar = { aEliminar = exportacion.archivo }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    aEliminar?.let { archivo ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar PDF") },
            text = { Text("¿Eliminar \"${archivo.name}\"? Los datos de la salida no se borran.") },
            confirmButton = {
                TextButton(onClick = {
                    almacen.eliminar(archivo)
                    exportaciones = almacen.listarConDatos()
                    aEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaPdf(
    exportacion: Exportacion,
    onVer: () -> Unit,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit
) {
    val archivo = exportacion.archivo
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onVer)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exportacion.datos?.contenido ?: nombreLegible(archivo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${formatearFechaHora(archivo.lastModified())} · ${tamano(archivo)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onCompartir) {
                Icon(Icons.Filled.Share, contentDescription = "Compartir")
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

/**
 * Agrupa los PDF por salida. Los grupos se ordenan por su exportación más reciente;
 * los PDF sin salida conocida van al final, en "Otras exportaciones".
 */
private fun agrupar(exportaciones: List<Exportacion>, recorridos: List<Recorrido>): List<GrupoExportaciones> {
    val nombresActuales = recorridos.associate { it.id to it.nombre }
    val (conSalida, sinSalida) = exportaciones
        .map { if (it.datos == null) it.copy(datos = deducirSalida(it.archivo, recorridos)) else it }
        .partition { it.datos != null }

    val grupos = conSalida
        .groupBy { it.datos!!.recorridoId }
        .map { (id, lista) ->
            val titulo = nombresActuales[id] ?: "${lista.first().datos!!.nombreSalida} (salida eliminada)"
            GrupoExportaciones("salida-$id", titulo, lista)
        }
        .sortedByDescending { it.exportaciones.first().archivo.lastModified() }

    return if (sinSalida.isEmpty()) grupos
    else grupos + GrupoExportaciones("otras", "Otras exportaciones", sinSalida)
}

/**
 * Para PDFs exportados antes de que existiera el índice: deduce la salida a partir del
 * nombre del archivo ("Parada_2_Cerro_Chenque_…" o "Salida_Cerro_Chenque_…"). Solo se
 * asigna si el nombre coincide con exactamente una salida.
 */
private fun deducirSalida(archivo: File, recorridos: List<Recorrido>): DatosExportacion? {
    fun limpiar(texto: String) = texto.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_')

    val sinSello = archivo.nameWithoutExtension.replace(Regex("_\\d{4}-\\d{2}-\\d{2}_\\d{6}$"), "")
    val parada = Regex("^Parada_(\\d+)_(.+)$").find(sinSello)
    val (contenido, nombre) = when {
        parada != null -> "Parada ${parada.groupValues[1]}" to parada.groupValues[2]
        sinSello.startsWith("Salida_") -> "Salida completa" to sinSello.removePrefix("Salida_")
        sinSello.startsWith("Recorrido_") -> "Salida completa" to sinSello.removePrefix("Recorrido_")
        else -> return null
    }
    val coincidencia = recorridos.filter { limpiar(it.nombre) == nombre }.singleOrNull() ?: return null
    return DatosExportacion(coincidencia.id, coincidencia.nombre, contenido)
}

/** "Parada_1_Salida_2026-10-03_153000.pdf" → "Parada 1 Salida". */
private fun nombreLegible(archivo: File): String =
    archivo.nameWithoutExtension
        .replace(Regex("_\\d{4}-\\d{2}-\\d{2}_\\d{6}$"), "")
        .replace('_', ' ')

private fun tamano(archivo: File): String {
    val kb = archivo.length() / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024)
}
