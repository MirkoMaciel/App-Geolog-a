package com.example.appgeologia.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Tarjeta con un encabezado que se toca para mostrar u ocultar su contenido.
 * El estado (abierta/cerrada) lo maneja quien la usa, para poder recordarlo al girar la pantalla.
 */
@Composable
fun SeccionDesplegable(
    titulo: String,
    abierta: Boolean,
    onCambiar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    detalle: String? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCambiar(!abierta) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(
                if (abierta) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (abierta) "Ocultar" else "Mostrar"
            )
        }
        AnimatedVisibility(visible = abierta) {
            Column {
                HorizontalDivider()
                contenido()
            }
        }
    }
}
