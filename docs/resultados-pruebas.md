# Resultados de las pruebas de aceptación

Generado por `python3 pruebas/escenarios.py --informe docs/resultados-pruebas.md` el 2026-11-17, con la API arrancada con `--coworkhub.reloj.fijo=08:00` y los datos de `import.sql`.


## 0. Sesión

| | Paso | HTTP | Código |
|---|---|---|---|
| ✅ | Login admin | 200 |  |
| ✅ | Login recepción | 200 |  |
| ✅ | Login ana (MEMBER) | 200 |  |
| ✅ | Login con clave incorrecta → 401 | 401 | NO_AUTENTICADO |
| ✅ | Perfil de ana (RF-14) | 200 |  |
| ✅ | Health público (RF-15) | 200 |  |

## 1. Escenarios

| | Paso | HTTP | Código |
|---|---|---|---|
| ✅ | E1 · Reservar sala libre con 2 servicios → 201 con costo | 201 |  |
| ✅ | E2 · Horario que se cruza con otra reserva → 409 RN-01 | 409 | RN-01 |
| ✅ | E3 · 12:00-13:00 junto a una de 10:00-12:00 → 201 | 201 |  |
| ✅ | E4a · Reserva de 45 minutos → 409 RN-02 | 409 | RN-02 |
| ✅ | E4b · Fuera del horario de la sede → 409 RN-03 | 409 | RN-03 |
| ✅ | E5 · Preparar: reserva que empieza en 30 h | 201 |  |
| ✅ | E5 · Cancelar con 30 h → 200, cargo 0 | 200 |  |
| ✅ | E6 · Preparar: reserva que empieza en 5 h | 201 |  |
| ✅ | E6 · Cancelar con 5 h → 200, cargo del 50 % | 200 |  |
| ✅ | E7 · Preparar: reserva que empieza en 1 h | 201 |  |
| ✅ | E7 · Cancelar con 1 h → 409 RN-08 | 409 | RN-08 |
| ✅ | E8 · Confirmar una reserva cancelada → 409 RN-09 | 409 | RN-09 |
| ✅ | E9 · Un miembro consulta la reserva de otro → 403 | 403 | ACCESO_DENEGADO |
| ✅ | E10a · Endpoint protegido sin token → 401 | 401 | NO_AUTENTICADO |
| ✅ | E10b · Endpoint protegido con token alterado → 401 | 401 | NO_AUTENTICADO |
| ✅ | E10c · Endpoint protegido con token vencido → 401 | 401 | NO_AUTENTICADO |
| ✅ | E11 · Eliminar una sala con reservas futuras → 409 RN-12 | 409 | RN-12 |
| ✅ | E12 · Crear una sede sin nombre → 400 | 400 | VALIDACION |
| ✅ | E13 · 6 solicitudes simultáneas, misma sala y horario → una 201 y el resto 409 | 201/409/409/409/409/409 | RN-01 |
| ✅ | E14 · Listar miembros con ?estado=SUSPENDED → solo suspendidos | 200 |  |

## 2. Reglas

| | Paso | HTTP | Código |
|---|---|---|---|
| ✅ | RN-02 · Más de 8 horas → 409 | 409 | RN-02 |
| ✅ | RN-02 · Cruza la medianoche → 409 | 409 | RN-02 |
| ✅ | RN-03 · En el pasado → 409 | 409 | RN-03 |
| ✅ | RN-03 · Con menos de 1 h de anticipación → 409 | 409 | RN-03 |
| ✅ | RN-04 · Sala inactiva → 409 | 409 | RN-04 |
| ✅ | RN-04 · Asistentes sobre la capacidad → 409 | 409 | RN-04 |
| ✅ | RN-05 · Miembro suspendido → 409 | 409 | RN-05 |
| ✅ | RN-06 · Luis (plan Flex, máx. 2) ya tiene 2 activas → 409 | 409 | RN-06 |
| ✅ | RN-07 · Preparar: miembro nuevo con plan Profesional (10 h, 10 %) | 201 |  |
| ✅ | RN-07 · 8 h cubiertas por el plan → sala 0 | 201 |  |
| ✅ | RN-07 · 4 h más: 2 cubiertas y 2 excedentes × 30.000 × 0,9 = 54.000 | 201 |  |
| ✅ | RN-07 · Consumo del mes: 12 h usadas, 2 excedentes | 200 |  |
| ✅ | RN-07 · Subir el precio del café a 7.000 | 200 |  |
| ✅ | RN-07 · La reserva de E1 conserva el precio de 5.000 | 200 |  |
| ✅ | RN-07 · Volver el café a 5.000 | 200 |  |
| ✅ | RN-08 · Preparar: miembro nuevo con plan Flex | 201 |  |
| ✅ | RN-08 · Preparar: reserva que empieza en 24 h exactas | 201 |  |
| ✅ | RN-08 · Cancelar con 24 h exactas → sin cargo | 200 |  |
| ✅ | RN-08 · Preparar: reserva que empieza en 2 h exactas | 201 |  |
| ✅ | RN-08 · Cancelar con 2 h exactas → se permite, con cargo del 50 % | 200 |  |
| ✅ | RN-09 · Completar una PENDING → 409 | 409 | RN-09 |
| ✅ | RN-09 · Confirmar una PENDING → 200 | 200 |  |
| ✅ | RN-09 · Completar una CONFIRMED → 200 | 200 |  |
| ✅ | RN-10 · Agregar servicio a una CONFIRMED → 409 | 409 | RN-10 |
| ✅ | RF-06 · Agregar servicio a una PENDING → costo recalculado | 200 |  |
| ✅ | RF-06 · Quitar el servicio → 200 sin cuerpo | 200 |  |
| ✅ | RF-06 · El detalle se borró y el costo bajó a 0 | 200 |  |
| ✅ | RN-11 · Email repetido con otras mayúsculas → 409 | 409 | RN-11 |
| ✅ | RN-11 · Documento repetido → 409 | 409 | RN-11 |
| ✅ | RN-11 · Nombre de sala repetido en la misma sede → 409 | 409 | RN-11 |
| ✅ | RN-12 · Eliminar un plan con miembros → 409 | 409 | RN-12 |
| ✅ | RN-12 · Eliminar un miembro con reservas → 409 | 409 | RN-12 |
| ✅ | RN-12 · Eliminar una sede con salas → 409 | 409 | RN-12 |

## 3. Permisos

| | Paso | HTTP | Código |
|---|---|---|---|
| ✅ | MEMBER crea una sede → 403 | 403 | ACCESO_DENEGADO |
| ✅ | RECEPTIONIST crea un plan → 403 | 403 | ACCESO_DENEGADO |
| ✅ | MEMBER lista reservas por estado → 403 | 403 | ACCESO_DENEGADO |
| ✅ | MEMBER lista reservas de otro miembro → 403 | 403 | ACCESO_DENEGADO |
| ✅ | MEMBER lista sus reservas → 200 | 200 |  |
| ✅ | MEMBER lista miembros → 403 | 403 | ACCESO_DENEGADO |
| ✅ | MEMBER ve la ficha de otro → 403 | 403 | ACCESO_DENEGADO |
| ✅ | MEMBER ve su consumo → 200 | 200 |  |
| ✅ | MEMBER reserva a nombre de otro → 403 | 403 | ACCESO_DENEGADO |
| ✅ | MEMBER reserva a su nombre → 201 | 201 |  |
| ✅ | MEMBER confirma su reserva → 403 (solo personal) | 403 | ACCESO_DENEGADO |
| ✅ | MEMBER cancela su reserva → 200 | 200 |  |

## 4. Errores

| | Paso | HTTP | Código |
|---|---|---|---|
| ✅ | Falla a mitad de camino (servicio 999) → 404 | 404 | NO_ENCONTRADO |
| ✅ | … y no quedó nada guardado (rollback) | 200 |  |
| ✅ | JSON mal formado → 400 | 400 | JSON_INVALIDO |
| ✅ | Ruta inexistente → 404 con el cuerpo estándar | 404 | RUTA_INEXISTENTE |
| ✅ | Método no permitido → 405 con el cuerpo estándar | 405 | METODO_NO_PERMITIDO |
| ✅ | Recurso inexistente → 404 | 404 | NO_ENCONTRADO |
| ✅ | Listar reservas sin filtro → 400 | 400 | SOLICITUD_INVALIDA |

**78 de 78 pasos correctos.**
