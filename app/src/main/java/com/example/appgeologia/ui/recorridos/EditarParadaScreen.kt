package com.example.appgeologia.ui.recorridos

import com.example.appgeologia.ui.componentes.SeccionDesplegable
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appgeologia.data.recorrido.DatosSitio
import com.example.appgeologia.archivos.AlmacenFotos
import com.example.appgeologia.data.recorrido.ItemParada
import com.example.appgeologia.data.recorrido.tieneDatosSuelo
import com.example.appgeologia.ui.componentes.MiniaturaFoto
import com.example.appgeologia.data.recorrido.OpcionesSuelo
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.pdf.DialogoExportarPdf
import com.example.appgeologia.pdf.PlanillaPdf
import com.example.appgeologia.ubicacion.ResultadoUbicacion
import com.example.appgeologia.ubicacion.rememberSolicitudUbicacion
import com.example.appgeologia.ui.componentes.CampoConSugerencias
import com.example.appgeologia.ui.componentes.CampoTexto
import com.example.appgeologia.ui.componentes.TituloSeccion
import com.example.appgeologia.ui.componentes.formatearCoordenada
import com.example.appgeologia.ui.componentes.formatearFechaHora
import com.example.appgeologia.ui.componentes.formatearHora
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Planilla de una parada. Los cambios se guardan solos (medio segundo después de dejar
 * de escribir y al salir de la pantalla), para no perder datos en el campo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarParadaScreen(
    recorrido: Recorrido,
    paradaGuardada: Parada,
    viewModel: RecorridosViewModel,
    onEditarItem: (Long) -> Unit,
    onVolver: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var parada by remember(paradaGuardada.id) { mutableStateOf(paradaGuardada) }
    var descripcionAbierta by rememberSaveable { mutableStateOf(false) }
    var horizontesAbiertos by rememberSaveable { mutableStateOf(true) }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }
    var exportar by rememberSaveable { mutableStateOf(false) }
    var eliminada by remember { mutableStateOf(false) }

    // ----- Guardado automático -----
    val paradaActual by rememberUpdatedState(parada)
    fun guardar() {
        if (!eliminada) viewModel.guardarParada(recorrido.id, paradaActual)
    }
    LaunchedEffect(parada) {
        delay(500)
        guardar()
    }
    DisposableEffect(Unit) { onDispose { guardar() } }

    // ----- Coordenadas (se editan como texto y se validan) -----
    var latTexto by remember(paradaGuardada.id) { mutableStateOf(textoCoordenada(paradaGuardada.latitud)) }
    var lonTexto by remember(paradaGuardada.id) { mutableStateOf(textoCoordenada(paradaGuardada.longitud)) }
    var altTexto by remember(paradaGuardada.id) { mutableStateOf(paradaGuardada.altitud?.let { "%.0f".format(it) } ?: "") }

    val ubicacion = rememberSolicitudUbicacion { resultado ->
        when (resultado) {
            is ResultadoUbicacion.Exito -> with(resultado.posicion) {
                latTexto = textoCoordenada(latitud)
                lonTexto = textoCoordenada(longitud)
                altTexto = altitud?.let { "%.0f".format(it) } ?: altTexto
                parada = parada.copy(
                    latitud = latitud,
                    longitud = longitud,
                    altitud = altitud ?: parada.altitud,
                    precisionMetros = precisionMetros
                )
            }
            ResultadoUbicacion.SinPermiso -> scope.launch { snackbar.showSnackbar("Sin permiso de ubicación.") }
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
                        Text("Parada ${parada.numero}")
                        Text(recorrido.nombre, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        guardar()
                        exportar = true
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Exportar parada en PDF")
                    }
                    IconButton(onClick = { confirmarEliminar = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar parada")
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
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ----- Descripción general (desplegable, campos de texto libre) -----
            SeccionDesplegable(
                titulo = "Descripción general",
                abierta = descripcionAbierta,
                onCambiar = { descripcionAbierta = it },
                icono = Icons.Filled.Edit,
                detalle = "Ubicación, datos del sitio y observaciones"
            ) {
                DescripcionGeneral(
                    parada = parada,
                    latTexto = latTexto,
                    lonTexto = lonTexto,
                    altTexto = altTexto,
                    onLat = { texto ->
                        latTexto = texto
                        parada = parada.copy(latitud = parsearCoordenada(texto, 90.0), precisionMetros = null)
                    },
                    onLon = { texto ->
                        lonTexto = texto
                        parada = parada.copy(longitud = parsearCoordenada(texto, 180.0), precisionMetros = null)
                    },
                    onAlt = { texto ->
                        altTexto = texto
                        parada = parada.copy(altitud = texto.replace(',', '.').toDoubleOrNull())
                    },
                    buscandoGps = ubicacion.buscando,
                    onUsarGps = ubicacion.solicitar,
                    onSitio = { cambio -> parada = parada.copy(sitio = cambio(parada.sitio)) },
                    onObservaciones = { parada = parada.copy(observaciones = it) }
                )
            }

            // ----- Horizontes -----
            Button(
                onClick = {
                    guardar()
                    horizontesAbiertos = true
                    onEditarItem(viewModel.agregarItem(recorrido.id, parada.id))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Añadir nuevo horizonte")
            }

            // El desplegable aparece recién cuando hay al menos un horizonte.
            if (paradaGuardada.items.isNotEmpty()) {
                SeccionDesplegable(
                    titulo = "Horizontes (${paradaGuardada.items.size})",
                    abierta = horizontesAbiertos,
                    onCambiar = { horizontesAbiertos = it },
                    icono = Icons.AutoMirrored.Filled.List
                ) {
                    ListaHorizontes(
                        items = paradaGuardada.items,
                        almacenFotos = viewModel.fotos,
                        onAbrir = { id ->
                            guardar()
                            onEditarItem(id)
                        },
                        onEliminar = { id -> viewModel.eliminarItem(recorrido.id, parada.id, id) }
                    )
                }
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("Eliminar parada ${parada.numero}") },
            text = { Text("Se borrará la parada con todos sus horizontes y fotos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    eliminada = true
                    viewModel.eliminarParada(recorrido.id, parada.id)
                    onVolver()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } }
        )
    }

    if (exportar) {
        DialogoExportarPdf(
            titulo = "Exportar parada ${parada.numero}",
            generar = { context ->
                // Se usa la versión en pantalla (con los horizontes guardados).
                PlanillaPdf(context).generarParada(recorrido, paradaActual.copy(items = paradaGuardada.items))
            },
            onCerrar = { exportar = false }
        )
    }
}

@Composable
private fun DescripcionGeneral(
    parada: Parada,
    latTexto: String,
    lonTexto: String,
    altTexto: String,
    onLat: (String) -> Unit,
    onLon: (String) -> Unit,
    onAlt: (String) -> Unit,
    buscandoGps: Boolean,
    onUsarGps: () -> Unit,
    onSitio: ((DatosSitio) -> DatosSitio) -> Unit,
    onObservaciones: (String) -> Unit
) {
    val s = parada.sitio
    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        TituloSeccion("Ubicación")
        Text("Fecha: ${formatearFechaHora(parada.fecha)}", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto(
                "Latitud", latTexto, onLat,
                modifier = Modifier.weight(1f),
                teclado = KeyboardType.Decimal,
                error = if (latTexto.isNotBlank() && parada.latitud == null) "Entre -90 y 90" else null
            )
            CampoTexto(
                "Longitud", lonTexto, onLon,
                modifier = Modifier.weight(1f),
                teclado = KeyboardType.Decimal,
                error = if (lonTexto.isNotBlank() && parada.longitud == null) "Entre -180 y 180" else null
            )
        }
        CampoTexto("Altitud (m s.n.m.)", altTexto, onAlt, teclado = KeyboardType.Decimal)
        parada.precisionMetros?.let {
            Text("Precisión del GPS: ±%.0f m".format(it), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(onClick = onUsarGps, enabled = !buscandoGps, modifier = Modifier.fillMaxWidth()) {
            if (buscandoGps) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Buscando señal GPS…")
            } else {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Usar mi ubicación actual")
            }
        }

        TituloSeccion("Datos generales")
        CampoTexto("Calicata", s.calicata, { v -> onSitio { it.copy(calicata = v) } })
        CampoTexto("Sitio de referencia", s.sitioReferencia, { v -> onSitio { it.copy(sitioReferencia = v) } })
        CampoTexto("Proyecto", s.proyecto, { v -> onSitio { it.copy(proyecto = v) } })

        TituloSeccion("Geomorfología y relieve")
        CampoTexto("Geomorfología", s.geomorfologia, { v -> onSitio { it.copy(geomorfologia = v) } })
        CampoTexto("Posición geomórfica", s.posicionGeomorfica, { v -> onSitio { it.copy(posicionGeomorfica = v) } })
        CampoConSugerencias("Relieve", s.relieve, OpcionesSuelo.sugerenciasRelieve) { v ->
            onSitio { it.copy(relieve = v) }
        }
        CampoConSugerencias("Exposición", s.exposicion, OpcionesSuelo.sugerenciasExposicion) { v ->
            onSitio { it.copy(exposicion = v) }
        }
        CampoTexto("Pendiente (%)", s.pendiente, { v -> onSitio { it.copy(pendiente = v) } })
        CampoTexto(
            "Material originario / Sustrato", s.materialOriginario,
            { v -> onSitio { it.copy(materialOriginario = v) } }
        )

        TituloSeccion("Vegetación y uso")
        CampoTexto("Vegetación", s.vegetacion, { v -> onSitio { it.copy(vegetacion = v) } }, lineas = 2)
        CampoTexto("Cobertura vegetal (%)", s.coberturaVegetal, { v -> onSitio { it.copy(coberturaVegetal = v) } })
        CampoTexto("Uso de la tierra", s.usoTierra, { v -> onSitio { it.copy(usoTierra = v) } })

        TituloSeccion("Drenaje y superficie")
        CampoConSugerencias("Clase de drenaje", s.claseDrenaje, OpcionesSuelo.sugerenciasClaseDrenaje) { v ->
            onSitio { it.copy(claseDrenaje = v) }
        }
        CampoTexto("Anegamiento", s.anegamiento, { v -> onSitio { it.copy(anegamiento = v) } })
        CampoConSugerencias("Erosión", s.erosion, OpcionesSuelo.sugerenciasErosion) { v ->
            onSitio { it.copy(erosion = v) }
        }
        CampoTexto("Sales / Álcalis", s.salesAlcalis, { v -> onSitio { it.copy(salesAlcalis = v) } })
        CampoTexto("Pedregosidad", s.pedregosidad, { v -> onSitio { it.copy(pedregosidad = v) } })

        TituloSeccion("Clasificación")
        CampoTexto("Clasificación", s.clasificacion, { v -> onSitio { it.copy(clasificacion = v) } })
        CampoTexto("Temperatura", s.temperatura, { v -> onSitio { it.copy(temperatura = v) } })

        TituloSeccion("Observaciones")
        CampoTexto("Observaciones", parada.observaciones, onObservaciones, lineas = 3)
    }
}

/** Lista de horizontes de la parada (dentro del desplegable "Horizontes"). */
@Composable
private fun ListaHorizontes(
    items: List<ItemParada>,
    almacenFotos: AlmacenFotos,
    onAbrir: (Long) -> Unit,
    onEliminar: (Long) -> Unit
) {
    var aEliminar by remember { mutableStateOf<ItemParada?>(null) }

    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { indice, item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().clickable { onAbrir(item.id) }
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.fotos.firstOrNull()?.let { foto ->
                        MiniaturaFoto(almacenFotos, foto, tamano = 64)
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.titulo.ifBlank { "Horizonte ${indice + 1} (sin título)" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.descripcion.isNotBlank()) {
                            Text(
                                item.descripcion,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            listOfNotNull(
                                item.fecha.takeIf { it > 0 }?.let { formatearHora(it) },
                                "${item.fotos.size} foto(s)",
                                "Planilla de suelos".takeIf { item.tieneDatosSuelo() }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { aEliminar = item }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar horizonte")
                    }
                }
            }
        }
    }

    aEliminar?.let { item ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar horizonte") },
            text = { Text("¿Eliminar \"${item.titulo.ifBlank { "sin título" }}\" y sus ${item.fotos.size} foto(s)?") },
            confirmButton = {
                TextButton(onClick = {
                    onEliminar(item.id)
                    aEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { aEliminar = null }) { Text("Cancelar") } }
        )
    }
}

private fun textoCoordenada(valor: Double?): String = valor?.let { formatearCoordenada(it) } ?: ""

/** Acepta coma o punto decimal; devuelve null si no es un número dentro de ±[limite]. */
private fun parsearCoordenada(texto: String, limite: Double): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in -limite..limite }
