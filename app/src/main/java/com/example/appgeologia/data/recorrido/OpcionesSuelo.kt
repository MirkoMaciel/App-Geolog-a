package com.example.appgeologia.data.recorrido

/**
 * Una opción de la planilla. [codigo] es lo que se imprime en el PDF (igual que en
 * la planilla en papel) y [descripcion] es el texto completo que se muestra en la app.
 */
data class Opcion(val codigo: String, val descripcion: String = codigo)

/**
 * Opciones fijas de la planilla de descripción de suelos.
 *
 * Cada lista de [lineas] representa cómo se imprimen en el PDF: la mayoría entra en una
 * sola línea, pero adhesividad y plasticidad ocupan dos, como en la planilla original.
 */
class GrupoOpciones(val lineas: List<List<Opcion>>) {
    constructor(vararg opciones: Opcion) : this(listOf(opciones.toList()))

    val todas: List<Opcion> get() = lineas.flatten()
}

object OpcionesSuelo {

    val limiteDefinicion = GrupoOpciones(
        Opcion("MA", "Muy abrupto"),
        Opcion("A", "Abrupto"),
        Opcion("C", "Claro"),
        Opcion("G", "Gradual"),
        Opcion("D", "Difuso")
    )

    val limiteTopografia = GrupoOpciones(
        Opcion("Pl", "Plano"),
        Opcion("Ond", "Ondulado"),
        Opcion("Irr", "Irregular"),
        Opcion("Que", "Quebrado")
    )

    val adhesividad = GrupoOpciones(
        listOf(
            listOf(Opcion("No adhesivo"), Opcion("Lig adhesivo", "Ligeramente adhesivo")),
            listOf(Opcion("Mod adhesivo", "Moderadamente adhesivo"), Opcion("Muy adhesivo"))
        )
    )

    val plasticidad = GrupoOpciones(
        listOf(
            listOf(Opcion("No plástico"), Opcion("Lig plástico", "Ligeramente plástico")),
            listOf(Opcion("Mod plástico", "Moderadamente plástico"), Opcion("Muy plástico"))
        )
    )

    val estructuraGrado = GrupoOpciones(
        Opcion("SE", "Sin estructura"),
        Opcion("Déb", "Débil"),
        Opcion("Mod", "Moderada"),
        Opcion("Fte", "Fuerte")
    )

    val reaccionHcl = GrupoOpciones(
        Opcion("NoEf", "No efervescente"),
        Opcion("MSEf", "Muy suavemente efervescente"),
        Opcion("SEf", "Suavemente efervescente"),
        Opcion("FEf", "Fuertemente efervescente"),
        Opcion("VEf", "Violentamente efervescente")
    )

    val concentracionTipo = GrupoOpciones(
        Opcion("D", "Difuso"),
        Opcion("M", "Masas"),
        Opcion("N", "Nódulos"),
        Opcion("C", "Concreciones"),
        Opcion("Cr", "Cristales")
    )

    val humedad = GrupoOpciones(
        Opcion("Seco"),
        Opcion("Fres.", "Fresco"),
        Opcion("Húm.", "Húmedo"),
        Opcion("Sat.", "Saturado")
    )

    val raicesCantidad = GrupoOpciones(
        Opcion("Pocas"),
        Opcion("Comunes"),
        Opcion("Abund.", "Abundantes")
    )

    // --- Sugerencias para campos de texto libre (se pueden escribir otros valores) ---

    /**
     * Formas de las concentraciones (y rasgos redoximórficos). En la app se elige por la
     * figura; en el PDF se imprime solo el nombre ([Opcion.codigo]).
     */
    val formasConcentracion = listOf(
        Opcion("Cilíndrica", "Cuerpos tubulares y elongados. Ej.: túneles de lombrices rellenados."),
        Opcion("Dendrítica", "Cuerpos tubulares, elongados y ramificados. Ej.: seudomorfos de raíces."),
        Opcion("Irregular", "Cuerpos de espaciado o formas no repetitivos."),
        Opcion("Laminar", "Capas tabulares o lentes relativamente finas. Ej.: lamelas."),
        Opcion("Esférica", "Cuerpos bien redondeados a toscamente esféricos. Ej.: municiones de Fe/Mn."),
        Opcion("Hilos", "Filamentos finos y elongados (< 1 mm), no dendríticos. Ej.: hilos de CaCO₃."),
        Opcion("Reticulada", "Cuerpos entrelazados con espaciado similar. Ej.: plintita.")
    )

    val sugerenciasTextura = listOf(
        "Arenoso", "Areno franco", "Franco arenoso", "Franco", "Franco limoso", "Limoso",
        "Franco arcillo arenoso", "Franco arcilloso", "Franco arcillo limoso",
        "Arcillo arenoso", "Arcillo limoso", "Arcilloso"
    )

    val sugerenciasResistencia = listOf(
        "Suelto", "Blando", "Lig. duro", "Duro", "Muy duro", "Friable", "Firme"
    )

    val sugerenciasEstructuraTipo = listOf(
        "Granular", "Migajosa", "Bloques angulares", "Bloques subangulares",
        "Laminar", "Prismática", "Columnar", "Masiva", "Grano suelto"
    )

    val sugerenciasTamano = listOf("Muy fino", "Fino", "Medio", "Grueso", "Muy grueso")

    /** Tamaños de raíces (en femenino, como se anotan en la planilla). */
    val sugerenciasTamanoRaices = listOf("Muy finas", "Finas", "Medias", "Gruesas", "Muy gruesas")

    val sugerenciasCantidad = listOf("Pocas", "Comunes", "Abundantes")

    val sugerenciasRaicesUbicacion = listOf("Entre agregados", "Dentro de agregados", "En grietas")

    val sugerenciasRedondez = listOf("Angular", "Subangular", "Subredondeado", "Redondeado")

    val sugerenciasClaseDrenaje = listOf(
        "Excesivamente drenado", "Algo excesivamente drenado", "Bien drenado",
        "Moderadamente bien drenado", "Imperfectamente drenado",
        "Pobremente drenado", "Muy pobremente drenado"
    )

    val sugerenciasErosion = listOf("Nula", "Leve", "Moderada", "Severa")

    val sugerenciasExposicion = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")

    val sugerenciasRelieve = listOf("Normal", "Subnormal", "Cóncavo", "Plano", "Excesivo")
}
