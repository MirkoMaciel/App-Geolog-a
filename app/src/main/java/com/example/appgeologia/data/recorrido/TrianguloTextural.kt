package com.example.appgeologia.data.recorrido

/**
 * Triángulo textural (USDA, el mismo de la Fig. 22 del libro de Geología, Tomo IV).
 *
 * Clasifica la textura de la tierra fina (< 2 mm) a partir de los porcentajes de
 * arena, limo y arcilla, que siempre suman 100 %.
 *
 * Geometría del dibujo:
 *  - vértice inferior izquierdo = 100 % arena
 *  - vértice inferior derecho  = 100 % limo
 *  - vértice superior          = 100 % arcilla
 */
object TrianguloTextural {

    /**
     * Las 12 clases texturales.
     * [nombre]: lo que se escribe en el campo Textura y sale en el PDF.
     * [etiqueta]: el texto que se dibuja dentro del triángulo (como en el libro).
     * [vertices]: el polígono de la clase, como pares (arena %, arcilla %).
     * [posEtiqueta]: dónde se escribe la etiqueta, como (arena %, arcilla %).
     */
    enum class Clase(
        val nombre: String,
        val etiqueta: List<String>,
        val vertices: List<Pair<Double, Double>>,
        val posEtiqueta: Pair<Double, Double>
    ) {
        ARCILLOSO(
            "Arcilloso", listOf("arcillosa"),
            listOf(45.0 to 40.0, 45.0 to 55.0, 0.0 to 100.0, 0.0 to 60.0, 20.0 to 40.0),
            27.0 to 58.0
        ),
        ARCILLO_LIMOSO(
            "Arcillo limoso", listOf("arcillo", "limosa"),
            listOf(20.0 to 40.0, 0.0 to 60.0, 0.0 to 40.0),
            7.0 to 45.0
        ),
        ARCILLO_ARENOSO(
            "Arcillo arenoso", listOf("arcillo", "arenosa"),
            listOf(65.0 to 35.0, 45.0 to 55.0, 45.0 to 35.0),
            53.0 to 41.0
        ),
        FRANCO_ARCILLO_LIMOSO(
            "Franco arcillo limoso", listOf("franco arcillo-", "limosa"),
            listOf(20.0 to 27.0, 20.0 to 40.0, 0.0 to 40.0, 0.0 to 27.0),
            11.0 to 33.0
        ),
        FRANCO_ARCILLOSO(
            "Franco arcilloso", listOf("franco arcillosa"),
            listOf(45.0 to 27.0, 45.0 to 40.0, 20.0 to 40.0, 20.0 to 27.0),
            32.0 to 33.0
        ),
        FRANCO_ARCILLO_ARENOSO(
            "Franco arcillo arenoso", listOf("franco", "arcillo", "arenosa"),
            listOf(80.0 to 20.0, 65.0 to 35.0, 45.0 to 35.0, 45.0 to 27.0, 52.0 to 20.0),
            61.0 to 27.0
        ),
        FRANCO(
            "Franco", listOf("franca"),
            listOf(43.0 to 7.0, 52.0 to 7.0, 52.0 to 20.0, 45.0 to 27.0, 23.0 to 27.0),
            40.0 to 17.0
        ),
        FRANCO_LIMOSO(
            "Franco limoso", listOf("franco", "limosa"),
            listOf(50.0 to 0.0, 20.0 to 0.0, 8.0 to 12.0, 0.0 to 12.0, 0.0 to 27.0, 23.0 to 27.0, 43.0 to 7.0),
            18.0 to 14.0
        ),
        LIMOSO(
            "Limoso", listOf("limosa"),
            listOf(20.0 to 0.0, 0.0 to 0.0, 0.0 to 12.0, 8.0 to 12.0),
            7.0 to 4.0
        ),
        FRANCO_ARENOSO(
            "Franco arenoso", listOf("franco arenosa"),
            listOf(70.0 to 0.0, 50.0 to 0.0, 43.0 to 7.0, 52.0 to 7.0, 52.0 to 20.0, 80.0 to 20.0, 85.0 to 15.0),
            66.0 to 11.0
        ),
        ARENO_FRANCO(
            "Areno franco", listOf("areno", "franca"),
            listOf(85.0 to 0.0, 70.0 to 0.0, 85.0 to 15.0, 90.0 to 10.0),
            82.0 to 5.0
        ),
        ARENOSO(
            "Arenoso", listOf("arenosa"),
            listOf(100.0 to 0.0, 85.0 to 0.0, 90.0 to 10.0),
            94.0 to 2.0
        )
    }

    /**
     * Clase textural de una muestra (porcentajes de 0 a 100 que suman 100).
     * Usa los límites estándar USDA; en un límite exacto gana la clase que se
     * evalúa primero (más arcillosa).
     */
    fun clasificar(arena: Double, limo: Double, arcilla: Double): Clase = when {
        arcilla >= 40 && limo >= 40 -> Clase.ARCILLO_LIMOSO
        arcilla >= 35 && arena > 45 -> Clase.ARCILLO_ARENOSO
        arcilla >= 40 -> Clase.ARCILLOSO
        arcilla >= 27 && arena <= 20 -> Clase.FRANCO_ARCILLO_LIMOSO
        arcilla >= 27 && arena <= 45 -> Clase.FRANCO_ARCILLOSO
        arcilla >= 20 && limo < 28 && arena > 45 -> Clase.FRANCO_ARCILLO_ARENOSO
        limo >= 80 && arcilla < 12 -> Clase.LIMOSO
        limo >= 50 -> Clase.FRANCO_LIMOSO
        arcilla >= 7 && limo >= 28 && arena <= 52 -> Clase.FRANCO
        limo + 1.5 * arcilla < 15 -> Clase.ARENOSO
        limo + 2 * arcilla < 30 -> Clase.ARENO_FRANCO
        else -> Clase.FRANCO_ARENOSO
    }

    /** Versión con enteros (como se cargan en la app). Null si no suman 100. */
    fun clasificar(arena: Int, limo: Int, arcilla: Int): Clase? =
        if (arena < 0 || limo < 0 || arcilla < 0 || arena + limo + arcilla != 100) null
        else clasificar(arena.toDouble(), limo.toDouble(), arcilla.toDouble())
}
