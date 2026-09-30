package com.example.appgeologia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appgeologia.data.CatalogoRocas
import com.example.appgeologia.data.Roca
import com.example.appgeologia.ui.theme.AppGeologiaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleRocaScreen(roca: Roca, onVolver: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roca.nombre) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("←", fontSize = 24.sp)
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InicialRoca(roca = roca, tamano = 96)
            Text(
                text = "Roca ${roca.tipo.nombre.lowercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = roca.tipo.color,
                fontWeight = FontWeight.Bold
            )

            SeccionInfo(titulo = "Descripción", contenido = roca.descripcion)
            SeccionInfo(titulo = "¿Cómo se forma?", contenido = roca.tipo.origen)
            SeccionInfo(
                titulo = "Minerales principales",
                contenido = roca.mineralesPrincipales.joinToString("\n") { "• $it" }
            )
            SeccionInfo(titulo = "Dureza", contenido = roca.dureza)
            SeccionInfo(titulo = "Usos", contenido = roca.usos)
        }
    }
}

@Composable
private fun SeccionInfo(titulo: String, contenido: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = contenido, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalleRocaPreview() {
    AppGeologiaTheme {
        DetalleRocaScreen(roca = CatalogoRocas.rocas.first(), onVolver = {})
    }
}
