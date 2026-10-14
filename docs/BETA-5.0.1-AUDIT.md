# Inventario final de la consolidación 5.0.1

Base: a4b6a2ea7a410938039dcbc07def1beb9cf6f747, Beta 2.0.1.
La release anuncia exclusivamente la lista implementada del README. El prompt
maestro sirve también como hoja de ruta: no todas sus aspiraciones se presentan
como completadas dentro de esta beta. Los elementos excluidos se identifican aquí.

| Área | Implementado en 5.0.1 | Límites o trabajo posterior |
|---|---|---|
| Motor | Media3 encapsulado, búsqueda exacta, decodificadores Android con fallback, audio/pistas, errores legibles | HEVC/AV1/HDR/4K dependen del equipo; sin FFmpeg/LibVLC ni cobertura universal |
| Arquitectura | Servicio, motor y repositorios separados; componentes de miniaturas, previsualización, temas y movimiento | UI aún en Activity; Room/ViewModel/Media3 MediaSession requieren otra migración, no se anuncian |
| Interfaz | Controles vectoriales compactos, vistas adaptables, cuatro paletas, transiciones, PiP e inmersión | Sin certificación visual exhaustiva de todas las tabletas/capas |
| Subtítulos | SRT/VTT externo, selección incrustada, fuentes, colores, bordes, fondo, posición, asociación SAF y preferencias | Delay solo externo; ASS/SSA incrustado según Media3, sin certificación de todos sus efectos ni archivos ASS externos |
| Audio | Selección, velocidad, ecualizador por presets compatible, amplificación, solo audio, foco/auriculares | Sin normalización, limitador ni desfase manual AV; grabaciones ajenas sin solicitar foco no se pueden detectar universalmente |
| Biblioteca | Miniaturas reales/caché, lista/cuadrícula, búsqueda, subcarpetas, filtros, favoritos, historial, continuar, exclusión, selección, compartir, renombrar/borrar y observer | Renombrar Android 10+; filtros de extensión y metadatos disponibles; sin benchmark físico completo |
| Listas | Esquema preservado, operaciones previas, multiadición, arrastre, orden persistente, cola editable y aleatorio por ciclos | Reordenar no modifica una cola iniciada; referencias ocultas/inaccesibles conservadas |
| Gestos | Doble toque editable, búsqueda, brillo/volumen, zoom, bloqueo y desactivación individual | Sin editor arbitrario de asignaciones; ajuste de precisión requiere equipos reales |
| Avanzado | PiP, solo audio/segundo plano, velocidad, temporizador, A–B, marcadores, reanudación, encuadre, zoom, preview e información | Preview aproximado; interfaz limita duración a ~24,8 días |
| Temas | AMOLED, Light, Dynamic, Signature; acentos y movimiento iguales funcionalmente | Cristal excluido hasta medir legibilidad y coste |
| Diferenciación | Cine, previews, marcadores y continuidad; privacidad local y acceso SAF | Redes y bloqueo de colecciones excluidos; USB depende del proveedor Android/SAF |
| Rendimiento | ListView reutiliza filas, colas de trabajo/caché acotadas, mapas para orden de listas, tareas fuera de UI | Sin cifras de energía/RAM física ni certificación de 10 000 vídeos |
| Seguridad | Permisos mínimos, sin Internet; Scoped Storage, permisos parciales, SAF y autorización para editar/borrar; validación de subtítulos | Capa del fabricante y pérdida de permisos requieren ensayos físicos |
| Pruebas | Java, Lint, motor/formatos/subtítulos, repositorios, UI/PiP, actualización con firma y datos, API 26/35 | Matriz física pendiente; consultar resultados exactos, no deducir pruebas por compilación |
| Repositorio | MIT, licencias de terceros incluidas, README/CHANGELOG/informe; Actions fijadas por SHA y permisos reducidos | Firma privada de producción no se incorpora a esta beta compatible |
| Publicación | 5.0.1-beta, versionCode 5; APK + hash + informe; publicación tras trabajos de validación | No sobrescribe una release ya existente; requiere verificar resultado remoto |

## Decisiones

Se conserva el esquema SharedPreferences para evitar una migración destructiva.
Los repositorios encapsulan datos y no sobrescriben JSON de listas dañado.
Media3 no ofrece por sí solo una solución verificada de desfase AV o DSP con
normalización/limitación para todos los formatos: se excluyen estas funciones.
Se evalúan motores nativos en PLAYBACK-ENGINE-DECISION.md sin incorporarlos a
ciegas ni aumentar el APK con binarios no auditados.

La compatibilidad entre instalaciones se comprueba con el APK **publicado**
2.0.1, su certificado y valores de los tipos de datos existentes. Esto prueba
las claves representativas y no todas las posibles configuraciones del usuario.
