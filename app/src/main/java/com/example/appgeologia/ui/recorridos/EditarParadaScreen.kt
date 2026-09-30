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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appgeologia.data.recorrido.DatosSitio
import com.example.appgeologia.data.recorrido.Horizonte
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
    onEditarHorizonte: (Long) -> Unit,
    onVolver: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var parada by remember(paradaGuardada.id) { mutableStateOf(paradaGuardada) }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
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
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = pestana) {
                Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Sitio") })
                Tab(
                    selected = pestana == 1,
                    onClick = { pestana = 1 },
                    text = { Text("Horizontes (${paradaGuardada.horizontes.size})") }
                )
            }

            if (pestana == 0) {
                PestanaSitio(
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
            } else {
                PestanaHorizontes(
                    horizontes = paradaGuardada.horizontes,
                    onAbrir = { id ->
                        guardar()
                        onEditarHorizonte(id)
                    },
                    onAgregar = {
                        guardar()
                        onEditarHorizonte(viewModel.agregarHorizonte(recorrido.id, parada.id))
                    },
                    onEliminar = { id -> viewModel.eliminarHorizonte(recorrido.id, parada.id, id) }
                )
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("Eliminar parada ${parada.numero}") },
            text = { Text("Se borrará la parada con todos sus horizontes. Esta acción no se puede deshacer.") },
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
                PlanillaPdf(context).generarParada(recorrido, paradaActual.copy(horizontes = paradaGuardada.horizontes))
            },
            onCerrar = { exportar = false }
        )
    }
}

@Composable
private fun PestanaSitio(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
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

@Composable
private fun PestanaHorizontes(
    horizontes: List<Horizonte>,
    onAbrir: (Long) -> Unit,
    onAgregar: () -> Unit,
    onEliminar: (Long) -> Unit
) {
    var aEliminar by remember { mutableStateOf<Horizonte?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Cargá los horizontes del perfil de arriba hacia abajo. En el PDF se imprimen 4 por página.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        itemsIndexed(horizontes, key = { _, h -> h.id }) { indice, horizonte ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onAbrir(horizonte.id) }) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            horizonte.nombre.ifBlank { "Horizonte ${indice + 1} (sin nombre)" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            listOfNotNull(
                                horizonte.profundidad.takeIf { it.isNotBlank() }?.let { "$it cm" },
                                horizonte.textura.takeIf { it.isNotBlank() },
                                horizonte.colorHumedo.takeIf { it.isNotBlank() }
                            ).joinToString(" · ").ifEmpty { "Tocá para completar" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { aEliminar = horizonte }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar horizonte")
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAgregar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar horizonte")
            }
        }
    }

    aEliminar?.let { horizonte ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            title = { Text("Eliminar horizonte") },
            text = { Text("¿Eliminar el horizonte ${horizonte.nombre.ifBlank { "sin nombre" }}?") },
            confirmButton = {
                TextButton(onClick = {
                    onEliminar(horizonte.id)
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
