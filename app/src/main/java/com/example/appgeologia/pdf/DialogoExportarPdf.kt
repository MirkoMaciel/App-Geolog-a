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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appgeologia.archivos.AlmacenExportaciones
import com.example.appgeologia.archivos.DatosExportacion
import com.example.appgeologia.archivos.Exportacion
import com.example.appgeologia.archivos.compartirPdf
import com.example.appgeologia.archivos.verPdf
import com.example.appgeologia.data.recorrido.Parada
import com.example.appgeologia.data.recorrido.Recorrido
import com.example.appgeologia.ui.componentes.formatearFechaHora
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Exporta el PDF de una parada. [obtener] devuelve los datos actuales (ya guardados). */
@Composable
fun DialogoExportarParada(obtener: () -> Pair<Recorrido, Parada>?, onCerrar: () -> Unit) {
    val actual = obtener() ?: return CerrarAlInstante(onCerrar)
    val (datos, nombreBase) = AlmacenExportaciones.datosParada(actual.first, actual.second)
    DialogoExportarPdf(
        titulo = "Exportar parada ${actual.second.numero}",
        datos = datos,
        nombreBase = nombreBase,
        modificado = { obtener()?.second?.modificado ?: 0L },
        escribir = { context, archivo ->
            val (recorrido, parada) = obtener() ?: actual
            PlanillaPdf(context).generarParada(recorrido, parada, archivo)
        },
        onCerrar = onCerrar
    )
}

/** Exporta el PDF de la salida completa. [obtener] devuelve los datos actuales. */
@Composable
fun DialogoExportarSalida(obtener: () -> Recorrido?, onCerrar: () -> Unit) {
    val actual = obtener() ?: return CerrarAlInstante(onCerrar)
    val (datos, nombreBase) = AlmacenExportaciones.datosSalida(actual)
    DialogoExportarPdf(
        titulo = "Exportar salida",
        datos = datos,
        nombreBase = nombreBase,
        modificado = { obtener()?.modificado ?: 0L },
        escribir = { context, archivo -> PlanillaPdf(context).generarRecorrido(obtener() ?: actual, archivo) },
        onCerrar = onCerrar
    )
}

/** Si lo que se quería exportar ya no existe (por ejemplo, se borró), se cierra el diálogo. */
@Composable
private fun CerrarAlInstante(onCerrar: () -> Unit) {
    LaunchedEffect(Unit) { onCerrar() }
}

/** Vuelve a generar el PDF vigente de una parada (o de la salida si [parada] es null). */
fun regenerarPdf(context: Context, almacen: AlmacenExportaciones, recorrido: Recorrido, parada: Parada?): File =
    if (parada != null) {
        val (datos, nombre) = AlmacenExportaciones.datosParada(recorrido, parada)
        almacen.exportar(datos, nombre, copia = false) { PlanillaPdf(context).generarParada(recorrido, parada, it) }
    } else {
        val (datos, nombre) = AlmacenExportaciones.datosSalida(recorrido)
        almacen.exportar(datos, nombre, copia = false) { PlanillaPdf(context).generarRecorrido(recorrido, it) }
    }

private sealed interface Estado {
    data object Revisando : Estado
    data class Conflicto(val anterior: Exportacion) : Estado
    data object Generando : Estado
    data class Listo(val archivo: File, val mensaje: String, val aviso: Boolean = false) : Estado
    data class Fallo(val mensaje: String) : Estado
}

/**
 * Exporta un PDF sin generar archivos de más:
 *  - si no hay un PDF de esa parada/salida, lo genera;
 *  - si ya hay uno y los datos no cambiaron, usa ese (no genera nada);
 *  - si ya hay uno pero los datos cambiaron, pregunta: reemplazarlo, usar el anterior o
 *    guardar una copia nueva aparte.
 * Después ofrece verlo, compartirlo o guardar una copia en el teléfono.
 */
@Composable
private fun DialogoExportarPdf(
    titulo: String,
    datos: DatosExportacion,
    nombreBase: String,
    modificado: () -> Long,
    escribir: (Context, File) -> Unit,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val almacen = remember { AlmacenExportaciones(context) }
    var estado by remember { mutableStateOf<Estado>(Estado.Revisando) }
    val queCambio = if (datos.paradaId != null) "la parada" else "la salida"

    fun generar(copia: Boolean, mensaje: String) {
        estado = Estado.Generando
        scope.launch {
            estado = try {
                val archivo = withContext(Dispatchers.Default) {
                    almacen.exportar(datos, nombreBase, copia) { escribir(context, it) }
                }
                Estado.Listo(archivo, mensaje)
            } catch (e: Exception) {
                Estado.Fallo(e.message ?: "Error desconocido")
            }
        }
    }

    LaunchedEffect(Unit) {
        val anterior = withContext(Dispatchers.IO) { almacen.vigente(datos.recorridoId, datos.paradaId) }
        when {
            anterior == null -> generar(copia = false, mensaje = "PDF generado.")
            almacen.estaAlDia(anterior, modificado()) -> estado = Estado.Listo(
                anterior.archivo,
                "Este PDF ya está al día (exportado el ${formatearFechaHora(anterior.archivo.lastModified())}): " +
                    "no hizo falta generarlo de nuevo."
            )
            else -> estado = Estado.Conflicto(anterior)
        }
    }

    val lanzadorGuardar = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { destino: Uri? ->
        val origen = (estado as? Estado.Listo)?.archivo
        if (destino != null && origen != null) {
            context.contentResolver.openOutputStream(destino)?.use { salida ->
                origen.inputStream().use { it.copyTo(salida) }
            }
            Toast.makeText(context, "PDF guardado", Toast.LENGTH_SHORT).show()
            onCerrar()
        }
    }

    val actual = estado
    if (actual is Estado.Conflicto) {
        AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("Ya existe un PDF de ${datos.contenido.replaceFirstChar { it.lowercase() }}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se exportó el ${formatearFechaHora(actual.anterior.archivo.lastModified())}, " +
                            "pero $queCambio cambió desde entonces."
                    )
                    Text(
                        "Si elegís Reemplazar, el PDF anterior se borra y se genera uno nuevo con los datos actuales.",
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { generar(copia = false, mensaje = "Se reemplazó el PDF anterior por uno con los datos actuales.") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Reemplazar") }
                    OutlinedButton(
                        onClick = {
                            estado = Estado.Listo(
                                actual.anterior.archivo,
                                "Es el PDF anterior: no incluye los últimos cambios.",
                                aviso = true
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Usar el anterior") }
                    OutlinedButton(
                        onClick = { generar(copia = true, mensaje = "Se guardó como copia aparte; el PDF anterior se mantiene.") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Guardar como copia nueva") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = {
            when (actual) {
                Estado.Revisando, Estado.Generando, is Estado.Conflicto -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(if (actual == Estado.Revisando) "Revisando…" else "Generando PDF…")
                }
                is Estado.Fallo -> Text("No se pudo generar el PDF: ${actual.mensaje}")
                is Estado.Listo -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(actual.archivo.name, fontWeight = FontWeight.Bold)
                    Text(
                        actual.mensaje,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (actual.aviso) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text("Está en \"Mis exportaciones\".", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { verPdf(context, actual.archivo) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver")
                    }
                    Button(onClick = { compartirPdf(context, actual.archivo) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Compartir")
                    }
                    OutlinedButton(
                        onClick = { lanzadorGuardar.launch(actual.archivo.name) },
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
