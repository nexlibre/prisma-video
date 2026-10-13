# Prisma Video · 2.0.1 beta

Reproductor local para Android 8 o posterior. Minimalista, gratuito y abierto, sin anuncios, cuentas ni permiso de Internet.

## Descargar para Android

[**⬇ Descargar APK · Prisma Video 2.0.1 beta**](https://github.com/nexlibre/prisma-video/releases/download/v2.0.1-beta/Prisma-Video-2.0.1-beta.apk)

[Ver la versión y sus archivos](https://github.com/nexlibre/prisma-video/releases/tag/v2.0.1-beta)

Descarga la APK y abre el archivo para instalarlo. Se instala sobre las versiones anteriores conservando los ajustes. También se encuentra en **Assets / Recursos** de la versión.

## Reproducción

- Biblioteca local con búsqueda por nombre, archivos externos y exclusión persistente de carpetas/subcarpetas.
- Temas día y negro AMOLED, acentos menta/ámbar/azul y controles vectoriales.
- Línea de tiempo fina; anterior, reproducir/pausar y siguiente. Sin botones grandes de salto.
- Doble toque en los extremos para saltar entre 1 y 120 segundos, por defecto 5. El centro no provoca saltos.
- Gestos de búsqueda horizontal, brillo a la izquierda y volumen a la derecha. Se pueden desactivar por separado.
- Cola con todos/repetir uno/aleatorio y reproducción del siguiente archivo opcional.
- Solo audio con controles multimedia en la notificación y reproducción en segundo plano.
- Amplificación hasta 200 % con LoudnessEnhancer cuando el dispositivo lo admite. La ganancia máxima es aproximadamente +6 dB; el volumen del sistema también influye. En equipos incompatibles se limita a 100 %.
- Reanudación de la posición de cada vídeo, con pausa al desconectar auriculares y gestión del foco de audio.

## Opciones avanzadas

En el reproductor hay accesos con íconos a **Subtítulos** y **PiP**. El menú de tres puntos agrupa las demás funciones:

- **PiP:** ventana flotante del sistema con controles multimedia. Entrada manual o automática opcional al ir a Inicio. Si Android lo bloquea, habilita Pantalla en pantalla en la información de esta app.
- **Subtítulos SRT/VTT externos:** selecciona el archivo desde Android; se recuerda por vídeo mientras se mantenga el permiso de lectura. UTF-8, UTF-16 con BOM y Windows-1252. Hasta 5 MB y 50 000 líneas temporizadas; texto por línea limitado a 4096 caracteres. Estilo de texto plano, con etiquetas de formato eliminadas.
- **Sincronización:** de −10 000 a +10 000 ms. Un valor positivo retrasa el texto; uno negativo lo adelanta. Tamaño de 14 a 32, tres colores y fondo opcional.
- **Velocidad:** de 0,25× a 3×, conservando el tono cuando lo admite el motor.
- **Pistas de audio:** selección entre las pistas que informa el dispositivo.
- **Imagen:** ajustar sin recortar, llenar/recortar y estirar. Pellizcar para ampliar de 1× a 3×; el zoom se restablece en cada vídeo.
- **Bloqueo:** desactiva los controles y gestos; usa el ícono de desbloqueo para recuperarlos.
- **Temporizador:** pausa dentro de 5, 15, 30 o 60 minutos, o al terminar el vídeo. Se cancela al cerrar la reproducción; no persiste si Android termina el proceso.
- **Marcadores:** hasta 100 momentos con nombre por archivo. También hay repetición A–B; A y B deben estar separados al menos medio segundo. El fragmento se restablece al cambiar de vídeo.
- **Información:** duración, resolución, tipo, carpeta y velocidad.

## Novedades de 2.0.1 beta

- Listas de reproducción locales: crear, renombrar, añadir varios vídeos, quitar y ordenar por posición. Se conservan al cerrar la app. Eliminar una lista conserva los archivos.
- Explorador de carpetas integrado con conteos; abre una carpeta para buscar y reproducir sus vídeos.
- Menú al mantener pulsado un vídeo o tocar sus tres puntos: reproducir, añadir a lista, favoritos, compartir, información y eliminar archivo.
- Eliminación permanente con confirmación y autorización de Android cuando corresponda; limpia las listas, favoritos, historial y cola después del borrado. Cancelar conserva el archivo y sus referencias.

Las listas solo reproducen archivos disponibles y autorizados en la biblioteca. Las exclusiones, los permisos parciales y el filtro de duración también se respetan dentro de las listas; sus referencias se conservan si un archivo queda temporalmente oculto. El orden de una lista se cambia en el menú de cada vídeo. Una cola ya iniciada conserva su orden hasta abrir nuevamente la lista.

## Biblioteca y ajustes

Los íconos de la biblioteca permiten abrir el historial de los últimos 30 vídeos, filtrar favoritos, ordenar, actualizar y abrir un archivo. Se puede ordenar por fecha de incorporación, nombre o duración, y filtrar vídeos de menos de 10, 30 o 60 segundos.

Los ajustes se agrupan en Apariencia, Reproducción, Gestos y controles, Subtítulos, Biblioteca y privacidad, y Acerca de. Puedes desactivar la reanudación, configurar el tiempo de ocultación y borrar historial/posiciones sin borrar vídeos, favoritos ni marcadores.

Las exclusiones ocultan archivos de la biblioteca y nuevas colas; no los borran ni impiden abrirlos voluntariamente con el selector. No modifican una cola ya iniciada. El brillo solo afecta esta ventana. La reproducción se pausa cuando la app deja de ser visible, salvo en modo Solo audio; PiP visible sigue reproduciendo.

En Android 14 o posterior, la biblioteca respeta los vídeos que autorices, incluida una selección parcial. El botón Actualizar permite revisar el permiso.

## Compilación y validación

JDK 17, Android SDK 35 y Gradle 8.9:

```sh
./gradlew assembleDebug lintDebug
mkdir -p build/subtitle-tests
javac -d build/subtitle-tests app/src/main/java/org/nexlibre/prismavideo/SubtitleTrack.java tests/SubtitleTrackTest.java
java -cp build/subtitle-tests SubtitleTrackTest
```

GitHub Actions compila, ejecuta Android Lint y las pruebas del lector SRT/VTT y del orden de las listas y publica automáticamente la APK beta. Los cambios exclusivamente de documentación no recompilan. La clave de desarrollo incluida mantiene compatibles las instalaciones de las betas; una distribución de producción requiere una firma privada propia.

El lector de subtítulos se prueba con límites temporales, solapamientos, búsquedas después de saltos, codificaciones, orden, archivos inválidos y límites de tamaño. La reproducción, PiP, el cambio de orientación, los gestos y el aspecto visual requieren pruebas en dispositivos. No se afirma equivalencia con VLC o KMPlayer en cobertura de formatos: el motor MediaPlayer usa los códecs de Android. Esta versión no incorpora FFmpeg, streaming, ASS/SSA ni selección de subtítulos incrustados.

## Licencia

MIT. Copyright 2026 NexLibre.
