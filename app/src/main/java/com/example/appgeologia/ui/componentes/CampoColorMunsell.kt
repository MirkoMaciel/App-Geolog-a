package com.example.appgeologia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.appgeologia.data.recorrido.ColoresMunsell

private fun colorDe(chip: ColoresMunsell.Chip) = Color(0xFF000000 or chip.rgb.toLong())

/**
 * Campo de color Munsell: se puede escribir la notación a mano o elegirla en la tabla
 * tocando el matiz (10YR o 7.5YR). Si el texto corresponde a un color de la tabla, se
 * muestra una muestra del color debajo. En el PDF se imprime solo el texto.
 */
@Composable
fun CampoColorMunsell(etiqueta: String, valor: String, onCambio: (String) -> Unit) {
    val chip = ColoresMunsell.buscar(valor)
    var tablaAbierta by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        CampoTexto(etiqueta, valor, onCambio, placeholder = "Ej.: 10YR 5/3")

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Elegir en la tabla:", style = MaterialTheme.typography.bodySmall)
            ColoresMunsell.matices.forEach { matiz ->
                FilterChip(
                    selected = chip?.matiz == matiz,
                    onClick = { tablaAbierta = matiz },
                    label = { Text(matiz) }
                )
            }
        }

        if (chip != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorDe(chip))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(chip.notacion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Matiz ${chip.matiz} · Luminosidad ${chip.luminosidad} · Croma ${chip.croma}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else if (valor.isNotBlank()) {
            Text(
                "No es un color de las tablas 10YR / 7.5YR: se guarda igual como texto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    tablaAbierta?.let { matiz ->
        DialogoTablaMunsell(
            matizInicial = matiz,
            seleccionado = chip,
            onElegir = { elegido ->
                onCambio(elegido.notacion)
                tablaAbierta = null
            },
            onCerrar = { tablaAbierta = null }
        )
    }
}

/** La página del libro Munsell de un matiz: filas de luminosidad y columnas de croma. */
@Composable
private fun DialogoTablaMunsell(
    matizInicial: String,
    seleccionado: ColoresMunsell.Chip?,
    onElegir: (ColoresMunsell.Chip) -> Unit,
    onCerrar: () -> Unit
) {
    var matiz by rememberSaveable { mutableStateOf(matizInicial) }
    val anchoEtiqueta = 36.dp

    Dialog(onDismissRequest = onCerrar) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Tabla Munsell", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColoresMunsell.matices.forEach { opcion ->
                        FilterChip(selected = matiz == opcion, onClick = { matiz = opcion }, label = { Text(opcion) })
                    }
                }
                Spacer(Modifier.padding(top = 8.dp))

                // Filas: luminosidad (8 = más claro arriba)
                ColoresMunsell.tabla(matiz).forEach { fila ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Text(
                            "${fila.luminosidad}/",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(anchoEtiqueta)
                        )
                        fila.chips.forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .aspectRatio(0.8f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (chip != null) {
                                    val elegido = chip.notacion == seleccionado?.notacion
                                    val color = colorDe(chip)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(color)
                                            .then(
                                                if (elegido) Modifier.border(
                                                    3.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp)
                                                ) else Modifier
                                            )
                                            .clickable { onElegir(chip) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (elegido) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Elegido",
                                                tint = if (color.luminance() > 0.4f) Color.Black else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Columnas: croma
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Spacer(Modifier.width(anchoEtiqueta))
                    ColoresMunsell.cromas.forEach { croma ->
                        Text(
                            "/$croma",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    "↕ Luminosidad (value)   ↔ Croma (chroma)",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
                Text(
                    "Los colores en pantalla son aproximados: compará siempre con la tabla física.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                TextButton(onClick = onCerrar, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
            }
        }
    }
}
