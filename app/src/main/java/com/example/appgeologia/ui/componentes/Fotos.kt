package com.example.appgeologia.ui.componentes

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.appgeologia.archivos.AlmacenFotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carga la foto achicada en segundo plano (null mientras carga o si no existe). */
@Composable
private fun rememberFoto(almacen: AlmacenFotos, nombre: String, ladoMaximo: Int): Bitmap? {
    val bitmap by produceState<Bitmap?>(initialValue = null, nombre, ladoMaximo) {
        value = withContext(Dispatchers.IO) { almacen.cargarReducida(nombre, ladoMaximo) }
    }
    return bitmap
}

/** Miniatura cuadrada de una foto. */
@Composable
fun MiniaturaFoto(
    almacen: AlmacenFotos,
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Int = 96,
    onClick: (() -> Unit)? = null
) {
    val bitmap = rememberFoto(almacen, nombre, ladoMaximo = 320)
    Box(
        modifier = modifier
            .size(tamano.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Foto a pantalla completa; se cierra tocando la X o el botón atrás. */
@Composable
fun VisorFoto(almacen: AlmacenFotos, nombre: String, onCerrar: () -> Unit) {
    val bitmap = rememberFoto(almacen, nombre, ladoMaximo = 2048)
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Foto",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                )
            }
            IconButton(
                onClick = onCerrar,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
