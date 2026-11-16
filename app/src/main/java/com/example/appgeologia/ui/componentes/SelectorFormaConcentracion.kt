package com.example.appgeologia.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.appgeologia.data.recorrido.OpcionesSuelo
import kotlin.math.cos
import kotlin.math.sin

/**
 * Forma de las concentraciones. En la app y en el PDF se ve el nombre junto a su figura.
 * Se elige en una ventana con las 7 formas dibujadas.
 */
@Composable
fun SelectorFormaConcentracion(valor: String, onCambio: (String) -> Unit) {
    var elegir by rememberSaveable { mutableStateOf(false) }
    val forma = OpcionesSuelo.formasConcentracion.firstOrNull { it.codigo.equals(valor, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Forma", style = MaterialTheme.typography.labelLarge)
        when {
            forma != null -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                // La figura va junto al nombre, igual que en el PDF.
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FiguraForma(forma.codigo, Modifier.width(110.dp).height(66.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(forma.codigo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(forma.descripcion, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            valor.isNotBlank() -> Text(
                "\"$valor\" (forma escrita en una versión anterior)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(onClick = { elegir = true }) {
                Text(if (valor.isBlank()) "Elegir forma" else "Cambiar forma")
            }
            if (valor.isNotBlank()) {
                TextButton(onClick = { onCambio("") }) { Text("Quitar") }
            }
        }
    }

    if (elegir) {
        AlertDialog(
            onDismissRequest = { elegir = false },
            title = { Text("Forma de la concentración") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpcionesSuelo.formasConcentracion.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fila.forEach { opcion ->
                                val elegida = opcion.codigo == forma?.codigo
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (elegida) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (elegida) Modifier.border(
                                                2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp)
                                            ) else Modifier
                                        )
                                        .clickable {
                                            onCambio(opcion.codigo)
                                            elegir = false
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(opcion.codigo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        FiguraForma(opcion.codigo, Modifier.fillMaxWidth().height(52.dp).padding(vertical = 4.dp))
                                    }
                                }
                            }
                            // Si la última fila tiene una sola forma, se completa el espacio.
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { elegir = false }) { Text("Cerrar") } }
        )
    }
}

// ----------------------------------------------------------------------------- figuras

private val Relleno = Color(0xFF9C9C9C)
private val Punteado = Color(0xFF3C3C3C)
private val Contorno = Color(0xFF2E2E2E)

/** Dibujo de cada forma, inspirado en las figuras de la guía ("Ejemplos de moteados, concentraciones y formas"). */
@Composable
fun FiguraForma(nombre: String, modifier: Modifier) {
    Canvas(modifier = modifier) { dibujarForma(nombre) }
}

/**
 * Dibuja la misma figura de la app en un Canvas de Android (lo usa el PDF), dentro de
 * [rect] (en puntos de la página).
 */
fun dibujarFormaEnCanvas(canvas: android.graphics.Canvas, nombre: String, rect: android.graphics.RectF) {
    canvas.save()
    canvas.translate(rect.left, rect.top)
    CanvasDrawScope().draw(
        Density(1f),
        LayoutDirection.Ltr,
        androidx.compose.ui.graphics.Canvas(canvas),
        androidx.compose.ui.geometry.Size(rect.width(), rect.height())
    ) { dibujarForma(nombre) }
    canvas.restore()
}

private fun DrawScope.dibujarForma(nombre: String) {
    run {
        // Lienzo virtual de 100 × 60 centrado en el espacio disponible.
        val s = minOf(size.width / 100f, size.height / 60f)
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 60f * s) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)

        when (nombre) {
            "Cilíndrica" -> {
                listOf(30f to 0.9f, 62f to 1f).forEach { (cx, esc) ->
                    val ancho = 16f * esc
                    val alto = 50f * esc
                    val arriba = 30f - alto / 2f
                    val cuerpo = Path().apply {
                        addRoundRect(RoundRect(Rect(p(cx - ancho / 2, arriba), p(cx + ancho / 2, arriba + alto)), CornerRadius(ancho / 2 * s * 0.6f)))
                    }
                    rellenoPunteado(cuerpo, s)
                    drawOval(Contorno, topLeft = p(cx - ancho / 2, arriba), size = androidx.compose.ui.geometry.Size(ancho * s, 6f * s), style = Stroke(0.4f * s))
                }
            }
            "Dendrítica" -> {
                // Una "Y" ramificada y una rama en "T", como en la figura.
                fun rama(x1: Float, y1: Float, x2: Float, y2: Float, grosor: Float) {
                    val largo = kotlin.math.hypot(x2 - x1, y2 - y1)
                    val angulo = Math.toDegrees(kotlin.math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
                    rotate(angulo, pivot = p(x1, y1)) {
                        val camino = Path().apply {
                            addRoundRect(RoundRect(Rect(p(x1, y1 - grosor / 2), p(x1 + largo, y1 + grosor / 2)), CornerRadius(grosor / 2 * s)))
                        }
                        rellenoPunteado(camino, s)
                    }
                }
                rama(30f, 58f, 30f, 28f, 9f)
                rama(30f, 30f, 16f, 4f, 8f)
                rama(30f, 30f, 46f, 6f, 8f)
                rama(72f, 58f, 72f, 4f, 9f)
                rama(72f, 22f, 90f, 22f, 8f)
            }
            "Irregular" -> {
                listOf(
                    Triple(18f, 16f, 9f), Triple(46f, 12f, 8f), Triple(76f, 18f, 10f),
                    Triple(24f, 44f, 12f), Triple(54f, 40f, 7f), Triple(82f, 46f, 8f)
                ).forEachIndexed { i, (cx, cy, r) -> rellenoPunteado(mancha(::p, cx, cy, r, i), s) }
            }
            "Laminar" -> {
                listOf(
                    Triple(8f, 12f, 40f), Triple(54f, 8f, 38f), Triple(20f, 28f, 46f),
                    Triple(66f, 26f, 30f), Triple(4f, 44f, 34f), Triple(44f, 44f, 50f)
                ).forEach { (x, y, largo) ->
                    val lamina = Path().apply {
                        moveTo(p(x, y + 5f).x, p(x, y + 5f).y)
                        lineTo(p(x + largo, y).x, p(x + largo, y).y)
                        lineTo(p(x + largo + 3f, y + 3f).x, p(x + largo + 3f, y + 3f).y)
                        lineTo(p(x + 3f, y + 8f).x, p(x + 3f, y + 8f).y)
                        close()
                    }
                    rellenoPunteado(lamina, s)
                }
            }
            "Esférica" -> {
                listOf(
                    Triple(12f, 12f, 5f), Triple(34f, 10f, 6f), Triple(58f, 12f, 7f), Triple(84f, 10f, 5f),
                    Triple(20f, 32f, 7f), Triple(46f, 30f, 5f), Triple(70f, 32f, 8f), Triple(92f, 32f, 4f),
                    Triple(10f, 50f, 4f), Triple(34f, 50f, 6f), Triple(58f, 51f, 4f), Triple(80f, 50f, 6f)
                ).forEach { (cx, cy, r) ->
                    rellenoPunteado(Path().apply { addOval(Rect(p(cx - r, cy - r), p(cx + r, cy + r))) }, s)
                }
            }
            "Hilos" -> {
                // Filamentos finos, punteados y sin ramificar.
                val trazo = Stroke(width = 0.4f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * s, 2f * s)))
                listOf(14f, 28f, 42f, 56f, 70f, 84f).forEachIndexed { i, x ->
                    val hilo = Path().apply {
                        moveTo(p(x, 4f).x, p(x, 4f).y)
                        val dx = if (i % 2 == 0) 3f else -3f
                        cubicTo(p(x + dx, 20f).x, p(x + dx, 20f).y, p(x - dx, 40f).x, p(x - dx, 40f).y, p(x + dx / 2, 56f).x, p(x + dx / 2, 56f).y)
                    }
                    drawPath(hilo, Contorno, style = trazo)
                }
            }
            "Reticulada" -> {
                // Cuerpos entrelazados con espaciado similar (como ladrillos).
                for (fila in 0..2) {
                    val desplazamiento = if (fila % 2 == 0) 0f else 12f
                    for (col in 0..3) {
                        val x = 4f + desplazamiento + col * 24f
                        if (x + 20f > 98f) continue
                        val y = 4f + fila * 18f
                        val bloque = Path().apply {
                            addRoundRect(RoundRect(Rect(p(x, y), p(x + 20f, y + 14f)), CornerRadius(3f * s)))
                        }
                        rellenoPunteado(bloque, s)
                    }
                }
            }
        }
    }
}

/** Relleno gris con puntos oscuros y contorno, imitando el punteado de los dibujos. */
private fun DrawScope.rellenoPunteado(camino: Path, s: Float) {
    drawPath(camino, Relleno)
    clipPath(camino) {
        val b = camino.getBounds()
        val paso = 3.2f * s
        var fila = 0
        var y = b.top
        while (y < b.bottom) {
            var x = b.left + if (fila % 2 == 0) 0f else paso / 2f
            while (x < b.right) {
                drawCircle(Punteado, radius = 0.55f * s, center = Offset(x, y))
                x += paso
            }
            y += paso * 0.8f
            fila++
        }
    }
    drawPath(camino, Contorno, style = Stroke(width = 0.37f * s))
}

/** Mancha de contorno irregular (para la forma "Irregular"). */
private fun mancha(p: (Float, Float) -> Offset, cx: Float, cy: Float, r: Float, semilla: Int): Path {
    val radios = listOf(1.0f, 0.75f, 1.15f, 0.85f, 1.05f, 0.7f, 1.1f, 0.9f)
    val n = radios.size
    val puntos = (0 until n).map { i ->
        val a = 2 * Math.PI * i / n + semilla
        val rr = r * radios[(i + semilla) % n]
        p(cx + (rr * cos(a)).toFloat() * 1.3f, cy + (rr * sin(a)).toFloat())
    }
    return Path().apply {
        moveTo((puntos[0].x + puntos[1].x) / 2, (puntos[0].y + puntos[1].y) / 2)
        for (i in 1..n) {
            val actual = puntos[i % n]
            val siguiente = puntos[(i + 1) % n]
            quadraticTo(actual.x, actual.y, (actual.x + siguiente.x) / 2, (actual.y + siguiente.y) / 2)
        }
        close()
    }
}
