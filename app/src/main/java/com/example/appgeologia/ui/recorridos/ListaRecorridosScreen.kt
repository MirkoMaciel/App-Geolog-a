package com.example.appgeologia.ui.recorridos

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaRecorridosScreen(
    recorridos: List<Recorrido>,
    onAbrirRecorrido: (Long) -> Unit,
    onCrearRecorrido: (nombre: String, tipo: TipoSalida, proyecto: String, descripcion: String) -> Unit,
    barraInferior: @Composable () -> Unit
) {
    var mostrarDialogo by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis recorridos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogo = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Iniciar recorrido") }
            )
        },
        bottomBar = barraInferior
    ) { innerPadding ->
        if (recorridos.isEmpty()) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Todavía no hay recorridos.\nTocá \"Iniciar recorrido\" para comenzar una salida de campo.",
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
                    TarjetaRecorrido(recorrido, onClick = { onAbrirRecorrido(recorrido.id) })
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoNuevoRecorrido(
            onCancelar = { mostrarDialogo = false },
            onCrear = { nombre, tipo, proyecto, descripcion ->
                mostrarDialogo = false
                onCrearRecorrido(nombre, tipo, proyecto, descripcion)
            }
        )
    }
}

@Composable
private fun TarjetaRecorrido(recorrido: Recorrido, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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
                    label = { Text(if (recorrido.finalizado) "Finalizado" else "En curso") }
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

@Composable
fun DialogoNuevoRecorrido(
    onCancelar: () -> Unit,
    onCrear: (nombre: String, tipo: TipoSalida, proyecto: String, descripcion: String) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf(TipoSalida.SUELOS) }
    var proyecto by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo recorrido") },
        text = {
            Column {
                CampoTexto("Nombre del recorrido *", nombre, { nombre = it })
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
                CampoTexto("Proyecto", proyecto, { proyecto = it })
                CampoTexto("Descripción", descripcion, { descripcion = it }, lineas = 2)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCrear(nombre, tipo, proyecto, descripcion) },
                enabled = nombre.isNotBlank()
            ) { Text("Iniciar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
