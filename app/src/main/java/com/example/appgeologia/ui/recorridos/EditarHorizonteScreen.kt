package com.example.appgeologia.ui.recorridos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appgeologia.data.recorrido.Horizonte
import com.example.appgeologia.data.recorrido.OpcionesSuelo
import com.example.appgeologia.ui.componentes.CampoConSugerencias
import com.example.appgeologia.ui.componentes.CampoTexto
import com.example.appgeologia.ui.componentes.SelectorOpciones
import com.example.appgeologia.ui.componentes.TituloSeccion
import kotlinx.coroutines.delay

/**
 * Formulario de un horizonte: equivale a una columna de la tabla de la planilla.
 * Se guarda solo, igual que la parada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarHorizonteScreen(
    horizonteGuardado: Horizonte,
    numeroParada: Int,
    onGuardar: (Horizonte) -> Unit,
    onVolver: () -> Unit
) {
    var h by remember(horizonteGuardado.id) { mutableStateOf(horizonteGuardado) }
    val actual by rememberUpdatedState(h)
    LaunchedEffect(h) {
        delay(500)
        onGuardar(h)
    }
    DisposableEffect(Unit) { onDispose { onGuardar(actual) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Horizonte ${h.nombre}".trim())
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            TituloSeccion("Identificación")
            CampoTexto("Horizonte (ej. A, Bt1, C)", h.nombre, { h = h.copy(nombre = it) })
            CampoTexto("Profundidad (cm, ej. 0-15)", h.profundidad, { h = h.copy(profundidad = it) })

            TituloSeccion("Límite")
            SelectorOpciones("Definición", OpcionesSuelo.limiteDefinicion, h.limiteDefinicion) {
                h = h.copy(limiteDefinicion = it)
            }
            SelectorOpciones("Topografía", OpcionesSuelo.limiteTopografia, h.limiteTopografia) {
                h = h.copy(limiteTopografia = it)
            }

            TituloSeccion("Color (Munsell)")
            CampoTexto("Seco (ej. 10YR 5/3)", h.colorSeco, { h = h.copy(colorSeco = it) })
            CampoTexto("Húmedo (ej. 10YR 3/2)", h.colorHumedo, { h = h.copy(colorHumedo = it) })

            TituloSeccion("Textura")
            CampoConSugerencias("Textura al tacto", h.textura, OpcionesSuelo.sugerenciasTextura) {
                h = h.copy(textura = it)
            }

            TituloSeccion("Consistencia")
            CampoConSugerencias("Resistencia", h.resistencia, OpcionesSuelo.sugerenciasResistencia) {
                h = h.copy(resistencia = it)
            }
            SelectorOpciones("Adhesividad", OpcionesSuelo.adhesividad, h.adhesividad) {
                h = h.copy(adhesividad = it)
            }
            SelectorOpciones("Plasticidad", OpcionesSuelo.plasticidad, h.plasticidad) {
                h = h.copy(plasticidad = it)
            }

            TituloSeccion("Estructura")
            CampoConSugerencias("Tipo", h.estructuraTipo, OpcionesSuelo.sugerenciasEstructuraTipo) {
                h = h.copy(estructuraTipo = it)
            }
            SelectorOpciones("Grado", OpcionesSuelo.estructuraGrado, h.estructuraGrado) {
                h = h.copy(estructuraGrado = it)
            }
            CampoConSugerencias("Tamaño", h.estructuraTamano, OpcionesSuelo.sugerenciasTamano) {
                h = h.copy(estructuraTamano = it)
            }

            TituloSeccion("Reacción al HCl")
            SelectorOpciones("Efervescencia", OpcionesSuelo.reaccionHcl, h.reaccionHcl) {
                h = h.copy(reaccionHcl = it)
            }

            TituloSeccion("Concentraciones")
            SelectorOpciones("Tipo", OpcionesSuelo.concentracionTipo, h.concentracionTipo) {
                h = h.copy(concentracionTipo = it)
            }
            CampoConSugerencias("Tamaño", h.concentracionTamano, OpcionesSuelo.sugerenciasTamano) {
                h = h.copy(concentracionTamano = it)
            }
            CampoTexto("Color", h.concentracionColor, { h = h.copy(concentracionColor = it) })
            CampoConSugerencias("Cantidad", h.concentracionCantidad, OpcionesSuelo.sugerenciasCantidad) {
                h = h.copy(concentracionCantidad = it)
            }
            CampoConSugerencias("Forma", h.concentracionForma, OpcionesSuelo.sugerenciasFormaConcentracion) {
                h = h.copy(concentracionForma = it)
            }

            TituloSeccion("Otros rasgos")
            CampoTexto("Rasgos redox", h.rasgosRedox, { h = h.copy(rasgosRedox = it) })
            CampoTexto("Moteados", h.moteados, { h = h.copy(moteados = it) })
            CampoTexto("Barnices", h.barnices, { h = h.copy(barnices = it) })

            TituloSeccion("Humedad")
            SelectorOpciones("Estado de humedad", OpcionesSuelo.humedad, h.humedad) {
                h = h.copy(humedad = it)
            }

            TituloSeccion("Raíces")
            SelectorOpciones("Cantidad", OpcionesSuelo.raicesCantidad, h.raicesCantidad) {
                h = h.copy(raicesCantidad = it)
            }
            CampoConSugerencias("Tamaño", h.raicesTamano, OpcionesSuelo.sugerenciasTamano) {
                h = h.copy(raicesTamano = it)
            }
            CampoConSugerencias("Ubicación", h.raicesUbicacion, OpcionesSuelo.sugerenciasRaicesUbicacion) {
                h = h.copy(raicesUbicacion = it)
            }

            TituloSeccion("Fragmentos gruesos")
            CampoTexto("Tipo", h.fragmentosTipo, { h = h.copy(fragmentosTipo = it) })
            CampoTexto("% Volumen", h.fragmentosVolumen, { h = h.copy(fragmentosVolumen = it) })
            CampoConSugerencias("Redondez", h.fragmentosRedondez, OpcionesSuelo.sugerenciasRedondez) {
                h = h.copy(fragmentosRedondez = it)
            }
            CampoConSugerencias("Tamaño", h.fragmentosTamano, OpcionesSuelo.sugerenciasTamano) {
                h = h.copy(fragmentosTamano = it)
            }
        }
    }
}
