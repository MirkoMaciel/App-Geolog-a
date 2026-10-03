package com.example.appgeologia.pdf

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.appgeologia.archivos.compartirPdf
import com.example.appgeologia.archivos.verPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Genera el PDF con [generar] (queda guardado en "Mis exportaciones") y ofrece verlo,
 * compartirlo (mail, WhatsApp, Drive...) o guardar una copia en una carpeta del teléfono.
 */
@Composable
fun DialogoExportarPdf(
    titulo: String,
    generar: (Context) -> File,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    var archivo by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            archivo = withContext(Dispatchers.Default) { generar(context) }
        } catch (e: Exception) {
            error = e.message ?: "Error desconocido"
        }
    }

    val lanzadorGuardar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destino: Uri? ->
        val origen = archivo
        if (destino != null && origen != null) {
            context.contentResolver.openOutputStream(destino)?.use { salida ->
                origen.inputStream().use { it.copyTo(salida) }
            }
            Toast.makeText(context, "PDF guardado", Toast.LENGTH_SHORT).show()
            onCerrar()
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = {
            val listo = archivo
            when {
                error != null -> Text("No se pudo generar el PDF: $error")
                listo == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Generando PDF…")
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(listo.name)
                    Text(
                        "También quedó guardado en \"Mis exportaciones\".",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = { verPdf(context, listo) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver")
                    }
                    Button(onClick = { compartirPdf(context, listo) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Compartir")
                    }
                    OutlinedButton(
                        onClick = { lanzadorGuardar.launch(listo.name) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar copia en el teléfono")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
