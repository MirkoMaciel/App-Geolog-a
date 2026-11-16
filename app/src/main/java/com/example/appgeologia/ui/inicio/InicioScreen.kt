package com.example.appgeologia.ui.inicio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.appgeologia.R
import com.example.appgeologia.data.recorrido.GrupoOpciones
import com.example.appgeologia.data.recorrido.OpcionesSuelo
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.ui.componentes.formatearFecha
import com.example.appgeologia.ui.componentes.formatearFechaHora
import com.example.appgeologia.ui.recorridos.DialogoNuevoRecorrido
import com.example.appgeologia.ui.recorridos.NuevaSalida

/** Pantalla de bienvenida con accesos rápidos. */
@Composable
fun InicioScreen(
    recorridos: List<Recorrido>,
    cantidadExportaciones: Int,
    onCrearRecorrido: (NuevaSalida) -> Unit,
    onAbrirRecorrido: (Long) -> Unit,
    onIrARecorridos: () -> Unit,
    onIrAExportaciones: () -> Unit,
    barraInferior: @Composable () -> Unit
) {
    var dialogoNuevo by rememberSaveable { mutableStateOf(false) }
    var dialogoAyuda by rememberSaveable { mutableStateOf(false) }
    var dialogoGuia by rememberSaveable { mutableStateOf(false) }
    var menuAbierto by rememberSaveable { mutableStateOf(false) }

    val salidasEnCurso = recorridos.filterNot { it.finalizado }
    // La última salida activa: la que tuvo movimiento más reciente (nueva parada o item).
    val ultimaActiva = salidasEnCurso.maxByOrNull { ultimaActividad(it) }

    Scaffold(bottomBar = barraInferior) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ----- Bienvenida -----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Text(
                    "¡Bienvenido/a!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    stringResource(R.string.nombre_completo),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Registrá tus salidas de campo: paradas con posición GPS, observaciones con fotos " +
                        "y la planilla de descripción de suelos, lista para exportar en PDF.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ----- Resumen -----
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Contador("Salidas totales", recorridos.size, Modifier.weight(1f))
                    Contador("Salidas en curso", salidasEnCurso.size, Modifier.weight(1f))
                }

                // ----- Última salida activa -----
                if (ultimaActiva != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Última salida activa", style = MaterialTheme.typography.labelLarge)
                            Text(
                                ultimaActiva.nombre,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Iniciada el ${formatearFecha(ultimaActiva.fechaInicio)} · " +
                                    "${ultimaActiva.paradas.size} parada(s)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Último movimiento: ${formatearFechaHora(ultimaActividad(ultimaActiva))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { onAbrirRecorrido(ultimaActiva.id) }) { Text("Continuar salida") }
                        }
                    }
                }

                // ----- Acción principal -----
                Button(
                    onClick = { dialogoNuevo = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nueva salida de campo", style = MaterialTheme.typography.titleMedium)
                }

                // ----- Más opciones (desplegable) -----
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { menuAbierto = !menuAbierto }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Más opciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (menuAbierto) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (menuAbierto) "Ocultar opciones" else "Mostrar opciones"
                        )
                    }
                    AnimatedVisibility(visible = menuAbierto) {
                        Column {
                            HorizontalDivider()
                            Acceso(Icons.Filled.Place, "Mis salidas", "${recorridos.size} guardada(s)", onIrARecorridos)
                            Acceso(
                                Icons.Filled.Share, "Mis exportaciones",
                                "$cantidadExportaciones PDF generado(s)", onIrAExportaciones
                            )
                            Acceso(Icons.AutoMirrored.Filled.List, "Guía de la planilla", "Qué significa cada abreviatura") {
                                dialogoGuia = true
                            }
                            Acceso(Icons.Filled.Info, "Cómo se usa", "Los pasos de una salida de campo") {
                                dialogoAyuda = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogoNuevo) {
        DialogoNuevoRecorrido(
            onCancelar = { dialogoNuevo = false },
            onCrear = { nueva ->
                dialogoNuevo = false
                onCrearRecorrido(nueva)
            }
        )
    }
    if (dialogoAyuda) DialogoComoSeUsa(onCerrar = { dialogoAyuda = false })
    if (dialogoGuia) DialogoGuiaPlanilla(onCerrar = { dialogoGuia = false })
}

@Composable
private fun Contador(titulo: String, valor: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                valor.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(titulo, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Una opción dentro del menú desplegable "Más opciones". */
@Composable
private fun Acceso(icono: ImageVector, titulo: String, detalle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detalle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Momento del último movimiento de una salida: inicio, última parada o último item cargado. */
private fun ultimaActividad(recorrido: Recorrido): Long =
    maxOf(
        recorrido.fechaInicio,
        recorrido.paradas.maxOfOrNull { parada ->
            maxOf(parada.fecha, parada.items.maxOfOrNull { it.fecha } ?: 0L)
        } ?: 0L
    )

@Composable
private fun DialogoComoSeUsa(onCerrar: () -> Unit) {
    val pasos = listOf(
        "Iniciá una salida con \"Nueva salida de campo\".",
        "En cada lugar donde te detengas, agregá una parada: se guarda la hora y la posición " +
            "(con el GPS, a mano o manteniendo presionado el mapa).",
        "Dentro de la parada tocá \"Añadir nuevo horizonte\": título, descripción y fotos " +
            "(sacadas en el momento o elegidas de los archivos).",
        "Para que el horizonte aparezca en la tabla del PDF, desplegá \"Datos de la planilla de suelos\" y completalo.",
        "En \"Descripción general\" cargá los datos del sitio: calicata, geomorfología, vegetación, etc.",
        "Exportá la parada o la salida completa en PDF. Los archivos quedan en \"Mis exportaciones\"."
    )
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cómo se usa") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pasos.forEachIndexed { i, paso -> Text("${i + 1}. $paso") }
                Text(
                    "Todo se guarda solo en el teléfono y funciona sin conexión. Sin internet, " +
                        "el mapa usa el mapa descargado de la zona de Escalante (Chubut); fuera " +
                        "de esa zona solo se ven las partes que ya se miraron con internet.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido") } }
    )
}

/** Referencia rápida de las abreviaturas de la planilla, para consultar en el campo. */
@Composable
private fun DialogoGuiaPlanilla(onCerrar: () -> Unit) {
    val grupos: List<Pair<String, GrupoOpciones>> = listOf(
        "Límite: definición" to OpcionesSuelo.limiteDefinicion,
        "Límite: topografía" to OpcionesSuelo.limiteTopografia,
        "Estructura: grado" to OpcionesSuelo.estructuraGrado,
        "Reacción al HCl" to OpcionesSuelo.reaccionHcl,
        "Concentraciones: tipo" to OpcionesSuelo.concentracionTipo,
        "Humedad" to OpcionesSuelo.humedad,
        "Raíces: cantidad" to OpcionesSuelo.raicesCantidad
    )
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Guía de la planilla") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                grupos.forEach { (titulo, grupo) ->
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    grupo.todas.filter { it.codigo != it.descripcion }.forEach { opcion ->
                        Text("${opcion.codigo}: ${opcion.descripcion}", style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
