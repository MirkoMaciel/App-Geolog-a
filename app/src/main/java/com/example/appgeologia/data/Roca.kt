package com.example.appgeologia.data

import androidx.compose.ui.graphics.Color

/** Las tres grandes familias de rocas según su origen. */
enum class TipoRoca(val nombre: String, val origen: String, val color: Color) {
    IGNEA(
        nombre = "Ígnea",
        origen = "Se forma cuando el magma o la lava se enfría y solidifica.",
        color = Color(0xFFC0522D)
    ),
    SEDIMENTARIA(
        nombre = "Sedimentaria",
        origen = "Se forma por la acumulación y compactación de sedimentos a lo largo del tiempo.",
        color = Color(0xFFC9A227)
    ),
    METAMORFICA(
        nombre = "Metamórfica",
        origen = "Se forma cuando otra roca se transforma por efecto de altas presiones y temperaturas.",
        color = Color(0xFF5B7B8C)
    )
}

data class Roca(
    val id: Int,
    val nombre: String,
    val tipo: TipoRoca,
    val descripcion: String,
    val mineralesPrincipales: List<String>,
    val dureza: String,
    val usos: String
)

/** Catálogo fijo de rocas que muestra la app. */
object CatalogoRocas {

    val rocas: List<Roca> = listOf(
        Roca(
            id = 1,
            nombre = "Granito",
            tipo = TipoRoca.IGNEA,
            descripcion = "Roca plutónica de grano grueso, formada por el enfriamiento lento del magma en el interior de la corteza.",
            mineralesPrincipales = listOf("Cuarzo", "Feldespato", "Mica"),
            dureza = "6 – 7 (Mohs)",
            usos = "Encimeras, pisos, monumentos y construcción."
        ),
        Roca(
            id = 2,
            nombre = "Basalto",
            tipo = TipoRoca.IGNEA,
            descripcion = "Roca volcánica oscura y de grano fino. Es la roca más abundante de la corteza oceánica.",
            mineralesPrincipales = listOf("Plagioclasa", "Piroxeno", "Olivino"),
            dureza = "6 (Mohs)",
            usos = "Adoquines, balasto de vías férreas y lana de roca."
        ),
        Roca(
            id = 3,
            nombre = "Obsidiana",
            tipo = TipoRoca.IGNEA,
            descripcion = "Vidrio volcánico natural que se forma cuando la lava se enfría tan rápido que no llegan a crecer cristales.",
            mineralesPrincipales = listOf("Sílice amorfa"),
            dureza = "5 – 5,5 (Mohs)",
            usos = "Herramientas prehistóricas, ornamentos y bisturís de precisión."
        ),
        Roca(
            id = 4,
            nombre = "Arenisca",
            tipo = TipoRoca.SEDIMENTARIA,
            descripcion = "Roca formada por granos de arena cementados. Suele conservar capas visibles de su depósito.",
            mineralesPrincipales = listOf("Cuarzo", "Feldespato"),
            dureza = "6 – 7 (Mohs, según el cemento)",
            usos = "Construcción, fachadas y fabricación de vidrio."
        ),
        Roca(
            id = 5,
            nombre = "Caliza",
            tipo = TipoRoca.SEDIMENTARIA,
            descripcion = "Roca compuesta principalmente de carbonato de calcio, muchas veces a partir de restos de organismos marinos.",
            mineralesPrincipales = listOf("Calcita", "Aragonito"),
            dureza = "3 (Mohs)",
            usos = "Fabricación de cemento y cal, y piedra ornamental."
        ),
        Roca(
            id = 6,
            nombre = "Carbón",
            tipo = TipoRoca.SEDIMENTARIA,
            descripcion = "Roca orgánica formada por restos vegetales acumulados en pantanos y sometidos a presión durante millones de años.",
            mineralesPrincipales = listOf("Carbono", "Materia orgánica"),
            dureza = "1 – 2,5 (Mohs)",
            usos = "Combustible y producción de acero."
        ),
        Roca(
            id = 7,
            nombre = "Mármol",
            tipo = TipoRoca.METAMORFICA,
            descripcion = "Se forma cuando la caliza se recristaliza por calor y presión. Es apreciado por su brillo y sus vetas.",
            mineralesPrincipales = listOf("Calcita", "Dolomita"),
            dureza = "3 – 4 (Mohs)",
            usos = "Escultura, pisos y revestimientos."
        ),
        Roca(
            id = 8,
            nombre = "Pizarra",
            tipo = TipoRoca.METAMORFICA,
            descripcion = "Roca de grano muy fino que se separa fácilmente en láminas planas. Proviene de arcillas y lutitas.",
            mineralesPrincipales = listOf("Mica", "Clorita", "Cuarzo"),
            dureza = "3 – 4 (Mohs)",
            usos = "Tejados, pizarrones y baldosas."
        ),
        Roca(
            id = 9,
            nombre = "Cuarcita",
            tipo = TipoRoca.METAMORFICA,
            descripcion = "Roca muy dura que resulta de la transformación de la arenisca rica en cuarzo.",
            mineralesPrincipales = listOf("Cuarzo"),
            dureza = "7 (Mohs)",
            usos = "Encimeras, balasto y revestimientos resistentes."
        )
    )

    fun buscarPorId(id: Int): Roca? = rocas.find { it.id == id }

    /** Devuelve todas las rocas si [tipo] es null, o solo las de ese tipo. */
    fun filtrar(tipo: TipoRoca?): List<Roca> =
        if (tipo == null) rocas else rocas.filter { it.tipo == tipo }
}
