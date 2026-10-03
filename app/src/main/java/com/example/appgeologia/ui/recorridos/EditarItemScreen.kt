package com.example.appgeologia.ui.recorridos

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appgeologia.archivos.AlmacenFotos
import com.example.appgeologia.data.recorrido.ItemParada
import com.example.appgeologia.data.recorrido.OpcionesSuelo
import com.example.appgeologia.data.recorrido.tieneDatosSuelo
import com.example.appgeologia.ui.componentes.CampoConSugerencias
import com.example.appgeologia.ui.componentes.CampoTexto
import com.example.appgeologia.ui.componentes.MiniaturaFoto
import com.example.appgeologia.ui.componentes.SelectorOpciones
import com.example.appgeologia.ui.componentes.TituloSeccion
import com.example.appgeologia.ui.componentes.VisorFoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Formulario de un item de la parada: título, descripción, fotos y los datos de la
 * planilla de suelos. Se guarda solo, igual que la parada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarItemScreen(
    itemGuardado: ItemParada,
    numeroParada: Int,
    almacenFotos: AlmacenFotos,
    onGuardar: (ItemParada) -> Unit,
    onVolver: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var item by remember(itemGuardado.id) { mutableStateOf(itemGuardado) }
    val actual by rememberUpdatedState(item)
    LaunchedEffect(item) {
        delay(500)
        onGuardar(item)
    }
    DisposableEffect(Unit) { onDispose { onGuardar(actual) } }

    var mostrarSuelo by rememberSaveable { mutableStateOf(itemGuardado.tieneDatosSuelo()) }
    var fotoAbierta by remember { mutableStateOf<String?>(null) }
    var fotoAQuitar by remember { mutableStateOf<String?>(null) }
    var procesando by remember { mutableIntStateOf(0) }

    /** Importa en segundo plano y agrega las fotos al item. */
    fun importarFotos(importar: suspend () -> List<String?>) {
        scope.launch {
            procesando++
            val nombres = withContext(Dispatchers.IO) { importar() }
            procesando--
            val validos = nombres.filterNotNull()
            if (validos.isNotEmpty()) item = item.copy(fotos = item.fotos + validos)
            if (validos.size < nombres.size) snackbar.showSnackbar("No se pudo leer alguna de las fotos.")
        }
    }

    // ----- Cámara -----
    // Se guarda la ruta como texto para no perderla si Android cierra la app mientras la cámara está abierta.
    var rutaCamara by rememberSaveable { mutableStateOf<String?>(null) }
    val lanzadorCamara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val temporal = rutaCamara?.let(::File)
        rutaCamara = null
        if (ok && temporal != null) importarFotos { listOf(almacenFotos.importarArchivo(temporal)) }
        else temporal?.delete()
    }
    fun sacarFoto() {
        val (archivo, uri) = almacenFotos.nuevoArchivoCamara()
        rutaCamara = archivo.absolutePath
        try {
            lanzadorCamara.launch(uri)
        } catch (e: ActivityNotFoundException) {
            rutaCamara = null
            scope.launch { snackbar.showSnackbar("No se encontró una app de cámara.") }
        }
    }

    // ----- Galería / archivos -----
    val lanzadorGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) importarFotos { uris.map { almacenFotos.importarUri(it) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(item.titulo.ifBlank { "Item sin título" })
                        Text("Parada $numeroParada", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.Check, contentDescription = "Listo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            TituloSeccion("Observación")
            CampoTexto("Título", item.titulo, { item = item.copy(titulo = it) })
            CampoTexto("Descripción", item.descripcion, { item = item.copy(descripcion = it) }, lineas = 4)

            TituloSeccion("Fotos (${item.fotos.size})")
            if (item.fotos.isNotEmpty() || procesando > 0) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.fotos.forEach { nombre ->
                        Box {
                            MiniaturaFoto(almacenFotos, nombre, tamano = 110, onClick = { fotoAbierta = nombre })
                            // Box en lugar de IconButton: IconButton fuerza 48 dp y tapa la miniatura.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { fotoAQuitar = nombre },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close, contentDescription = "Quitar foto",
                                    tint = Color.White, modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    if (procesando > 0) {
                        Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = ::sacarFoto, modifier = Modifier.weight(1f)) {
                    Text("Tomar foto")
                }
                OutlinedButton(
                    onClick = {
                        lanzadorGaleria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Elegir de archivos")
                }
            }

            // ----- Planilla de suelos (desplegable) -----
            Spacer(Modifier.padding(top = 16.dp))
            Card(modifier = Modifier.fillMaxWidth().clickable { mostrarSuelo = !mostrarSuelo }) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Datos de la planilla de suelos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (item.tieneDatosSuelo()) "Este item aparece como columna en la tabla del PDF."
                            else "Opcional. Completalo si este item describe un horizonte del perfil.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        if (mostrarSuelo) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (mostrarSuelo) "Ocultar" else "Mostrar"
                    )
                }
            }
            if (mostrarSuelo) {
                CamposSuelo(item = item, onCambio = { item = it })
            }
        }
    }

    fotoAbierta?.let { VisorFoto(almacenFotos, it, onCerrar = { fotoAbierta = null }) }

    fotoAQuitar?.let { nombre ->
        AlertDialog(
            onDismissRequest = { fotoAQuitar = null },
            title = { Text("Quitar foto") },
            text = { Text("¿Quitar esta foto del item?") },
            confirmButton = {
                TextButton(onClick = {
                    item = item.copy(fotos = item.fotos - nombre)
                    fotoAQuitar = null
                }) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { fotoAQuitar = null }) { Text("Cancelar") } }
        )
    }
}

/** Los campos de la planilla de suelos (una columna de la tabla del PDF). */
@Composable
private fun CamposSuelo(item: ItemParada, onCambio: (ItemParada) -> Unit) {
    val h = item
    fun cambiar(nuevo: ItemParada) = onCambio(nuevo)

    TituloSeccion("Horizonte")
    CampoTexto("Horizonte (ej. A, Bt1, C)", h.nombre, { cambiar(h.copy(nombre = it)) })
    CampoTexto("Profundidad (cm, ej. 0-15)", h.profundidad, { cambiar(h.copy(profundidad = it)) })

    TituloSeccion("Límite")
    SelectorOpciones("Definición", OpcionesSuelo.limiteDefinicion, h.limiteDefinicion) {
        cambiar(h.copy(limiteDefinicion = it))
    }
    SelectorOpciones("Topografía", OpcionesSuelo.limiteTopografia, h.limiteTopografia) {
        cambiar(h.copy(limiteTopografia = it))
    }

    TituloSeccion("Color (Munsell)")
    CampoTexto("Seco (ej. 10YR 5/3)", h.colorSeco, { cambiar(h.copy(colorSeco = it)) })
    CampoTexto("Húmedo (ej. 10YR 3/2)", h.colorHumedo, { cambiar(h.copy(colorHumedo = it)) })

    TituloSeccion("Textura")
    CampoConSugerencias("Textura al tacto", h.textura, OpcionesSuelo.sugerenciasTextura) {
        cambiar(h.copy(textura = it))
    }

    TituloSeccion("Consistencia")
    CampoConSugerencias("Resistencia", h.resistencia, OpcionesSuelo.sugerenciasResistencia) {
        cambiar(h.copy(resistencia = it))
    }
    SelectorOpciones("Adhesividad", OpcionesSuelo.adhesividad, h.adhesividad) {
        cambiar(h.copy(adhesividad = it))
    }
    SelectorOpciones("Plasticidad", OpcionesSuelo.plasticidad, h.plasticidad) {
        cambiar(h.copy(plasticidad = it))
    }

    TituloSeccion("Estructura")
    CampoConSugerencias("Tipo", h.estructuraTipo, OpcionesSuelo.sugerenciasEstructuraTipo) {
        cambiar(h.copy(estructuraTipo = it))
    }
    SelectorOpciones("Grado", OpcionesSuelo.estructuraGrado, h.estructuraGrado) {
        cambiar(h.copy(estructuraGrado = it))
    }
    CampoConSugerencias("Tamaño", h.estructuraTamano, OpcionesSuelo.sugerenciasTamano) {
        cambiar(h.copy(estructuraTamano = it))
    }

    TituloSeccion("Reacción al HCl")
    SelectorOpciones("Efervescencia", OpcionesSuelo.reaccionHcl, h.reaccionHcl) {
        cambiar(h.copy(reaccionHcl = it))
    }

    TituloSeccion("Concentraciones")
    SelectorOpciones("Tipo", OpcionesSuelo.concentracionTipo, h.concentracionTipo) {
        cambiar(h.copy(concentracionTipo = it))
    }
    CampoConSugerencias("Tamaño", h.concentracionTamano, OpcionesSuelo.sugerenciasTamano) {
        cambiar(h.copy(concentracionTamano = it))
    }
    CampoTexto("Color", h.concentracionColor, { cambiar(h.copy(concentracionColor = it)) })
    CampoConSugerencias("Cantidad", h.concentracionCantidad, OpcionesSuelo.sugerenciasCantidad) {
        cambiar(h.copy(concentracionCantidad = it))
    }
    CampoConSugerencias("Forma", h.concentracionForma, OpcionesSuelo.sugerenciasFormaConcentracion) {
        cambiar(h.copy(concentracionForma = it))
    }

    TituloSeccion("Otros rasgos")
    CampoTexto("Rasgos redox", h.rasgosRedox, { cambiar(h.copy(rasgosRedox = it)) })
    CampoTexto("Moteados", h.moteados, { cambiar(h.copy(moteados = it)) })
    CampoTexto("Barnices", h.barnices, { cambiar(h.copy(barnices = it)) })

    TituloSeccion("Humedad")
    SelectorOpciones("Estado de humedad", OpcionesSuelo.humedad, h.humedad) {
        cambiar(h.copy(humedad = it))
    }

    TituloSeccion("Raíces")
    SelectorOpciones("Cantidad", OpcionesSuelo.raicesCantidad, h.raicesCantidad) {
        cambiar(h.copy(raicesCantidad = it))
    }
    CampoConSugerencias("Tamaño", h.raicesTamano, OpcionesSuelo.sugerenciasTamano) {
        cambiar(h.copy(raicesTamano = it))
    }
    CampoConSugerencias("Ubicación", h.raicesUbicacion, OpcionesSuelo.sugerenciasRaicesUbicacion) {
        cambiar(h.copy(raicesUbicacion = it))
    }

    TituloSeccion("Fragmentos gruesos")
    CampoTexto("Tipo", h.fragmentosTipo, { cambiar(h.copy(fragmentosTipo = it)) })
    CampoTexto("% Volumen", h.fragmentosVolumen, { cambiar(h.copy(fragmentosVolumen = it)) })
    CampoConSugerencias("Redondez", h.fragmentosRedondez, OpcionesSuelo.sugerenciasRedondez) {
        cambiar(h.copy(fragmentosRedondez = it))
    }
    CampoConSugerencias("Tamaño", h.fragmentosTamano, OpcionesSuelo.sugerenciasTamano) {
        cambiar(h.copy(fragmentosTamano = it))
    }
}
