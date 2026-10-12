# Prisma Video · 1.0.1 beta

Reproductor local para Android 8.0 o posterior. Interfaz minimalista en español, sin conexión a Internet, anuncios ni cuentas.

## Incluido

- Biblioteca de vídeos de MediaStore, búsqueda por nombre y actualización manual.
- Abrir un vídeo con el selector de archivos de Android o desde otra aplicación.
- Exclusión persistente de carpetas y sus subcarpetas. Se pueden marcar las carpetas detectadas o introducir una ruta relativa, como `Movies/Privados`.
- Tema día y noche negro AMOLED, recordado entre aperturas.
- Reproducir y pausar, vídeo anterior y siguiente y línea de tiempo fina. Controles con íconos vectoriales, sin botones de salto ni paneles grandes.
- Doble toque en el 30 % izquierdo para retroceder y en el 30 % derecho para avanzar. El centro no provoca saltos. Por defecto 5 segundos; editable de 1 a 120 segundos en Ajustes.
- Deslizamiento horizontal sobre el vídeo: busca hasta 120 segundos a lo largo del ancho de la pantalla, con vista previa de tiempo; aplica el salto al soltar.
- Deslizamiento vertical a la izquierda: brillo de esta ventana. A la derecha: volumen del reproductor.
- Cola de la lista visible, repetir todos, repetir uno y aleatorio evitando repetir inmediatamente el mismo archivo cuando hay varias opciones.
- Modo solo audio con notificación multimedia, controles y reproducción en segundo plano/con pantalla apagada.
- Volumen de 0 a 200 %. Hasta 100 % regula la señal; por encima usa LoudnessEnhancer, con ganancia máxima de unos +6 dB. Si el efecto no está disponible se limita al 100 % y se informa al usuario. El volumen del sistema también influye en lo que se escucha.
- Pausa al desconectar auriculares y gestión del foco de audio.
- Pantalla completa horizontal, vídeo ajustado sin deformación y controles que se ocultan tras 4 segundos. Un toque muestra u oculta tanto los controles como el título. Las opciones de volumen y amplificación se encuentran en el menú de tres puntos.

## Uso

Instala `Prisma-Video-1.0.1-beta.apk`. Autoriza el acceso a vídeos y, si lo deseas, las notificaciones. En Android 14 o posterior puedes autorizar solo una selección: la biblioteca mostrará los archivos autorizados. El botón Actualizar permite revisar ese acceso. También puedes usar Abrir archivo.

Las exclusiones ocultan carpetas de la biblioteca y de nuevas colas; no borran archivos ni impiden abrir voluntariamente un archivo con el selector. La exclusión de una carpeta no interrumpe una cola ya iniciada.

La reproducción de vídeo se pausa al salir de la app; activa Solo audio para seguir escuchando. El brillo vuelve a ser el del sistema al salir de la pantalla de reproducción.

## Compilar y publicar

Android Studio, JDK 17, Android SDK 35 y Gradle 8.9:

```sh
./gradlew assembleDebug lintDebug
```

La APK queda en `app/build/outputs/apk/debug/app-debug.apk`. El proyecto incluye una clave de desarrollo conocida para que las betas siguientes puedan instalarse sobre esta. No es una clave de producción. Antes de distribuir una edición de producción debe configurarse una firma privada propia.

Sube el contenido de este proyecto a la raíz de un repositorio nuevo, en la rama `main`. El flujo de GitHub Actions compila, ejecuta Android Lint y publica la APK en una prerelease `v1.0.1-beta` automáticamente. Necesita GitHub Actions habilitado y permisos de escritura para el flujo. Las futuras versiones deben actualizar versionCode, versionName y las referencias a versión del flujo.

## Estado de validación

La entrega inicial fue compilada contra SDK 35, convertida a DEX y empaquetada; se verificaron la firma APK v2/v3, alineación y manifiesto. No fue instalada en un dispositivo o emulador en este entorno. Las pruebas de reproducción, gestos, permisos, orientación, segundo plano y compatibilidad del amplificador deben realizarse en el teléfono antes de considerar estable esta beta. La compatibilidad de formatos y códecs depende del MediaPlayer del dispositivo.

La app usa únicamente APIs de Android y no solicita permiso de Internet. No incluye streaming, subtítulos, ecualizador ni historial en esta primera beta.

## Licencia

MIT. Copyright 2026 NexLibre.
