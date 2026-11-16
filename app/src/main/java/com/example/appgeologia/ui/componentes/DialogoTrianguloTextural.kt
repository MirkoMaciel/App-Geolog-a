package com.example.appgeologia.ui.componentes

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.appgeologia.data.recorrido.TrianguloTextural
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Colores tomados de la figura del libro: líneas negras, grilla punteada gris y trazas azules.
private val ColorLinea = Color(0xFF1A1A1A)
private val ColorGrilla = Color(0xFF9A9A9A)
private val ColorTraza = Color(0xFF4A86C8)
private val ColorPunto = Color(0xFF5B9BD5)
private val ColorBordePunto = Color(0xFF2E6DA4)

private val ALTURA = sqrt(3.0).toFloat() / 2f

/**
 * Ubicación del triángulo dentro del lienzo (deja márgenes para números y títulos).
 * Convierte porcentajes (arena, limo, arcilla) a puntos de pantalla y al revés.
 */
private class Geometria(ancho: Float, alto: Float, densidad: Density) {
    private val margenIzq = with(densidad) { 58.dp.toPx() }
    private val margenDer = with(densidad) { 58.dp.toPx() }
    private val margenSup = with(densidad) { 14.dp.toPx() }
    private val margenInf = with(densidad) { 64.dp.toPx() }

    val lado = minOf(ancho - margenIzq - margenDer, (alto - margenSup - margenInf) / ALTURA)
    val altura = lado * ALTURA
    val x0 = margenIzq + (ancho - margenIzq - margenDer - lado) / 2f
    val yBase = margenSup + altura

    /** Punto del triángulo para fracciones (0..1) de limo y arcilla. */
    fun punto(limo: Float, arcilla: Float) = Offset(x0 + lado * (limo + arcilla / 2f), yBase - altura * arcilla)

    fun puntoArenaArcilla(arenaPct: Double, arcillaPct: Double): Offset {
        val arcilla = (arcillaPct / 100).toFloat()
        val limo = 1f - (arenaPct / 100).toFloat() - arcilla
        return punto(limo, arcilla)
    }

    /** Porcentajes enteros (arena, limo, arcilla) del punto tocado; si está afuera, el más cercano del borde. */
    fun porcentajes(p: Offset): Triple<Int, Int, Int> {
        var arcilla = ((yBase - p.y) / altura).coerceIn(0f, 1f)
        var limo = (p.x - x0) / lado - arcilla / 2f
        var arena = 1f - limo - arcilla
        if (limo < 0f) { arena += limo; limo = 0f }
        if (arena < 0f) { limo += arena; arena = 0f }
        limo = limo.coerceIn(0f, 1f)
        arcilla = (1f - arena - limo).coerceIn(0f, 1f)
        val arcillaPct = (arcilla * 100).roundToInt()
        val limoPct = (limo * 100).roundToInt().coerceAtMost(100 - arcillaPct)
        return Triple(100 - arcillaPct - limoPct, limoPct, arcillaPct)
    }
}

/**
 * Vista del triángulo textural. Se puede tocar o arrastrar el punto dentro del
 * triángulo, o escribir los porcentajes; las trazas azules y la clase se actualizan solas.
 * Al aplicar, devuelve los tres valores y el nombre de la clase (que es lo que va al PDF).
 */
@Composable
fun DialogoTrianguloTextural(
    arenaInicial: Int?,
    limoInicial: Int?,
    arcillaInicial: Int?,
    onAplicar: (arena: Int, limo: Int, arcilla: Int, clase: String) -> Unit,
    onCerrar: () -> Unit
) {
    var arenaTxt by rememberSaveable { mutableStateOf(arenaInicial?.toString().orEmpty()) }
    var limoTxt by rememberSaveable { mutableStateOf(limoInicial?.toString().orEmpty()) }
    var arcillaTxt by rememberSaveable { mutableStateOf(arcillaInicial?.toString().orEmpty()) }

    val arena = arenaTxt.toIntOrNull()
    val limo = limoTxt.toIntOrNull()
    val arcilla = arcillaTxt.toIntOrNull()
    val suma = (arena ?: 0) + (limo ?: 0) + (arcilla ?: 0)
    val clase = if (arena != null && limo != null && arcilla != null) TrianguloTextural.clasificar(arena, limo, arcilla) else null

    fun desdeTriangulo(valores: Triple<Int, Int, Int>) {
        arenaTxt = valores.first.toString()
        limoTxt = valores.second.toString()
        arcillaTxt = valores.third.toString()
    }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Triángulo textural",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
                }
                Text(
                    "Tocá o arrastrá el punto dentro del triángulo, o escribí los porcentajes de la tierra fina (< 2 mm).",
                    style = MaterialTheme.typography.bodySmall
                )

                // ----- Triángulo -----
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    // Alto justo para el triángulo: lado * 0,866 + márgenes de números y títulos.
                    BoxWithConstraints {
                        val alto = (maxWidth - 116.dp) * ALTURA + 78.dp
                        LienzoTriangulo(
                            arena = arena, limo = limo, arcilla = arcilla, valido = clase != null,
                            onElegir = ::desdeTriangulo,
                            modifier = Modifier.fillMaxWidth().height(alto)
                        )
                    }
                }

                // ----- Valores -----
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoPorcentaje("Arena %", arenaTxt, { arenaTxt = it }, Modifier.weight(1f))
                    CampoPorcentaje("Limo %", limoTxt, { limoTxt = it }, Modifier.weight(1f))
                    CampoPorcentaje("Arcilla %", arcillaTxt, { arcillaTxt = it }, Modifier.weight(1f))
                }

                // ----- Resultado -----
                if (clase != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("Clase textural", style = MaterialTheme.typography.labelMedium)
                        Text(clase.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        when {
                            arena == null || limo == null || arcilla == null ->
                                "Completá los tres porcentajes o tocá el triángulo."
                            suma != 100 -> "La suma da $suma %: tiene que dar 100 % " +
                                (if (suma < 100) "(faltan ${100 - suma} %)." else "(sobran ${suma - 100} %).")
                            else -> "Los porcentajes no pueden ser negativos."
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    OutlinedButton(onClick = onCerrar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = { if (clase != null) onAplicar(arena!!, limo!!, arcilla!!, clase.nombre) },
                        enabled = clase != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("Usar esta clase") }
                }
            }
        }
    }
}

@Composable
private fun CampoPorcentaje(etiqueta: String, valor: String, onCambio: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> onCambio(nuevo.filter { it.isDigit() }.take(3)) },
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

/** El dibujo del triángulo, con el gesto para elegir el punto. */
@Composable
private fun LienzoTriangulo(
    arena: Int?,
    limo: Int?,
    arcilla: Int?,
    valido: Boolean,
    onElegir: (Triple<Int, Int, Int>) -> Unit,
    modifier: Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { p ->
                    onElegir(Geometria(size.width.toFloat(), size.height.toFloat(), this).porcentajes(p))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { cambio, _ ->
                    onElegir(Geometria(size.width.toFloat(), size.height.toFloat(), this).porcentajes(cambio.position))
                }
            }
    ) {
        val g = Geometria(size.width, size.height, this)
        dibujarGrilla(g)
        dibujarClases(g)
        dibujarBordeYEscalas(g)
        if (valido && arena != null && limo != null && arcilla != null) dibujarTrazas(g, arena, limo, arcilla)
    }
}

// ----------------------------------------------------------------------------- dibujo

/** Grilla punteada cada 10 %, paralela a los tres lados (como en la figura). */
private fun DrawScope.dibujarGrilla(g: Geometria) {
    val punteado = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    val grosor = 0.7.dp.toPx()
    for (k in 1..9) {
        val f = k / 10f
        // Arcilla constante: horizontales
        drawLine(ColorGrilla, g.punto(0f, f), g.punto(1f - f, f), grosor, pathEffect = punteado)
        // Limo constante: paralelas al lado izquierdo
        drawLine(ColorGrilla, g.punto(f, 0f), g.punto(f, 1f - f), grosor, pathEffect = punteado)
        // Arena constante: paralelas al lado derecho
        drawLine(ColorGrilla, g.punto(1f - f, 0f), g.punto(0f, 1f - f), grosor, pathEffect = punteado)
    }
}

/** Límites de las 12 clases (negro) y sus nombres. */
private fun DrawScope.dibujarClases(g: Geometria) {
    // Las regiones chicas llevan letra más chica para que el nombre no pise las líneas.
    val chicas = setOf(
        TrianguloTextural.Clase.ARENOSO, TrianguloTextural.Clase.ARENO_FRANCO,
        TrianguloTextural.Clase.ARCILLO_LIMOSO, TrianguloTextural.Clase.FRANCO_ARCILLO_LIMOSO
    )
    TrianguloTextural.Clase.entries.forEach { clase ->
        val pincel = pincelTexto(this, if (clase in chicas) 8f else 9.5f, ColorLinea)
        val camino = Path()
        clase.vertices.forEachIndexed { i, (arena, arcilla) ->
            val p = g.puntoArenaArcilla(arena, arcilla)
            if (i == 0) camino.moveTo(p.x, p.y) else camino.lineTo(p.x, p.y)
        }
        camino.close()
        drawPath(camino, ColorLinea, style = Stroke(width = 1.3.dp.toPx()))

        val centro = g.puntoArenaArcilla(clase.posEtiqueta.first, clase.posEtiqueta.second)
        val altoLinea = pincel.textSize * 1.1f
        var y = centro.y - altoLinea * (clase.etiqueta.size - 1) / 2f + pincel.textSize / 3f
        clase.etiqueta.forEach { linea ->
            drawIntoCanvas { it.nativeCanvas.drawText(linea, centro.x, y, pincel) }
            y += altoLinea
        }
    }
}

/** Contorno del triángulo, marcas cada 10 % y títulos de los ejes. */
private fun DrawScope.dibujarBordeYEscalas(g: Geometria) {
    val izq = g.punto(0f, 0f)
    val der = g.punto(1f, 0f)
    val arriba = g.punto(0f, 1f)
    val contorno = Path().apply {
        moveTo(izq.x, izq.y); lineTo(der.x, der.y); lineTo(arriba.x, arriba.y); close()
    }
    drawPath(contorno, ColorLinea, style = Stroke(width = 1.8.dp.toPx()))

    val marca = 6.dp.toPx()
    val numeros = pincelTexto(this, 10f, ColorLinea, negrita = true)
    val c60 = 0.5f
    val s60 = ALTURA
    for (k in 1..10) {
        val f = k / 10f
        val texto = (k * 10).toString()
        drawIntoCanvas { lienzo ->
            val nc = lienzo.nativeCanvas
            // Arcilla (lado izquierdo): marca horizontal hacia afuera
            val pa = g.punto(0f, f)
            drawLine(ColorLinea, pa, pa + Offset(-marca, 0f), 1.dp.toPx())
            nc.drawText(texto, pa.x - marca - numeros.measureText(texto) / 2f - 3.dp.toPx(), pa.y + numeros.textSize / 3f, numeros)
            // Limo (lado derecho): marca hacia arriba a la derecha
            val pl = g.punto(f, 1f - f)
            val dl = Offset(marca * c60, -marca * s60)
            drawLine(ColorLinea, pl, pl + dl, 1.dp.toPx())
            nc.drawText(texto, pl.x + dl.x + numeros.measureText(texto) / 2f + 3.dp.toPx(), pl.y + dl.y + numeros.textSize / 3f, numeros)
            // Arena (base): marca hacia abajo a la derecha; 100 en el vértice izquierdo
            val pb = g.punto(1f - f, 0f)
            val db = Offset(marca * c60, marca * s60)
            drawLine(ColorLinea, pb, pb + db, 1.dp.toPx())
            nc.drawText(texto, pb.x + db.x, pb.y + db.y + numeros.textSize + 2.dp.toPx(), numeros)
        }
    }

    // Títulos de los ejes
    val titulos = pincelTexto(this, 11f, ColorLinea, negrita = true)
    drawIntoCanvas { lienzo ->
        val nc = lienzo.nativeCanvas
        val medioIzq = g.punto(0f, 0.5f)
        nc.save()
        nc.rotate(-60f, medioIzq.x - 36.dp.toPx(), medioIzq.y)
        nc.drawText("Arcilla por separado (%)", medioIzq.x - 36.dp.toPx(), medioIzq.y, titulos)
        nc.restore()

        val medioDer = g.punto(0.5f, 0.5f)
        nc.save()
        nc.rotate(60f, medioDer.x + 36.dp.toPx(), medioDer.y)
        nc.drawText("Limo por separado (%)", medioDer.x + 36.dp.toPx(), medioDer.y, titulos)
        nc.restore()

        val medioBase = g.punto(0.5f, 0f)
        val yTitulo = medioBase.y + 44.dp.toPx()
        nc.drawText("Arena por separado (%)", medioBase.x, yTitulo, titulos)
        // Flecha hacia la izquierda debajo del título (la arena crece hacia la izquierda)
        val ancho = titulos.measureText("Arena por separado (%)")
        val yFlecha = yTitulo + 8.dp.toPx()
        val inicio = Offset(medioBase.x + ancho / 2f, yFlecha)
        val fin = Offset(medioBase.x - ancho / 2f, yFlecha)
        drawLine(ColorLinea, inicio, fin, 1.2.dp.toPx())
        drawLine(ColorLinea, fin, fin + Offset(6.dp.toPx(), -3.dp.toPx()), 1.2.dp.toPx())
        drawLine(ColorLinea, fin, fin + Offset(6.dp.toPx(), 3.dp.toPx()), 1.2.dp.toPx())
    }
}

/**
 * Las tres trazas azules desde el punto hasta cada eje, como en la figura:
 * arcilla → horizontal al lado izquierdo; limo → paralela al lado izquierdo hasta el
 * lado derecho; arena → paralela al lado derecho hasta la base.
 */
private fun DrawScope.dibujarTrazas(g: Geometria, arena: Int, limo: Int, arcilla: Int) {
    val a = arena / 100f
    val l = limo / 100f
    val c = arcilla / 100f
    val p = g.punto(l, c)
    val grosor = 2.2.dp.toPx()
    drawLine(ColorTraza, p, g.punto(0f, c), grosor)          // a la escala de arcilla
    drawLine(ColorTraza, p, g.punto(l, 1f - l), grosor)      // a la escala de limo
    drawLine(ColorTraza, p, g.punto(1f - a, 0f), grosor)     // a la escala de arena
    drawCircle(ColorPunto, radius = 8.dp.toPx(), center = p)
    drawCircle(ColorBordePunto, radius = 8.dp.toPx(), center = p, style = Stroke(width = 1.5.dp.toPx()))
}

private fun pincelTexto(densidad: Density, tamSp: Float, color: Color, negrita: Boolean = false) = Paint().apply {
    isAntiAlias = true
    this.color = color.toArgb()
    textAlign = Paint.Align.CENTER
    textSize = with(densidad) { tamSp.dp.toPx() }
    typeface = Typeface.create(Typeface.SANS_SERIF, if (negrita) Typeface.BOLD else Typeface.NORMAL)
}
