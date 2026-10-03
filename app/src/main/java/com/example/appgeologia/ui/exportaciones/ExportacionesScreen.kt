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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appgeologia.archivos.AlmacenExportaciones
import com.example.appgeologia.archivos.compartirPdf
import com.example.appgeologia.archivos.verPdf
import com.example.appgeologia.ui.componentes.formatearFechaHora
import java.io.File

/** Historial de los PDF generados: verlos, compartirlos o borrarlos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportacionesScreen(barraInferior: @Composable () -> Unit) {
    val context = LocalContext.current
    val almacen = remember { AlmacenExportaciones(context) }
    var archivos by remember { mutableStateOf(almacen.listar()) }
    var aEliminar by remember { mutableStateOf<File?>(null) }

    // Se vuelve a leer la carpeta cada vez que se entra a la pantalla.
    LaunchedEffect(Unit) { archivos = almacen.listar() }

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
        if (archivos.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Todavía no exportaste ningún PDF.\nAbrí una parada o un recorrido y tocá el ícono de compartir.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(archivos, key = { it.name }) { archivo ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { verPdf(context, archivo) }) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    nombreLegible(archivo),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${formatearFechaHora(archivo.lastModified())} · ${tamano(archivo)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { compartirPdf(context, archivo) }) {
                                Icon(Icons.Filled.Share, contentDescription = "Compartir")
                            }
                            IconButton(onClick = { aEliminar = archivo }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
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
            text = { Text("¿Eliminar \"${archivo.name}\"? Los datos del recorrido no se borran.") },
            confirmButton = {
                TextButton(onClick = {
                    archivo.delete()
                    archivos = almacen.listar()
                    aEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
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
