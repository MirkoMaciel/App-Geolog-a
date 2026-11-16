package com.example.appgeologia.ui.exportaciones

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appgeologia.archivos.AlmacenExportaciones
import com.example.appgeologia.archivos.Exportacion
import com.example.appgeologia.archivos.compartirPdf
import com.example.appgeologia.archivos.verPdf
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.pdf.regenerarPdf
import com.example.appgeologia.ui.componentes.SeccionDesplegable
import com.example.appgeologia.ui.componentes.formatearFechaHora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Los PDF de una misma salida, como se muestran en un desplegable. */
private class GrupoExportaciones(
    val clave: String,
    val titulo: String,
    val exportaciones: List<Exportacion>
)

/** Estado de un PDF respecto de los datos actuales de la app. */
private enum class EstadoPdf(val texto: String) {
    AL_DIA("Al día"),
    DESACTUALIZADO("Desactualizado: la parada o la salida cambió desde que se exportó"),
    COPIA("Copia guardada aparte"),
    SIN_DATOS("Su parada o salida ya no existe")
}

/**
 * PDFs exportados, agrupados por salida en desplegables. Cada parada y cada salida tiene
 * un solo PDF vigente: se marca si está al día o si quedó desactualizado (y se puede
 * actualizar en el lugar). Las copias guardadas aparte se muestran como "Copia".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportacionesScreen(recorridos: List<Recorrido>, barraInferior: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val almacen = remember { AlmacenExportaciones(context) }
    var exportaciones by remember { mutableStateOf(almacen.listarConDatos()) }
    var aEliminar by remember { mutableStateOf<File?>(null) }
    var actualizando by remember { mutableStateOf<File?>(null) }
    // Claves de los grupos desplegados (se recuerdan al girar la pantalla).
    var abiertos by rememberSaveable { mutableStateOf(listOf<String>()) }

    // Se vuelve a leer la carpeta cada vez que se entra a la pantalla.
    LaunchedEffect(Unit) { exportaciones = withContext(Dispatchers.IO) { almacen.listarConDatos() } }

    val grupos = agrupar(exportaciones, recorridos)

    fun actualizar(exportacion: Exportacion, recorrido: Recorrido, parada: Parada?) {
        actualizando = exportacion.archivo
        scope.launch {
            try {
                withContext(Dispatchers.Default) { regenerarPdf(context, almacen, recorrido, parada) }
                Toast.makeText(context, "PDF actualizado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo actualizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
            exportaciones = withContext(Dispatchers.IO) { almacen.listarConDatos() }
            actualizando = null
        }
    }

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
                    val desactualizados = grupo.exportaciones.count { estadoDe(almacen, it, recorridos).first == EstadoPdf.DESACTUALIZADO }
                    SeccionDesplegable(
                        titulo = grupo.titulo,
                        abierta = grupo.clave in abiertos,
                        onCambiar = { abrir -> abiertos = if (abrir) abiertos + grupo.clave else abiertos - grupo.clave },
                        icono = Icons.Filled.Place,
                        detalle = "${grupo.exportaciones.size} PDF" +
                            (if (desactualizados > 0) " · $desactualizados desactualizado(s)" else "")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grupo.exportaciones.forEach { exportacion ->
                                val (estado, origen) = estadoDe(almacen, exportacion, recorridos)
                                TarjetaPdf(
                                    exportacion = exportacion,
                                    estado = estado,
                                    actualizando = actualizando == exportacion.archivo,
                                    onVer = { verPdf(context, exportacion.archivo) },
                                    onActualizar = origen?.let { (r, p) -> { actualizar(exportacion, r, p) } },
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

/**
 * Estado del PDF y, si está desactualizado, la salida y parada para regenerarlo.
 * Un PDF está al día si se generó después de la última modificación de su parada/salida.
 */
private fun estadoDe(
    almacen: AlmacenExportaciones,
    exportacion: Exportacion,
    recorridos: List<Recorrido>
): Pair<EstadoPdf, Pair<Recorrido, Parada?>?> {
    val datos = exportacion.datos ?: return EstadoPdf.SIN_DATOS to null
    if (datos.copia) return EstadoPdf.COPIA to null
    val recorrido = recorridos.find { it.id == datos.recorridoId } ?: return EstadoPdf.SIN_DATOS to null
    val parada = datos.paradaId?.let { id -> recorrido.paradas.find { it.id == id } ?: return EstadoPdf.SIN_DATOS to null }
    val modificado = parada?.modificado ?: recorrido.modificado
    return if (almacen.estaAlDia(exportacion, modificado)) EstadoPdf.AL_DIA to null
    else EstadoPdf.DESACTUALIZADO to (recorrido to parada)
}

@Composable
private fun TarjetaPdf(
    exportacion: Exportacion,
    estado: EstadoPdf,
    actualizando: Boolean,
    onVer: () -> Unit,
    onActualizar: (() -> Unit)?,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit
) {
    val archivo = exportacion.archivo
    val colorEstado: Color = when (estado) {
        EstadoPdf.AL_DIA -> MaterialTheme.colorScheme.secondary
        EstadoPdf.DESACTUALIZADO -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
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
                Text(estado.texto, style = MaterialTheme.typography.labelMedium, color = colorEstado, fontWeight = FontWeight.Bold)
            }
            if (actualizando) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(24.dp))
            } else if (onActualizar != null) {
                IconButton(onClick = onActualizar) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar PDF", tint = MaterialTheme.colorScheme.tertiary)
                }
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
    val (conSalida, sinSalida) = exportaciones.partition { it.datos != null }

    val grupos = conSalida
        .groupBy { it.datos!!.recorridoId }
        .map { (id, lista) ->
            val titulo = nombresActuales[id] ?: "${lista.first().datos!!.nombreSalida} (salida eliminada)"
            // Primero la salida completa, después las paradas en orden, al final las copias.
            val ordenados = lista.sortedWith(
                compareBy<Exportacion>({ it.datos!!.copia }, { it.datos!!.paradaId != null }, { it.datos!!.contenido.length }, { it.datos!!.contenido })
            )
            GrupoExportaciones("salida-$id", titulo, ordenados)
        }
        .sortedByDescending { g -> g.exportaciones.maxOf { it.archivo.lastModified() } }

    return if (sinSalida.isEmpty()) grupos
    else grupos + GrupoExportaciones("otras", "Otras exportaciones", sinSalida)
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
