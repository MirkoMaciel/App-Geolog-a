package com.example.appgeologia.ui.componentes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appgeologia.data.recorrido.GrupoOpciones
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Título de una sección del formulario (equivale a un grupo de filas de la planilla). */
@Composable
fun TituloSeccion(texto: String) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    teclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1,
    error: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = lineas == 1,
        minLines = lineas,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

/**
 * Selector de una opción de la planilla (por ejemplo "MA – A – C – G – D").
 * Tocar la opción ya elegida la desmarca.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorOpciones(
    etiqueta: String,
    opciones: GrupoOpciones,
    seleccion: String,
    onCambio: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.todas.forEach { opcion ->
                FilterChip(
                    selected = seleccion == opcion.codigo,
                    onClick = { onCambio(if (seleccion == opcion.codigo) "" else opcion.codigo) },
                    label = {
                        Text(
                            if (opcion.descripcion == opcion.codigo) opcion.codigo
                            else "${opcion.codigo} · ${opcion.descripcion}"
                        )
                    }
                )
            }
        }
    }
}

/** Campo de texto libre con sugerencias rápidas debajo (se puede escribir cualquier valor). */
@Composable
fun CampoConSugerencias(
    etiqueta: String,
    valor: String,
    sugerencias: List<String>,
    onCambio: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CampoTexto(etiqueta = etiqueta, valor = valor, onCambio = onCambio)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sugerencias.forEach { sugerencia ->
                AssistChip(onClick = { onCambio(sugerencia) }, label = { Text(sugerencia) })
            }
        }
    }
}

fun formatearFecha(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

fun formatearFechaHora(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

fun formatearHora(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

fun formatearCoordenada(valor: Double?): String =
    valor?.let { String.format(Locale.US, "%.6f", it) } ?: "—"
