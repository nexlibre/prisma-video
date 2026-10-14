# Informe de validación · Prisma Video 5.0.1 beta

## Verificación automatizada exigida por la publicación

El trabajo `publish` necesita que terminen correctamente `build` y las dos
entradas API 26/35 de `playback-smoke`. No se usa una compilación como sustituto
de pruebas de ejecución. Los informes HTML y capturas se conservan en Actions.

| Comprobación | Casos y alcance |
|---|---|
| Compilación/Lint | assembleDebug, lintDebug y validación del wrapper; SDK 35, JDK 17 |
| Java SRT/VTT | codificaciones, timestamps inválidos, solapamientos, límites, búsquedas y tamaño |
| Java listas/aleatorio | orden, adición, ciclos sin repeticiones, 2/3/100/10000 entradas |
| Motor MP4 | H.264/AAC, duración, audio, pista de texto mov_text, cues, búsqueda, velocidad, pausa, final y error de archivo ausente |
| Contenedores | MKV y MOV H.264/AAC; WebM VP9/Opus si hay decodificador VP9 (la prueba se omite explícitamente si falta) |
| Repositorios | JSON 2.0.1, listas, renombrado, otros datos preservados, rechazo de sobrescritura de JSON dañado, normalización de carpetas |
| Interfaz | acceso a cuatro temas, selección múltiple, controles de vídeo local y entrada PiP si el emulador ofrece la función |
| Capturas | cuatro temas, reproductor y PiP cuando está disponible; inspección visual adicional antes de publicar |
| Actualización real | instalar APK publicado 2.0.1, escribir preferencias reales, instalar 5.0.1 encima, iniciar app y comparar valores |
| Certificado/datos | apksigner en ambos APK; mismo SHA-256 del certificado; listas, favoritos, exclusiones, marcadores, referencias de subtítulos, historial y posiciones |
| Publicación | APK probado adjunto, SHA256SUMS, informe y notas; no sobrescribir releases anteriores |

Las muestras son sintéticas originales de dos segundos y 64×64. Se almacenan
en Base64 como recursos **androidTest** y no se incluyen en el APK principal.
El MP4 incluye H.264, AAC y mov_text en español. SHA-256:
`f169c90a573073dd07d192ccc62ca62aa83620893dbb7117164e0b0efa1d6c99`.
Los contenedores adicionales conservan o transcodifican estos contenidos de
prueba. No se usan películas ni archivos de usuarios.

## Resultados históricos de las fases

- Núcleo inicial: Actions 38502061873 aprobó build/Lint, tres pruebas Java y dos pruebas de motor por API.
- Biblioteca/repositorios: Actions 38503123274 aprobó build y ambos trabajos de emulador.
- El primer intento de actualización, Actions 38504249201, se detuvo por la resolución de apksigner; no se anuncia como aprobado. La ruta se corrigió en el verificador.
- La evidencia definitiva de la release será el run indicado al final de este informe por el trabajo de publicación. Consultar su commit y artefactos, no extrapolar resultados de una fase anterior.

## Límites de los ensayos

Las pruebas del motor usan superficie nula; las de interfaz abren una superficie
real y capturan el reproductor, pero no certifican fotograma por fotograma,
color HDR ni calidad audiovisual. No se validó físicamente HEVC/AV1/HDR/4K,
latencia de accesorios, fabricantes, batería o memoria en equipos reales.
El corpus pequeño no certifica cualquier archivo con la misma extensión.

Pendientes de matriz física: llamadas y grabaciones de otras apps, desconexión
de auriculares, multicanal, ecualizador/amplificación, todas las transiciones
PiP/rotación, permisos parciales, cancelación/autorización de borrado y
renombrado, SAF/USB, gestos/arrastre, bibliotecas grandes y métricas de inicio,
seek, fotogramas, RAM y energía. Esto está documentado como pendiente, no aprobado.

Una app externa que no solicita foco de audio puede grabar sin que Android
notifique al reproductor. No se ofrece detección universal de grabaciones.
La firma beta pública de desarrollo no sustituye una firma privada de producción.
