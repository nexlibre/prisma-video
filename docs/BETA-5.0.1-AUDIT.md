# Consolidación 5.0.1: inventario y estado verificable

Base auditada: a4b6a2ea7a410938039dcbc07def1beb9cf6f747 (2.0.1-beta).
Este documento describe código, no certifica comportamiento en dispositivos.
La actualización completa **no está terminada**. No se ha publicado 5.0.1.

| Área | Base existente | Trabajo en esta rama | Pendiente para 5.0.1 |
|---|---|---|---|
| Motor | MediaPlayer, SurfaceView | Núcleo PlaybackEngine con Media3 1.5.1; búsqueda exacta; alternativa entre decodificadores Android | Matriz de contenedores/códecs, HDR y resoluciones; recuperación; evaluar FFmpeg/LibVLC |
| Arquitectura | PlaybackService separado; MainActivity reúne biblioteca, interfaz y preferencias | Motor encapsulado; Activity sin acceso directo al reproductor | Repositorios, ViewModel, componentes de gestos/temas/errores; evaluar Room y Media3 MediaSession |
| Interfaz | Íconos vectoriales, controles compactos e inmersivos | Conservada | Renovación, tabletas, transiciones y reducción de movimiento |
| Subtítulos | SRT/VTT externos; lector acotado; sincronización; tamaño, tres colores y fondo | Lector existente conservado | Incrustados, ASS/SSA, asociación automática, idioma, tipografías, posición y estilos avanzados |
| Audio | Pistas, velocidad, LoudnessEnhancer, modo solo audio y foco | Selección de pistas Media3; pausa ante pérdidas de foco sin reanudación automática | Ecualizador, sincronización AV, normalización, multicanal y validación de interrupciones |
| Biblioteca | MediaStore asíncrono, lista, búsqueda, carpetas, favoritos, historial, exclusiones | Conservada | Miniaturas, cuadrícula, subcarpetas, filtros completos, selección múltiple, renombrado, actualización incremental |
| Listas | JSON en preferencias; crear/renombrar/eliminar, selección al añadir, ordenar con menú | Aleatorio por ciclos sin repetición; prueba determinista | Arrastrar/soltar, cola editable, escalabilidad y migraciones |
| Gestos | Doble toque, búsqueda, brillo/volumen, zoom, bloqueo y desactivación | Conservados | Ajustes refinados, indicadores y prevención de accidentes |
| Avanzado | PiP, segundo plano solo audio, velocidad, temporizador, A–B, marcadores, reanudación e imagen | Conservado en código | Miniaturas de búsqueda y continuidad de todas las preferencias por vídeo |
| Temas | AMOLED/día, tres acentos | Conservados | Dynamic Color, Prisma Signature y personalización completa |
| Diferenciación | Inmersión, marcadores, información y acceso externo SAF | Sin nuevos permisos | Modo Cine refinado, línea de tiempo, colecciones; evaluar USB, SMB, WebDAV y URL |
| Rendimiento | ListView y escaneo en ejecutor | Media3; componente de reproducción separado | Mediciones, caché de miniaturas, RAM/energía y bibliotecas grandes |
| Seguridad | Scoped Storage, permisos parciales, borrado autorizado, límites de subtítulos | CI sin permiso de escritura ni publicación automática | Revisar todas las entradas, archivos ausentes, persistencia SAF y servicios |
| Pruebas | Pruebas Java de subtítulos y orden; assembleDebug y Lint en CI | Prueba de aleatorio; CI para rama/PR, APK de desarrollo como artefacto | Emuladores, dispositivos, migraciones y matriz multimedia |
| Repositorio | MIT, README, release, firma beta de desarrollo | Inventario y matriz de calidad; desarrollo aislado de release | CHANGELOG final, documentación modular y firma privada de producción |
| Publicación | APK 2.0.1 disponible | No reemplaza APK anterior ni declara 5.0.1 completa | Compilar, validar actualización y firma, aprobar matriz y publicar APK final |

## Decisiones y límites

- Conserva applicationId y todos los nombres y claves de SharedPreferences. No hay una migración destructiva de datos.
- Media3 se fija a 1.5.1 para esta primera integración; actualizarlo requiere volver a comprobar minSdk, compileSdk y regresiones. Su licencia Apache-2.0 debe acompañar la distribución.
- Decoder fallback prueba otro decodificador registrado por Android cuando falla la inicialización. No incorpora decodificadores FFmpeg ni garantiza software, AVI, HEVC, AV1 o HDR en todos los equipos.
- El servicio mantiene MediaSession de Android mientras se valida la migración del motor; no se anuncia migración a Media3 MediaSession.
- Las posiciones de interfaz siguen limitadas a enteros de milisegundos (~24,8 días); no se certifican vídeos de duración mayor.
- Se opta por pausar ante interrupciones y exigir reproducción manual posterior. Ninguna aplicación puede garantizar detectar todas las grabaciones ajenas si estas no solicitan foco de audio.
- El entorno local no tiene javac/SDK ni puede resolver services.gradle.org. Compilación, Lint y pruebas Java se delegan a CI; no se dan por aprobadas antes del resultado.
- Se mantiene el número de versión 2.0.1 durante esta fase interna. El artefacto de CI se llama desarrollo 5.0.1, no release 5.0.1 instalable final.

## Secuencia de integración

1. Validar motor, ciclo del servicio, foco, posiciones y pruebas de regresión.
2. Completar subtítulos/pistas, persistencia por vídeo y reproducción avanzada.
3. Separar biblioteca y listas, incorporar metadatos, miniaturas y edición segura.
4. Renovar interfaz, temas y gestos con medición de rendimiento.
5. Completar la matriz de calidad y actualizar este inventario con resultados.
6. Subir versionCode, verificar firma y actualización sin pérdida de datos; documentar capacidades comprobadas y publicar una única beta.
