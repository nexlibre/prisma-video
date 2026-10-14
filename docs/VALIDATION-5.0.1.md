# Matriz de validación 5.0.1

No marcar una casilla por una compilación exitosa. Registrar dispositivo/API,
archivo de prueba y hash, pasos, resultado, errores, tiempos y commit.

| Comprobación | Casos mínimos | Estado |
|---|---|---|
| CI | Java: SRT/VTT, listas y aleatorio; assembleDebug; lintDebug | Aprobada en Actions 38502061873: Java, assembleDebug y lintDebug |
| Motor | MP4/H264/AAC; MKV; WebM/VP9; HEVC; AV1; MOV; AVI; MPEG; corrupto e inaccesible | No ejecutada |
| Hardware | 720p/1080p/2K/4K; HDR10/HLG; códec ausente y fallback | No ejecutada |
| Servicio | Abrir/cerrar, cambiar vídeo durante preparación, superficie destruida, audio solo, notificación | No ejecutada |
| Foco | Llamada, otro reproductor, mensaje de voz, grabación, auriculares; nunca reanudar indebidamente | No ejecutada |
| Pistas | Varios idiomas, cambio sin reinicio, pista no compatible | No ejecutada |
| Subtítulos | SRT/VTT, UTF8/16/1252, solapamientos, salto adelante/atrás, límites y permisos revocados | Java existente; dispositivo pendiente |
| Pantalla | Rotación durante carga/reproducción, PiP manual/automático, bloqueo y regreso | No ejecutada |
| Gestos | Doble toque, brillo/volumen, búsqueda, zoom y desactivación | No ejecutada |
| Biblioteca | 10/1000/10000 archivos; permisos denegados/parciales; exclusión y archivos desaparecidos | No ejecutada |
| Listas | Crear, renombrar, ordenar, añadir/quitar, reiniciar proceso; cola y aleatorio | Java existente/nuevo; dispositivo pendiente |
| Almacenamiento | Borrado cancelado y autorizado, SAF, SD/USB, permisos persistentes | No ejecutada |
| Actualización | Instalar 2.0.1, crear todos los datos, actualizar con firma idéntica y comprobarlos | No ejecutada |
| Rendimiento | Inicio frío/caliente, apertura y seek p50/p95, RAM, fotogramas y energía; antes/después | Sin mediciones |
| APIs | Android 8, 10, 13, 14/15 y dispositivo moderno; teléfono/tableta | No ejecutada |

La firma de desarrollo existente permite comparar betas, pero no sustituye una
firma privada de producción. Verificar certificado, applicationId y versionCode
antes de cualquier publicación. FFmpeg/LibVLC requieren un análisis específico
de licencia y tamaño antes de incorporarlos.

## Prueba automatizada del motor

`PlaybackEngineTest` se ejecuta en emuladores API 26 y 35. Usa un archivo
sintético original de dos segundos, H.264 64×64 y AAC, sin material de terceros.
Se guarda en Base64 entre los recursos de androidTest y se decodifica en la
caché del dispositivo; no se incorpora al APK principal.
SHA-256 del MP4: `210e4137384cc17cda14c411b62cc68861885882e29858504f1e477aebe48644`.

Comprueba preparación, duración, selección de audio, seek, velocidad,
progreso, pausa, finalización y error de archivo ausente. Usa superficie nula:
no demuestra presentación de fotogramas, gestos, PiP, foco del servicio ni HDR.
Los informes se conservan como artefactos de Actions. Estado: aprobado en ambas APIs (dos pruebas por emulador; cuatro ejecuciones), en el commit `3432d56b405e6ea719ba43509c051b0c2ca1645f`.

Evidencia: https://github.com/nexlibre/prisma-video/actions/runs/38502061873

La validación del servicio, foco, interfaz, migración de instalación, códecs distintos y dispositivos reales sigue pendiente.
