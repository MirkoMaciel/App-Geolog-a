package com.example.appgeologia.ui.recorridos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.data.recorrido.TipoSalida
import com.example.appgeologia.ui.componentes.CampoTexto
import com.example.appgeologia.ui.componentes.formatearFecha
import com.example.appgeologia.ui.componentes.formatearFechaHora

/**
 * Lista de salidas. Con "Seleccionar" (o manteniendo presionada una salida) se entra al
 * modo selección para borrar una o varias salidas de una vez, previa confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaRecorridosScreen(
    recorridos: List<Recorrido>,
    onAbrirRecorrido: (Long) -> Unit,
    onCrearRecorrido: (NuevaSalida) -> Unit,
    onEliminarRecorridos: (List<Long>) -> Unit,
    barraInferior: @Composable () -> Unit
) {
    var mostrarDialogo by rememberSaveable { mutableStateOf(false) }
    var modoSeleccion by rememberSaveable { mutableStateOf(false) }
    var seleccion by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }

    // Si una salida seleccionada ya no existe, se la quita de la selección.
    val seleccionadas = seleccion.filter { id -> recorridos.any { it.id == id } }

    fun salirDeSeleccion() {
        modoSeleccion = false
        seleccion = emptyList()
    }
    fun alternar(id: Long) {
        seleccion = if (id in seleccionadas) seleccionadas - id else seleccionadas + id
    }

    // El botón "atrás" primero cierra el modo selección.
    BackHandler(enabled = modoSeleccion) { salirDeSeleccion() }

    Scaffold(
        topBar = {
            if (modoSeleccion) {
                TopAppBar(
                    title = { Text("${seleccionadas.size} seleccionada(s)") },
                    navigationIcon = {
                        IconButton(onClick = ::salirDeSeleccion) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancelar selección")
                        }
                    },
                    actions = {
                        val todas = seleccionadas.size == recorridos.size
                        TextButton(onClick = { seleccion = if (todas) emptyList() else recorridos.map { it.id } }) {
                            Text(if (todas) "Ninguna" else "Todas", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        IconButton(onClick = { confirmarEliminar = true }, enabled = seleccionadas.isNotEmpty()) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar seleccionadas")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Mis salidas") },
                    actions = {
                        if (recorridos.isNotEmpty()) {
                            TextButton(onClick = { modoSeleccion = true }) {
                                Text("Seleccionar", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        floatingActionButton = {
            if (!modoSeleccion) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarDialogo = true },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva salida de campo") }
                )
            }
        },
        bottomBar = barraInferior
    ) { innerPadding ->
        if (recorridos.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Todavía no hay salidas.\nTocá \"Nueva salida de campo\" para comenzar.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Los más recientes primero
                items(recorridos.sortedByDescending { it.fechaInicio }, key = { it.id }) { recorrido ->
                    TarjetaRecorrido(
                        recorrido = recorrido,
                        modoSeleccion = modoSeleccion,
                        seleccionada = recorrido.id in seleccionadas,
                        onClick = {
                            if (modoSeleccion) alternar(recorrido.id) else onAbrirRecorrido(recorrido.id)
                        },
                        onMantenerPresionado = {
                            // Mantener presionado entra al modo selección con esa salida elegida.
                            if (!modoSeleccion) {
                                modoSeleccion = true
                                seleccion = listOf(recorrido.id)
                            } else {
                                alternar(recorrido.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoNuevoRecorrido(
            onCancelar = { mostrarDialogo = false },
            onCrear = { nueva ->
                mostrarDialogo = false
                onCrearRecorrido(nueva)
            }
        )
    }

    if (confirmarEliminar && seleccionadas.isNotEmpty()) {
        val aBorrar = recorridos.filter { it.id in seleccionadas }
        val paradas = aBorrar.sumOf { it.paradas.size }
        val cantidad = aBorrar.size
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text(if (cantidad == 1) "Eliminar salida" else "Eliminar $cantidad salidas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (cantidad == 1) "¿Realmente querés borrar 1 salida?"
                        else "¿Realmente querés borrar $cantidad salidas?",
                        fontWeight = FontWeight.Bold
                    )
                    aBorrar.take(5).forEach { Text("• ${it.nombre}") }
                    if (cantidad > 5) Text("• y ${cantidad - 5} más")
                    Text(
                        "También se borran sus $paradas parada(s), horizontes y fotos. Los PDF ya " +
                            "exportados se conservan en \"Mis exportaciones\". Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    onEliminarRecorridos(seleccionadas)
                    salirDeSeleccion()
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TarjetaRecorrido(
    recorrido: Recorrido,
    modoSeleccion: Boolean,
    seleccionada: Boolean,
    onClick: () -> Unit,
    onMantenerPresionado: () -> Unit
) {
    Card(
        colors = if (seleccionada) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors(),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onMantenerPresionado)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (modoSeleccion) {
                Checkbox(
                    checked = seleccionada,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        recorrido.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                if (recorrido.finalizado) "Finalizada" else "En curso",
                                color = if (recorrido.finalizado) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
                Text(
                    "${recorrido.tipoSalida.nombre} · ${formatearFecha(recorrido.fechaInicio)} · " +
                        "${recorrido.paradas.size} parada(s)",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (recorrido.proyecto.isNotBlank()) {
                    Text("Proyecto: ${recorrido.proyecto}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * Ventana "Nueva salida de campo". La fecha y hora se toman en el momento en que se
 * abre (cuando se tocó el botón), se muestran sin poder editarse y se guardan como
 * fecha de inicio de la salida.
 */
@Composable
fun DialogoNuevoRecorrido(
    onCancelar: () -> Unit,
    onCrear: (NuevaSalida) -> Unit
) {
    // rememberSaveable: la hora no cambia aunque se gire el teléfono con la ventana abierta.
    val fechaInicio = rememberSaveable { System.currentTimeMillis() }
    var nombre by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf(TipoSalida.SUELOS) }
    var proyecto by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva salida de campo") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Fecha y hora de inicio: solo lectura
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Fecha y hora de inicio", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatearFechaHora(fechaInicio),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                CampoTexto("Nombre salida de campo *", nombre, { nombre = it })

                Text("Tipo de salida", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoSalida.entries.forEach { opcion ->
                        FilterChip(
                            selected = tipo == opcion,
                            onClick = { tipo = opcion },
                            label = { Text(opcion.nombre) }
                        )
                    }
                }

                Text(
                    "El nombre debe ser específico de la salida de campo, ejemplo: Cerro Chenque",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                CampoTexto("Proyecto", proyecto, { proyecto = it }, placeholder = "Ej.: Cerro Chenque")
                CampoTexto("Descripción", descripcion, { descripcion = it }, lineas = 2)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCrear(NuevaSalida(nombre, tipo, proyecto, descripcion, fechaInicio)) },
                enabled = nombre.isNotBlank()
            ) { Text("Iniciar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Lo que se completa en la ventana "Nueva salida de campo". */
data class NuevaSalida(
    val nombre: String,
    val tipo: TipoSalida,
    val proyecto: String,
    val descripcion: String,
    val fechaInicio: Long
)
