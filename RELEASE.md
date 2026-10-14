# Prisma Video 5.0.1 beta

Actualización de consolidación del reproductor local: motor Media3, biblioteca con miniaturas y filtros, temas, subtítulos incrustados y continuidad por vídeo.

## Disponible

- Media3/ExoPlayer separado de la interfaz, búsqueda exacta y selección de audio.
- Biblioteca con miniaturas, lista/cuadrícula, filtros y selección múltiple; carpetas con subcarpetas y «Continuar viendo».
- Arrastre para ordenar listas y gestión de la cola; aleatorio por ciclos sin repetir vídeos antes de completar el recorrido.
- Subtítulos SRT/VTT externos y selección de pistas incrustadas; tipografía, estilo y posición. Detección de SRT/VTT desde una carpeta autorizada mediante Android.
- Continuidad por archivo: posición, velocidad, audio/subtítulos, encuadre y zoom.
- AMOLED Black, Light Minimal, Dynamic Color y Prisma Signature; animaciones desactivables.
- Vista previa de fotograma durante el arrastre de la línea de tiempo.
- Renombrado en Android 10+ con autorización, además de compartir y borrar con confirmación.
- Ecualizador con presets cuando el dispositivo lo admite, modo solo audio y pausa sin reanudación automática ante pérdidas de foco.
- PiP, gestos configurables, temporizador, repetición A–B, marcadores y controles multimedia conservados.

## Instalación y calidad

El APK mantiene el certificado de desarrollo de 2.0.1 y aumenta versionCode a 5. Se puede instalar como actualización; no desinstales la versión anterior si quieres conservar datos. SHA256SUMS permite comprobar la integridad del archivo.

La publicación automática exige compilación, Lint, pruebas Java, pruebas instrumentadas en API 26/35 y prueba real de instalación sobre el APK anterior, con comprobación del certificado y preferencias/listas/favoritos/exclusiones/marcadores/subtítulos/historial/posiciones.

Consulta el informe adjunto y los resultados de Actions para distinguir código implementado, ensayos automáticos y validación física pendiente.

## Limitaciones conocidas

- Códecs/perfiles, HEVC/AV1, alta resolución y HDR dependen del dispositivo; no hay soporte universal certificado.
- Ecualizador y amplificación dependen de la sesión de audio. La amplificación puede distorsionar; no incorpora normalizador ni limitador.
- Sincronización de subtítulos solo para SRT/VTT externos. Los efectos ASS/SSA no tienen cobertura completa certificada; no se cargan archivos ASS externos.
- Sin ajuste manual de desfase audio/vídeo, redes SMB/WebDAV/URL ni bloqueo de colecciones.
- Miniaturas de búsqueda aproximadas y sujetas a soporte de extracción del formato.
- No se ha completado la matriz física de rendimiento, energía, HDR, accesorios y fabricantes. PiP probado en emuladores no garantiza idéntico comportamiento en todas las capas de Android.
- Firma de desarrollo conservada para las betas; una publicación de producción necesita una firma privada diferente y una estrategia de migración.

No se anuncian las funciones excluidas como disponibles ni equivalencia total con VLC, MX Player o KMPlayer.
