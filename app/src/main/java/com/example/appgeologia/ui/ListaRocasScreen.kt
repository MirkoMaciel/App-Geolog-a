package com.example.appgeologia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.appgeologia.data.CatalogoRocas
import com.example.appgeologia.data.Roca
import com.example.appgeologia.data.TipoRoca
import com.example.appgeologia.ui.theme.AppGeologiaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaRocasScreen(onRocaClick: (Roca) -> Unit, barraInferior: @Composable () -> Unit = {}) {
    // null = "Todas". rememberSaveable conserva el filtro al girar la pantalla.
    var tipoSeleccionado by rememberSaveable { mutableStateOf<TipoRoca?>(null) }
    val rocas = CatalogoRocas.filtrar(tipoSeleccionado)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Rocas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = barraInferior
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            FiltrosTipo(
                seleccionado = tipoSeleccionado,
                onSeleccionar = { tipoSeleccionado = it }
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rocas, key = { it.id }) { roca ->
                    TarjetaRoca(roca = roca, onClick = { onRocaClick(roca) })
                }
            }
        }
    }
}

@Composable
private fun FiltrosTipo(seleccionado: TipoRoca?, onSeleccionar: (TipoRoca?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = seleccionado == null,
            onClick = { onSeleccionar(null) },
            label = { Text("Todas") }
        )
        TipoRoca.entries.forEach { tipo ->
            FilterChip(
                selected = seleccionado == tipo,
                onClick = { onSeleccionar(tipo) },
                label = { Text(tipo.nombre) }
            )
        }
    }
}

@Composable
private fun TarjetaRoca(roca: Roca, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InicialRoca(roca = roca)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = roca.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = roca.tipo.nombre,
                    style = MaterialTheme.typography.labelMedium,
                    color = roca.tipo.color
                )
                Text(
                    text = roca.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Círculo con la inicial de la roca, pintado con el color de su tipo. */
@Composable
fun InicialRoca(roca: Roca, tamano: Int = 48) {
    Box(
        modifier = Modifier
            .size(tamano.dp)
            .background(roca.tipo.color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = roca.nombre.first().toString(),
            color = Color.White,
            style = if (tamano > 48) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListaRocasPreview() {
    AppGeologiaTheme {
        ListaRocasScreen(onRocaClick = {})
    }
}
