# Prisma Video · 5.0.1 beta

Reproductor local, gratuito y de código abierto para **Android 8 o posterior**. Sin anuncios, cuentas, rastreadores ni permiso de Internet.

[**⬇ Descargar APK · Prisma Video 5.0.1 beta**](https://github.com/nexlibre/prisma-video/releases/download/v5.0.1-beta/Prisma-Video-5.0.1-beta.apk)

[Versión, novedades, SHA-256 e informe de validación](https://github.com/nexlibre/prisma-video/releases/tag/v5.0.1-beta)

Instala el APK sobre la Beta 2.0.1 para conservar los datos. Esta línea beta mantiene su certificado de desarrollo; no es una distribución con firma privada de producción. Conserva el mismo identificador de aplicación y aumenta versionCode de 4 a 5.

## Consolidación 5.0.1

- **Media3/ExoPlayer** en un núcleo separado, búsqueda exacta, selección de audio y alternativa entre decodificadores Android ante fallos de inicialización.
- **Biblioteca:** miniaturas reales, lista/cuadrícula, búsqueda, carpetas y subcarpetas, filtros de duración/resolución/tamaño/extensión, orden por fecha/nombre/duración/tamaño, favoritos y «Continuar viendo».
- **Listas:** datos anteriores conservados, crear/renombrar/añadir varios/quitar/eliminar; arrastre desde el asa y menú de posiciones. Aleatorio por ciclos sin repetir entradas; cola editable durante reproducción. Cambiar una lista conserva la cola ya iniciada.
- **Subtítulos:** externos SRT/VTT, pistas incrustadas reconocidas por Media3, tres tipografías, tamaño/color/fondo/borde y posición. Puedes conservar los estilos incrustados o usar tus ajustes. Selecciona una carpeta mediante Android para detectar automáticamente archivos SRT/VTT con el mismo nombre base del vídeo.
- **Continuidad por vídeo:** posición, velocidad, pista de audio, pista de subtítulos incrustada, archivo externo, encuadre y zoom. Una pista ausente se ignora con seguridad.
- **Apariencia:** AMOLED Black, Light Minimal, Dynamic Color (Android 12+, paleta de respaldo en versiones anteriores) y Prisma Signature. Cinco acentos; Dynamic Color usa el acento del sistema. Funciones idénticas en todos los temas.
- **Animaciones:** transiciones breves y controles con desvanecimiento, desactivables y respetuosos con la escala de animación de Android.
- **Archivos:** información de duración/resolución/tamaño/formato, compartir, exclusiones y eliminación con confirmación/autorización. Renombrar mediante MediaStore en Android 10+ cuando se conceda permiso.
- **Audio:** presets del ecualizador si la sesión/dispositivo lo admite, amplificación, solo audio, controles multimedia, pausa al desconectar auriculares o perder foco. Tras una interrupción se requiere reanudación manual.
- **Línea de tiempo:** vista previa de fotograma durante el arrastre, con cola de trabajo y memoria acotadas. Usa fotogramas clave aproximados; algunos formatos no permiten extraerlos.

## Reproducción y gestos

PiP manual/automático opcional; doble toque en los extremos para saltar de 1 a 120 segundos (5 por defecto); búsqueda horizontal, brillo a la izquierda, volumen a la derecha, pellizco de 1× a 3× y bloqueo táctil. Los gestos se pueden desactivar individualmente.

Velocidad de 0,25× a 3×, ajustar/llenar/estirar, temporizador, repetición A–B y hasta 100 marcadores con nombre por vídeo. Modo Cine oculta los controles en la interfaz inmersiva. Solo audio continúa en segundo plano; la reproducción de vídeo se pausa cuando deja de estar visible. PiP visible continúa reproduciendo.

SRT/VTT externos: UTF-8, UTF-16 con BOM y Windows-1252; límite de 5 MB y 50 000 entradas, texto acotado y etiquetas eliminadas. Sincronización de −10 000 a +10 000 ms **solo para archivos externos**. Positivo retrasa el texto. El selector de pistas incrustadas cambia la selección sin volver a abrir el archivo.

La ganancia por encima de 100 % depende de LoudnessEnhancer; puede distorsionar. No se anuncia normalización ni limitador de picos. El ecualizador puede no estar disponible en todos los dispositivos.

## Validación y límites

[Inventario por requisito](docs/BETA-5.0.1-AUDIT.md) · [Matriz de comprobaciones](docs/VALIDATION-5.0.1.md) · [Decisión del motor](docs/PLAYBACK-ENGINE-DECISION.md) · [Registro de cambios](CHANGELOG.md)

Actions compila, ejecuta Android Lint y pruebas Java, prueba el motor y la interfaz en emuladores API 26 y 35 y verifica la actualización desde el APK publicado 2.0.1 con el mismo certificado y conservación de datos. Solo publica tras superar estos trabajos.

Los ensayos sintéticos no certifican todos los perfiles, resoluciones ni equipos. HEVC, AV1, 4K y HDR dependen de decodificador y pantalla; requieren una matriz física adicional. No se promete compatibilidad universal ni equivalencia de códecs con VLC. No incluye FFmpeg, LibVLC, SMB, WebDAV, URL, bloqueo de colecciones, normalización, sincronización manual audio/vídeo ni ajuste temporal de subtítulos incrustados. ASS/SSA depende del soporte de la pista por Media3 y no garantiza todos sus efectos; no hay selector de archivos ASS externos.

Las listas/historial siguen usando las preferencias anteriores, con repositorio separado; Room no se introduce sin una migración probada. No se anuncian cifras de inicio, energía o memoria física que no hayan sido medidas. Las posiciones de interfaz están limitadas a unos 24,8 días por vídeo.

## Compilar

JDK 17, SDK 35 y Gradle 8.9:

```sh
./gradlew assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
```

Para la prueba completa de actualización consulta `scripts/verify-upgrade.py`: necesita emulador, adb, apksigner y ambos APK. El APK de pruebas contiene muestras sintéticas; estas no se distribuyen dentro de la aplicación.

## Licencia

Prisma Video: MIT, Copyright 2026 NexLibre. Media3, AndroidX y Guava: Apache 2.0. Avisos y texto de licencia incluidos en Ajustes → Acerca de → Licencias y en `app/src/main/assets/`.
