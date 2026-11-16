package com.example.appgeologia.data.recorrido

/**
 * Tabla de colores Munsell (Soil-Color Charts) para los matices más comunes en los suelos
 * de Comodoro Rivadavia: 10YR y 7.5YR. Copia la distribución de las páginas del libro:
 * filas de luminosidad (value) y columnas de croma (chroma), con huecos donde el libro
 * no tiene color.
 *
 * Notación: "MATIZ LUMINOSIDAD/CROMA", por ejemplo "10YR 5/3".
 *
 * Los colores son aproximados (sRGB) y sirven solo para ver el color en pantalla: la
 * pantalla del celular y la luz cambian cómo se ven, así que la referencia sigue siendo
 * la tabla física. En el PDF se imprime solo la notación en texto.
 *
 * Origen de los colores: tabla pública de conversión Munsell → RGB
 * (munsell-to-rgb.blogspot.com). Los chips de croma /1 y /3 que no figuraban ahí se
 * calcularon a partir de sus vecinos de la misma fila (/3 = promedio de /2 y /4;
 * /1 = 1,5·/2 − 0,5·/4), ya que el color varía casi linealmente con el croma.
 */
object ColoresMunsell {

    /** Un color de la tabla. [rgb] es 0xRRGGBB. */
    data class Chip(val matiz: String, val luminosidad: String, val croma: Int, val rgb: Int) {
        val notacion: String get() = "$matiz $luminosidad/$croma"
    }

    /** Una fila de la tabla: misma luminosidad, distintos cromas (null = hueco en el libro). */
    data class Fila(val luminosidad: String, val chips: List<Chip?>)

    /** Columnas de croma, en el orden del libro. */
    val cromas = listOf(1, 2, 3, 4, 6, 8)

    val matices = listOf("10YR", "7.5YR")

    private val colores: Map<String, Int> = mapOf(
        // ----- 10YR -----
        "10YR 8/1" to 0xD0C7BA, "10YR 8/2" to 0xD6C6AF, "10YR 8/3" to 0xDCC5A4,
        "10YR 8/4" to 0xE2C498, "10YR 8/6" to 0xECC281, "10YR 8/8" to 0xF5C068,
        "10YR 7/1" to 0xB6ACA2, "10YR 7/2" to 0xBBAB96, "10YR 7/3" to 0xC0AA8A,
        "10YR 7/4" to 0xC6A97F, "10YR 7/6" to 0xD0A768, "10YR 7/8" to 0xD8A551,
        "10YR 6/1" to 0x9C9288, "10YR 6/2" to 0xA1917D, "10YR 6/3" to 0xA69072,
        "10YR 6/4" to 0xAB8F68, "10YR 6/6" to 0xB58D51, "10YR 6/8" to 0xBC8B39,
        "10YR 5/1" to 0x81786F, "10YR 5/2" to 0x867765, "10YR 5/3" to 0x8C765A,
        "10YR 5/4" to 0x917550, "10YR 5/6" to 0x9A7339, "10YR 5/8" to 0xA07120,
        "10YR 4/1" to 0x686057, "10YR 4/2" to 0x6E5E4C, "10YR 4/3" to 0x735D43,
        "10YR 4/4" to 0x775C39, "10YR 4/6" to 0x7E5A25,
        "10YR 3/1" to 0x504841, "10YR 3/2" to 0x554738, "10YR 3/3" to 0x594630,
        "10YR 3/4" to 0x5D4527, "10YR 3/6" to 0x634310,
        "10YR 2/1" to 0x3A332D, "10YR 2/2" to 0x3E3226,
        // ----- 7.5YR -----
        "7.5YR 8/1" to 0xD1C6BE, "7.5YR 8/2" to 0xD8C5B3, "7.5YR 8/3" to 0xDFC4A8,
        "7.5YR 8/4" to 0xE6C29D, "7.5YR 8/6" to 0xF3BF88,
        "7.5YR 7/1" to 0xB6ACA3, "7.5YR 7/2" to 0xBDAA99, "7.5YR 7/3" to 0xC4A88F,
        "7.5YR 7/4" to 0xCBA785, "7.5YR 7/6" to 0xD6A470, "7.5YR 7/8" to 0xE0A15B,
        "7.5YR 6/1" to 0x9D928A, "7.5YR 6/2" to 0xA39080, "7.5YR 6/3" to 0xA98E76,
        "7.5YR 6/4" to 0xAF8D6D, "7.5YR 6/6" to 0xBA8A58, "7.5YR 6/8" to 0xC38743,
        "7.5YR 5/1" to 0x827872, "7.5YR 5/2" to 0x887668, "7.5YR 5/3" to 0x8E745E,
        "7.5YR 5/4" to 0x957355, "7.5YR 5/6" to 0x9F7041, "7.5YR 5/8" to 0xA76D2B,
        "7.5YR 4/1" to 0x695F58, "7.5YR 4/2" to 0x705D4F, "7.5YR 4/3" to 0x765C46,
        "7.5YR 4/4" to 0x7B5A3D, "7.5YR 4/6" to 0x83582C,
        "7.5YR 3/1" to 0x514842, "7.5YR 3/2" to 0x57463A, "7.5YR 3/3" to 0x5C4532,
        "7.5YR 3/4" to 0x60432A,
        "7.5YR 2.5/1" to 0x463D37, "7.5YR 2.5/2" to 0x4B3B30, "7.5YR 2.5/3" to 0x503A29
    )

    /** Qué cromas hay en cada fila de cada página del libro (copiado de las láminas). */
    private val distribucion: Map<String, List<Pair<String, List<Int>>>> = mapOf(
        "10YR" to listOf(
            "8" to listOf(1, 2, 3, 4, 6, 8),
            "7" to listOf(1, 2, 3, 4, 6, 8),
            "6" to listOf(1, 2, 3, 4, 6, 8),
            "5" to listOf(1, 2, 3, 4, 6, 8),
            "4" to listOf(1, 2, 3, 4, 6),
            "3" to listOf(1, 2, 3, 4, 6),
            "2" to listOf(1, 2)
        ),
        "7.5YR" to listOf(
            "8" to listOf(1, 2, 3, 4, 6),
            "7" to listOf(1, 2, 3, 4, 6, 8),
            "6" to listOf(1, 2, 3, 4, 6, 8),
            "5" to listOf(1, 2, 3, 4, 6, 8),
            "4" to listOf(1, 2, 3, 4, 6),
            "3" to listOf(1, 2, 3, 4),
            "2.5" to listOf(1, 2, 3)
        )
    )

    /** Las filas de la página de un matiz, de la más clara (arriba) a la más oscura. */
    fun tabla(matiz: String): List<Fila> =
        distribucion[matiz].orEmpty().map { (luminosidad, disponibles) ->
            Fila(
                luminosidad,
                cromas.map { croma ->
                    if (croma !in disponibles) null
                    else colores["$matiz $luminosidad/$croma"]?.let { Chip(matiz, luminosidad, croma, it) }
                }
            )
        }

    private val patron = Regex("""^\s*(10|7[.,]5)\s*YR\s*(\d+(?:[.,]5)?)\s*/\s*(\d+)\s*$""", RegexOption.IGNORE_CASE)

    /**
     * Interpreta un texto escrito a mano ("10YR 5/3", "10yr5/3", "7,5YR 4/4"...) y devuelve
     * el chip de la tabla, o null si no es una notación de la tabla.
     */
    fun buscar(texto: String): Chip? {
        val m = patron.find(texto) ?: return null
        val matiz = m.groupValues[1].replace(',', '.') + "YR"
        val luminosidad = m.groupValues[2].replace(',', '.')
        val croma = m.groupValues[3].toIntOrNull() ?: return null
        return tabla(matiz).firstOrNull { it.luminosidad == luminosidad }
            ?.chips?.firstOrNull { it?.croma == croma }
    }
}
