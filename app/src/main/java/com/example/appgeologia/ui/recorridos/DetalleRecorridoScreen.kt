package com.example.appgeologia.ui.recorridos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.pdf.DialogoExportarPdf
import com.example.appgeologia.pdf.PlanillaPdf
import com.example.appgeologia.ubicacion.ResultadoUbicacion
import com.example.appgeologia.ubicacion.rememberSolicitudUbicacion
import com.example.appgeologia.ui.componentes.formatearCoordenada
import com.example.appgeologia.ui.componentes.formatearFechaHora
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleRecorridoScreen(
    recorrido: Recorrido,
    viewModel: RecorridosViewModel,
    onAbrirParada: (Long) -> Unit,
    onVolver: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuAbierto by remember { mutableStateOf(false) }
    var dialogoNuevaParada by rememberSaveable { mutableStateOf(false) }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }
    var exportar by rememberSaveable { mutableStateOf(false) }
    // Coordenada elegida manteniendo presionado el mapa, pendiente de confirmar.
    var puntoMapa by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    fun crearYAbrir(lat: Double?, lon: Double?, alt: Double? = null, precision: Float? = null) {
        onAbrirParada(viewModel.crearParada(recorrido.id, lat, lon, alt, precision))
    }

    val ubicacion = rememberSolicitudUbicacion { resultado ->
        when (resultado) {
            is ResultadoUbicacion.Exito -> with(resultado.posicion) {
                crearYAbrir(latitud, longitud, altitud, precisionMetros)
            }
            ResultadoUbicacion.SinPermiso -> scope.launch {
                snackbar.showSnackbar("Sin permiso de ubicación. Podés cargar las coordenadas a mano.")
            }
            ResultadoUbicacion.NoDisponible -> scope.launch {
                snackbar.showSnackbar("No se pudo obtener la ubicación. Revisá que el GPS esté activado.")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(recorrido.nombre)
                        Text(
                            "${recorrido.tipoSalida.nombre} · ${if (recorrido.finalizado) "Finalizado" else "En curso"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { exportar = true }, enabled = recorrido.paradas.isNotEmpty()) {
                        Icon(Icons.Filled.Share, contentDescription = "Exportar recorrido en PDF")
                    }
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text(if (recorrido.finalizado) "Reabrir recorrido" else "Finalizar recorrido") },
                            onClick = {
                                menuAbierto = false
                                viewModel.cambiarEstado(recorrido.id, !recorrido.finalizado)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar recorrido") },
                            onClick = {
                                menuAbierto = false
                                confirmarEliminar = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (!recorrido.finalizado) {
                ExtendedFloatingActionButton(
                    onClick = { dialogoNuevaParada = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva parada") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            MapaRecorrido(
                paradas = recorrido.paradas,
                onParadaClick = { onAbrirParada(it.id) },
                onMantenerPresionado = { lat, lon ->
                    if (!recorrido.finalizado) puntoMapa = lat to lon
                },
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
            Text(
                if (recorrido.finalizado) "Recorrido finalizado. Reabrilo desde el menú para agregar paradas."
                else "Mantené presionado el mapa para crear una parada en ese punto.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (recorrido.paradas.isEmpty()) {
                    item { Text("Todavía no hay paradas en este recorrido.") }
                }
                items(recorrido.paradas.sortedBy { it.numero }, key = { it.id }) { parada ->
                    TarjetaParada(parada, onClick = { onAbrirParada(parada.id) })
                }
            }
        }
    }

    if (dialogoNuevaParada) {
        AlertDialog(
            onDismissRequest = { dialogoNuevaParada = false },
            title = { Text("Nueva parada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Cómo querés registrar la posición de la parada?")
                    Button(
                        onClick = {
                            dialogoNuevaParada = false
                            ubicacion.solicitar()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Usar mi ubicación (GPS)") }
                    OutlinedButton(
                        onClick = {
                            dialogoNuevaParada = false
                            crearYAbrir(null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cargar coordenadas a mano") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { dialogoNuevaParada = false }) { Text("Cancelar") } }
        )
    }

    if (ubicacion.buscando) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Buscando señal GPS…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Esto puede tardar unos segundos al aire libre.")
                }
            },
            confirmButton = {}
        )
    }

    puntoMapa?.let { (lat, lon) ->
        AlertDialog(
            onDismissRequest = { puntoMapa = null },
            title = { Text("Crear parada aquí") },
            text = { Text("Lat: ${formatearCoordenada(lat)}\nLong: ${formatearCoordenada(lon)}") },
            confirmButton = {
                TextButton(onClick = {
                    puntoMapa = null
                    crearYAbrir(lat, lon)
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { puntoMapa = null }) { Text("Cancelar") } }
        )
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("Eliminar recorrido") },
            text = { Text("Se borrarán \"${recorrido.nombre}\" y sus ${recorrido.paradas.size} paradas. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    viewModel.eliminarRecorrido(recorrido.id)
                    onVolver()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } }
        )
    }

    if (exportar) {
        DialogoExportarPdf(
            titulo = "Exportar recorrido",
            generar = { context -> PlanillaPdf(context).generarRecorrido(recorrido) },
            onCerrar = { exportar = false }
        )
    }
}

@Composable
private fun TarjetaParada(parada: Parada, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Parada ${parada.numero}" +
                    parada.sitio.calicata.takeIf { it.isNotBlank() }?.let { " · Calicata $it" }.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (parada.latitud != null && parada.longitud != null)
                    "${formatearCoordenada(parada.latitud)}, ${formatearCoordenada(parada.longitud)}"
                else "Sin coordenadas",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${formatearFechaHora(parada.fecha)} · ${parada.horizontes.size} horizonte(s)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
