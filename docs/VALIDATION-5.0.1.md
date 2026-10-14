# Matriz de validación 5.0.1

No marcar una casilla por una compilación exitosa. Registrar dispositivo/API,
archivo de prueba y hash, pasos, resultado, errores, tiempos y commit.

| Comprobación | Casos mínimos | Estado |
|---|---|---|
| CI | Java: SRT/VTT, listas y aleatorio; assembleDebug; lintDebug | Pendiente de ejecución remota |
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
