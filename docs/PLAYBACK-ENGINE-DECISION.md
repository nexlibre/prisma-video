# Motor de reproducción: decisión inicial

## Decisión

Media3 es el motor principal de esta rama. No se añade un segundo motor nativo
antes de medir fallos reales del corpus de vídeos. `PlaybackEngine` encapsula
la dependencia; el servicio conserva cola, foco, historial y temporizador.
Esto permite evaluar una alternativa posteriormente sin volver a acoplar
MainActivity a los detalles del motor.

## Evaluación de alternativas

| Alternativa | Qué aportaría | Coste y condición para integrarla |
|---|---|---|
| Media3 + decodificadores Android | Extractores y reproducción Android, fallback de inicialización | La decodificación depende del dispositivo; probar cada combinación de contenedor, códec y perfil |
| Extensión FFmpeg de Media3 | Ampliar decodificación de audio | No resuelve por sí sola todo el vídeo; compilar y mantener JNI por ABI, auditar opciones/licencias y medir APK |
| LibVLC como motor alternativo | Otro pipeline multimedia con bindings Android | Segundo ciclo de vida/superficies/pistas; fuentes y licencias de binarios; medir tamaño, energía y sincronización |

Los formatos contenedor y de muestras son capacidades distintas. Que un
extractor reconozca MKV no garantiza que el dispositivo decodifique su pista
HEVC. HDR requiere además capacidades compatibles de decodificador y pantalla.
La tabla oficial de Media3 es orientación, no un resultado de Prisma Video.

FFmpeg se distribuye bajo LGPL 2.1 o posterior; opciones/componentes GPL pueden
cambiar las condiciones del binario. La evaluación debe fijar configuración,
fuentes correspondientes, avisos y forma de enlace. No se integra un AAR nativo
sin conocer su composición. LibVLC declara LGPL 2.1; hay que revisar también
los bindings y módulos del artefacto concreto. No se ha determinado aún el
tamaño ni la cobertura de un binario alternativo: no se inventan cifras.

## Criterio de aceptación

Incorporar una alternativa solo si reproduce un caso que Media3 no resuelve,
supera el corpus y los ciclos de vida de Android, mantiene los datos,
permite distribuir todas sus dependencias correctamente y tiene coste
medido aceptable. No se anuncia software universal ni compatibilidad completa.

## Fuentes primarias consultadas

- https://developer.android.com/media/media3/exoplayer/supported-formats
- https://developer.android.com/reference/androidx/media3/exoplayer/DefaultRenderersFactory
- https://ffmpeg.org/legal.html
- https://images.videolan.org/vlc/libvlc.html
