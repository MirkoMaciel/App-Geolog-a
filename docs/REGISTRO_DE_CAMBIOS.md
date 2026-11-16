# Registro de cambios — Patagonia Ground Note (PGN)

## Versión 1.2 — 11 de noviembre de 2026

Rama `feature/feedback-docente`. APK: `APK/PGN_v1.2.apk` (21 MB, `versionCode = 3`).
Se instala encima de la 1.1 sin perder datos.

- **Nombre e ícono:** la app se llama **PGN** en el celular; "Patagonia Ground Note" en el
  inicio ("Bienvenido/a a Patagonia Ground Note") y en el pie de los PDF. Ícono nuevo.
- **Color Munsell:** tabla de colores (10YR y 7.5YR) para elegir el color seco y húmedo.
  En el PDF solo va la notación en texto.
- **Textura:** triángulo textural USDA para marcar arena/limo/arcilla y obtener la clase.
- **Concentraciones:** "D" ahora es Difuso; la forma se elige con figuras y en el PDF va la
  forma elegida. **Raíces:** tamaños muy finas, finas, medias, gruesas, muy gruesas.
- **PDF:** un solo PDF vigente por parada/salida; si ya existe, aviso con Reemplazar /
  Usar el anterior / Guardar como copia nueva. Limpieza automática de exportaciones viejas.
  Fotos reducidas a 150 dpi (PDF mucho más livianos).
- **Mapa sin conexión:** la app trae un mapa de la zona de Escalante (Chubut), de Camarones a
  Pico Truncado y de la costa a Sarmiento (8,5 MB, datos OpenStreetMap). Sin internet se usa
  solo, con un aviso debajo del mapa; también se puede elegir con "Usar mapa descargado".
- **Pruebas:** prueba integral de exportación (`TestPDF/`) y prueba del mapa sin conexión.

---

## Versión 1.1 — 3 y 4 de noviembre de 2026

Cambios realizados a partir del feedback de la docente que usó la app.
Se dividieron en **cambios generales** (estructura y formato) y **cambios específicos**
(tabulaciones y datos). En esta sesión se hicieron los **generales** y la **paleta de colores**.

---

### Ramas de Git

| Rama | Contenido | Estado |
|---|---|---|
| `main` | Versión que probó la docente | Sin cambios |
| `feature/feedback-docente` | Cambios generales del feedback | Commit `0e3c8df` |
| `feature/paleta-colores` | Sale de `feature/feedback-docente` + paleta de colores + versión 1.1 | **Sin commitear** |

Ninguna rama está subida a GitHub todavía (solo existen en la PC).

---

### 1. Cambios generales (rama `feature/feedback-docente`, commit `0e3c8df`)

#### 1.1 Pantalla de Inicio
- "Recorridos" pasó a llamarse **"Salidas"** (también en el menú inferior).
- Contadores: quedaron **"Salidas totales"** y **"Salidas en curso"** (se quitaron "Paradas" y "Fotos").
- La tarjeta **"Última salida activa"** muestra la salida en curso con movimiento más reciente
  (última parada u horizonte cargado), con la fecha del último movimiento y el botón "Continuar salida".
- El botón principal pasó a ser **"Nueva salida de campo"**.
- Las 4 opciones (Mis salidas, Mis exportaciones, Guía de la planilla, Cómo se usa) quedaron
  dentro del desplegable **"Más opciones"**.

#### 1.2 "Recorrido" → "Salida" en toda la app
- Todos los textos visibles: títulos, botones, menús, mensajes, ayuda y el PDF
  ("Salida de campo: …", "Salida: … · Parada N°", archivos `Salida_….pdf").
- Concordancia de género corregida: *una* salida, *finalizada*, *reabrila*.
- En el código las clases siguen llamándose `Recorrido` (solo cambió lo que ve el usuario).

#### 1.3 Ventana "Nueva salida de campo"
- Muestra la **fecha y hora de inicio** del momento en que se tocó el botón, **sin poder editarse**;
  esa es la fecha que se guarda como inicio de la salida.
- Campo **"Nombre salida de campo \*"**.
- Tipo de salida: se mantiene "Suelos".
- Proyecto: texto arriba *"El nombre debe ser específico de la salida de campo, ejemplo: Cerro Chenque"*
  y ejemplo dentro del campo *"Ej.: Cerro Chenque"*.

#### 1.4 Vista de una salida
- La barra superior muestra **solo el nombre** de la salida.
- Tarjeta **"Información de la salida"** al principio: tipo de salida, fecha y hora de inicio,
  estado, cantidad de paradas (y proyecto, si tiene).
- **"Paradas (n)"** y **"Mapa"** pasaron a ser desplegables, en ese orden.
- El mapa se puede mover con el dedo sin que se desplace la pantalla.

#### 1.5 Vista de una parada
- Se quitaron la hora y la posición de arriba (ya se ven en la vista de la salida).
- Se quitaron las pestañas: todo quedó en una sola pantalla con desplazamiento.
- **"Ubicación y sitio"** pasó a ser el desplegable **"Descripción general"** (mismos campos de texto libre).
- Los "items" pasaron a llamarse **"Horizontes"** en toda la app:
  - Botón **"Añadir nuevo horizonte"**.
  - Sin horizontes: solo se ve el botón.
  - Con al menos uno: aparece el desplegable **"Horizontes (n)"** debajo del botón.

#### 1.6 Vista de un horizonte
- El título "Observación" pasó a **"Descripción general horizonte"**.
- Ejemplos en los campos: Título *"Ej.: Primer horizonte"*, Descripción
  *"Ej.: Excavación realizada por nosotros"* (aparecen al tocar el campo).
- **Fotos** como desplegable, con el subtítulo **"¿Querés añadir imágenes?"**
  (cerrado si todavía no hay fotos).
- El campo de la planilla de suelos se llama ahora "Nombre del horizonte (ej. A, Bt1, C)".

#### 1.7 Mis salidas: borrar varias
- **"Seleccionar"** en la barra superior, o **mantener presionada** una salida, activa el modo selección.
- Casillas en cada tarjeta, botones **Todas / Ninguna** y **Eliminar**; la X o "atrás" cancelan.
- Confirmación: **"¿Realmente querés borrar n salidas?"**, con la lista de nombres y aviso de que
  se borran paradas, horizontes y fotos (los PDF exportados se conservan).

#### 1.8 Mis exportaciones agrupadas por salida
- Los PDF se muestran **agrupados por salida en desplegables** (forma dinámica: solo aparecen
  las salidas que tienen exportaciones).
- Cada PDF indica qué contiene ("Parada 2" o "Salida completa"), fecha y tamaño.
- PDFs de salidas borradas: quedan como "Nombre (salida eliminada)".
- PDFs sin salida identificable: grupo "Otras exportaciones".
- Técnica: cada exportación se anota en `exportaciones/indice.json`; para los PDF anteriores
  se deduce la salida por el nombre del archivo.

#### 1.9 Otros
- Nuevo componente reutilizable `SeccionDesplegable` (todos los desplegables de la app).
- Guía de estudio del código en `docs/Guia_del_codigo.pdf` (y su fuente `.html`).

---

### 2. Paleta de colores (rama `feature/paleta-colores`, sin commitear)

Se aplicó la **Opción 1 "Cartografía y Estratos"**:

| Color | Código | Uso |
|---|---|---|
| Caliza | `#F4F1EA` | Fondo de todas las pantallas (tarjetas en `#E7E2D8`) |
| Pizarra | `#2C3E50` | Barras superiores, encabezado de Inicio, texto principal, botones comunes, línea del mapa |
| Óxido de hierro | `#C85A17` | Acción principal: Nueva salida de campo, Nueva parada, Añadir nuevo horizonte, marcadores del mapa, etiqueta "En curso" |
| Olivino | `#6B8E23` | Estado "Finalizada", íconos secundarios, pestaña elegida del menú inferior |

- Se incluyó una versión para el **modo oscuro** del teléfono.
- El **PDF no cambió** (sigue en negro sobre blanco, como la planilla en papel).
- Archivos: `ui/theme/Color.kt` y `ui/theme/Theme.kt`.

---

### 3. Versión y APK
- Versión subida a **1.1** (`versionCode = 2`) en `app/build.gradle.kts`.
- APK generado: `app/build/outputs/apk/debug/Geologia_v1.1.apk` (12,9 MB), con todos los
  cambios generales y la paleta. Se instala encima de la versión anterior sin perder datos.

### 4. Verificación
- La app compila y pasan las 9 pruebas automáticas del repositorio.
- Cada cambio se revisó en el emulador con capturas.
- Pendiente de confirmar a mano: abrir y cerrar los desplegables "Paradas" y "Mapa" en la vista de salida.

---

### 5. Pendiente para la próxima sesión
1. Revisar la paleta en el celular (con luz de sol) y decidir: dejar la Opción 1, ajustarla o probar la Opción 2 "Mineralogía Pura".
2. **Commitear** la paleta en `feature/paleta-colores` cuando esté aprobada.
3. Definir si se suben las ramas a GitHub y cómo se unen (`feature/paleta-colores` → `feature/feedback-docente` → `main`).
4. Empezar los **cambios específicos** del feedback (tabulaciones y datos de la planilla).
5. Consultas abiertas de sesiones anteriores:
   - Significado de la "D" en concentraciones (tipo).
   - Formas de las concentraciones (en papel son íconos).
   - Qué indican los puntos rojos de la planilla original.
6. Opcional: renombrar en el código `Recorrido` → `Salida` e `ItemParada` → `Horizonte`, y actualizar la guía del código con los cambios de hoy.
