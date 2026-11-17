#!/usr/bin/env python3
"""
Pruebas de aceptación de CoWorkHub.

Una sola fuente de verdad para:
  1. Ejecutar los 14 escenarios del enunciado, más pruebas de cada regla de negocio, contra la
     API en marcha:            python3 pruebas/escenarios.py
  2. Generar la colección de Postman (también se importa en Insomnia):
                               python3 pruebas/escenarios.py --exportar docs/coworkhub.postman_collection.json
  3. Dejar el informe con los resultados:
                               python3 pruebas/escenarios.py --informe docs/resultados-pruebas.md

Requisitos: la aplicación recién arrancada (los datos de import.sql intactos) y con el reloj
fijado a las 8:00 de hoy, para que las anticipaciones de los escenarios sean exactas:

    mvn spring-boot:run -Dspring-boot.run.arguments=--coworkhub.reloj.fijo=08:00

Solo usa la biblioteca estándar de Python 3.
"""

import argparse
import base64
import datetime as dt
import hashlib
import hmac
import json
import sys
import threading
import urllib.error
import urllib.request

BASE = "http://localhost:8080"
# La misma clave por defecto de application.properties (solo para la prueba del token vencido).
SECRETO_JWT = "clave-educativa-de-coworkhub-solo-para-desarrollo-cambiala-en-produccion"

# ---------------------------------------------------------------------------- definición de los pasos
# Cada paso: nombre, método, ruta, token (variable o None), cuerpo, estado esperado, código de
# error esperado, comprobaciones {campo: valor} y variables a guardar {variable: campo}.
# Fechas: {{D0}} = hoy, {{D1}} = mañana… {{Dm1}} = ayer. El reloj de la API marca hoy a las 08:00.


def paso(carpeta, nombre, metodo, ruta, token="tokenAdmin", cuerpo=None, estado=200, codigo=None,
         comprobar=None, guardar=None, todos=None, largo=None):
    return dict(carpeta=carpeta, nombre=nombre, metodo=metodo, ruta=ruta, token=token, cuerpo=cuerpo,
                estado=estado, codigo=codigo, comprobar=comprobar or {}, guardar=guardar or {},
                todos=todos, largo=largo)


def reserva(miembro, sala, dia, inicio, fin, asistentes=2, servicios=None):
    cuerpo = {"miembroId": miembro, "salaId": sala, "inicio": "{{%s}}T%s:00" % (dia, inicio),
              "fin": "{{%s}}T%s:00" % (dia, fin), "asistentes": asistentes}
    if servicios:
        cuerpo["servicios"] = [{"servicioId": s, "cantidad": c} for s, c in servicios]
    return cuerpo


SEDE_SIN_NOMBRE = {"ciudad": "Girón", "direccion": "Calle 30 # 26-10", "horaApertura": "08:00", "horaCierre": "18:00"}

PASOS = [
    # ------------------------------------------------------------------ 0. Sesión
    paso("0. Sesión", "Login admin", "POST", "/auth/login", None, {"usuario": "admin", "contrasena": "admin123"},
         guardar={"tokenAdmin": "token"}, comprobar={"rol": "ADMIN"}),
    paso("0. Sesión", "Login recepción", "POST", "/auth/login", None, {"usuario": "recepcion", "contrasena": "recep123"},
         guardar={"tokenRecepcion": "token"}),
    paso("0. Sesión", "Login ana (MEMBER)", "POST", "/auth/login", None, {"usuario": "ana", "contrasena": "ana123"},
         guardar={"tokenAna": "token"}, comprobar={"miembroId": 1}),
    paso("0. Sesión", "Login con clave incorrecta → 401", "POST", "/auth/login", None,
         {"usuario": "admin", "contrasena": "equivocada"}, estado=401, codigo="NO_AUTENTICADO"),
    paso("0. Sesión", "Perfil de ana (RF-14)", "GET", "/auth/perfil", "tokenAna",
         comprobar={"usuario": "ana", "rol": "MEMBER", "miembroId": 1}),
    paso("0. Sesión", "Health público (RF-15)", "GET", "/actuator/health", None, comprobar={"status": "UP"}),

    # ------------------------------------------------------------------ 1. Los 14 escenarios del enunciado
    paso("1. Escenarios", "E1 · Reservar sala libre con 2 servicios → 201 con costo", "POST", "/api/reservas",
         cuerpo=reserva(5, 3, "D3", "10:00", "12:00", 3, [(1, 3), (2, 3)]), estado=201,
         comprobar={"costoSala": 0, "costoServicios": 51000, "costoTotal": 51000, "estado": "PENDING"},
         guardar={"reservaE1": "id"}),
    paso("1. Escenarios", "E2 · Horario que se cruza con otra reserva → 409 RN-01", "POST", "/api/reservas",
         cuerpo=reserva(3, 1, "D2", "11:00", "12:00"), estado=409, codigo="RN-01"),
    paso("1. Escenarios", "E3 · 12:00-13:00 junto a una de 10:00-12:00 → 201", "POST", "/api/reservas",
         cuerpo=reserva(3, 1, "D2", "12:00", "13:00"), estado=201, guardar={"reservaE3": "id"}),
    paso("1. Escenarios", "E4a · Reserva de 45 minutos → 409 RN-02", "POST", "/api/reservas",
         cuerpo=reserva(5, 3, "D4", "10:00", "10:45"), estado=409, codigo="RN-02"),
    paso("1. Escenarios", "E4b · Fuera del horario de la sede → 409 RN-03", "POST", "/api/reservas",
         cuerpo=reserva(5, 3, "D4", "19:00", "21:00"), estado=409, codigo="RN-03"),
    paso("1. Escenarios", "E5 · Preparar: reserva que empieza en 30 h", "POST", "/api/reservas",
         cuerpo=reserva(2, 4, "D1", "14:00", "15:00", 1), estado=201, comprobar={"costoSala": 8000},
         guardar={"reservaE5": "id"}),
    paso("1. Escenarios", "E5 · Cancelar con 30 h → 200, cargo 0", "POST", "/api/reservas/{{reservaE5}}/cancelar",
         comprobar={"estado": "CANCELLED", "cargoCancelacion": 0, "costoTotal": 0, "minutosDelPlanConsumidos": 0}),
    paso("1. Escenarios", "E6 · Preparar: reserva que empieza en 5 h", "POST", "/api/reservas",
         cuerpo=reserva(2, 4, "D0", "13:00", "14:00", 1), estado=201, comprobar={"costoSala": 8000},
         guardar={"reservaE6": "id"}),
    paso("1. Escenarios", "E6 · Cancelar con 5 h → 200, cargo del 50 %", "POST", "/api/reservas/{{reservaE6}}/cancelar",
         comprobar={"estado": "CANCELLED", "cargoCancelacion": 4000, "costoTotal": 4000}),
    paso("1. Escenarios", "E7 · Preparar: reserva que empieza en 1 h", "POST", "/api/reservas",
         cuerpo=reserva(2, 4, "D0", "09:00", "10:00", 1), estado=201, guardar={"reservaE7": "id"}),
    paso("1. Escenarios", "E7 · Cancelar con 1 h → 409 RN-08", "POST", "/api/reservas/{{reservaE7}}/cancelar",
         estado=409, codigo="RN-08"),
    paso("1. Escenarios", "E8 · Confirmar una reserva cancelada → 409 RN-09", "POST",
         "/api/reservas/{{reservaE5}}/confirmar", estado=409, codigo="RN-09"),
    paso("1. Escenarios", "E9 · Un miembro consulta la reserva de otro → 403", "GET", "/api/reservas/11", "tokenAna",
         estado=403, codigo="ACCESO_DENEGADO"),
    paso("1. Escenarios", "E10a · Endpoint protegido sin token → 401", "GET", "/api/reservas/8", None,
         estado=401, codigo="NO_AUTENTICADO"),
    paso("1. Escenarios", "E10b · Endpoint protegido con token alterado → 401", "GET", "/api/reservas/8", "tokenAlterado",
         estado=401, codigo="NO_AUTENTICADO"),
    paso("1. Escenarios", "E11 · Eliminar una sala con reservas futuras → 409 RN-12", "DELETE", "/api/salas/1",
         estado=409, codigo="RN-12"),
    paso("1. Escenarios", "E12 · Crear una sede sin nombre → 400", "POST", "/api/sedes", cuerpo=SEDE_SIN_NOMBRE,
         estado=400, codigo="VALIDACION"),
    # E13 (solicitudes simultáneas) no se puede hacer en Postman, que envía una solicitud a la vez:
    # lo ejecuta este script con hilos (ver probar_concurrencia).
    paso("1. Escenarios", "E14 · Listar miembros con ?estado=SUSPENDED → solo suspendidos", "GET",
         "/api/miembros?estado=SUSPENDED", todos=("estado", "SUSPENDED")),

    # ------------------------------------------------------------------ 2. Reglas de negocio una por una
    paso("2. Reglas", "RN-02 · Más de 8 horas → 409", "POST", "/api/reservas",
         cuerpo=reserva(3, 2, "D9", "07:00", "15:30"), estado=409, codigo="RN-02"),
    paso("2. Reglas", "RN-02 · Cruza la medianoche → 409", "POST", "/api/reservas",
         cuerpo={"miembroId": 3, "salaId": 2, "inicio": "{{D9}}T19:00:00", "fin": "{{D10}}T01:00:00", "asistentes": 2},
         estado=409, codigo="RN-02"),
    paso("2. Reglas", "RN-03 · En el pasado → 409", "POST", "/api/reservas",
         cuerpo=reserva(3, 2, "Dm1", "10:00", "11:00"), estado=409, codigo="RN-03"),
    paso("2. Reglas", "RN-03 · Con menos de 1 h de anticipación → 409", "POST", "/api/reservas",
         cuerpo=reserva(3, 3, "D0", "08:30", "09:30"), estado=409, codigo="RN-03"),
    paso("2. Reglas", "RN-04 · Sala inactiva → 409", "POST", "/api/reservas",
         cuerpo=reserva(3, 7, "D9", "10:00", "11:00"), estado=409, codigo="RN-04"),
    paso("2. Reglas", "RN-04 · Asistentes sobre la capacidad → 409", "POST", "/api/reservas",
         cuerpo=reserva(3, 4, "D9", "10:00", "11:00", 3), estado=409, codigo="RN-04"),
    paso("2. Reglas", "RN-05 · Miembro suspendido → 409", "POST", "/api/reservas",
         cuerpo=reserva(4, 3, "D9", "10:00", "11:00"), estado=409, codigo="RN-05"),
    paso("2. Reglas", "RN-06 · Luis (plan Flex, máx. 2) ya tiene 2 activas → 409", "POST", "/api/reservas",
         cuerpo=reserva(2, 3, "D9", "10:00", "11:00"), estado=409, codigo="RN-06"),

    paso("2. Reglas", "RN-07 · Preparar: miembro nuevo con plan Profesional (10 h, 10 %)", "POST", "/api/miembros",
         "tokenRecepcion", {"documento": "1098000020", "nombreCompleto": "Valentina Mantilla",
                            "email": "valentina.mantilla@ejemplo.com", "planId": 2, "usuario": "valentina",
                            "contrasena": "valentina123"}, estado=201, comprobar={"usuario": "valentina"},
         guardar={"miembroValentina": "id"}),
    paso("2. Reglas", "RN-07 · 8 h cubiertas por el plan → sala 0", "POST", "/api/reservas", "tokenRecepcion",
         {"miembroId": "{{miembroValentina}}", "salaId": 2, "inicio": "{{D40}}T08:00:00", "fin": "{{D40}}T16:00:00",
          "asistentes": 5}, estado=201, comprobar={"costoSala": 0}),
    paso("2. Reglas", "RN-07 · 4 h más: 2 cubiertas y 2 excedentes × 30.000 × 0,9 = 54.000", "POST", "/api/reservas",
         "tokenRecepcion", {"miembroId": "{{miembroValentina}}", "salaId": 1, "inicio": "{{D40}}T08:00:00",
                            "fin": "{{D40}}T12:00:00", "asistentes": 5}, estado=201, comprobar={"costoSala": 54000}),
    paso("2. Reglas", "RN-07 · Consumo del mes: 12 h usadas, 2 excedentes", "GET",
         "/api/miembros/{{miembroValentina}}/consumo?mes={{MES40}}",
         comprobar={"horasIncluidas": 10, "horasUsadas": 12, "horasRestantes": 0, "horasExcedentes": 2}),
    paso("2. Reglas", "RN-07 · Subir el precio del café a 7.000", "PUT", "/api/servicios-adicionales/1",
         cuerpo={"nombre": "Café y aromáticas (por persona)", "precioUnitario": 7000}, comprobar={"precioUnitario": 7000}),
    paso("2. Reglas", "RN-07 · La reserva de E1 conserva el precio de 5.000", "GET", "/api/reservas/{{reservaE1}}",
         comprobar={"costoServicios": 51000, "servicios.0.precioUnitarioAplicado": 5000}),
    paso("2. Reglas", "RN-07 · Volver el café a 5.000", "PUT", "/api/servicios-adicionales/1",
         cuerpo={"nombre": "Café y aromáticas (por persona)", "precioUnitario": 5000}),

    paso("2. Reglas", "RN-08 · Preparar: miembro nuevo con plan Flex", "POST", "/api/miembros", "tokenRecepcion",
         {"documento": "1098000021", "nombreCompleto": "Mateo Serrano", "email": "mateo.serrano@ejemplo.com",
          "planId": 1, "usuario": "mateo", "contrasena": "mateo123"}, estado=201, guardar={"miembroMateo": "id"}),
    paso("2. Reglas", "RN-08 · Preparar: reserva que empieza en 24 h exactas", "POST", "/api/reservas", "tokenRecepcion",
         {"miembroId": "{{miembroMateo}}", "salaId": 4, "inicio": "{{D1}}T08:00:00", "fin": "{{D1}}T09:00:00",
          "asistentes": 1}, estado=201, guardar={"reserva24h": "id"}),
    paso("2. Reglas", "RN-08 · Cancelar con 24 h exactas → sin cargo", "POST", "/api/reservas/{{reserva24h}}/cancelar",
         "tokenRecepcion", comprobar={"cargoCancelacion": 0}),
    paso("2. Reglas", "RN-08 · Preparar: reserva que empieza en 2 h exactas", "POST", "/api/reservas", "tokenRecepcion",
         {"miembroId": "{{miembroMateo}}", "salaId": 4, "inicio": "{{D0}}T10:00:00", "fin": "{{D0}}T11:00:00",
          "asistentes": 1}, estado=201, guardar={"reserva2h": "id"}),
    paso("2. Reglas", "RN-08 · Cancelar con 2 h exactas → se permite, con cargo del 50 %", "POST",
         "/api/reservas/{{reserva2h}}/cancelar", "tokenRecepcion", comprobar={"cargoCancelacion": 4000}),

    paso("2. Reglas", "RN-09 · Completar una PENDING → 409", "POST", "/api/reservas/{{reservaE3}}/completar",
         estado=409, codigo="RN-09"),
    paso("2. Reglas", "RN-09 · Confirmar una PENDING → 200", "POST", "/api/reservas/{{reservaE1}}/confirmar",
         "tokenRecepcion", comprobar={"estado": "CONFIRMED"}),
    paso("2. Reglas", "RN-09 · Completar una CONFIRMED → 200", "POST", "/api/reservas/{{reservaE1}}/completar",
         "tokenRecepcion", comprobar={"estado": "COMPLETED"}),

    paso("2. Reglas", "RN-10 · Agregar servicio a una CONFIRMED → 409", "POST", "/api/reservas/8/servicios",
         cuerpo={"servicioId": 3, "cantidad": 1}, estado=409, codigo="RN-10"),
    paso("2. Reglas", "RF-06 · Agregar servicio a una PENDING → costo recalculado", "POST",
         "/api/reservas/{{reservaE3}}/servicios", cuerpo={"servicioId": 3, "cantidad": 2},
         comprobar={"costoServicios": 10000}),
    paso("2. Reglas", "RF-06 · Quitar el servicio → 200 sin cuerpo", "DELETE", "/api/reservas/{{reservaE3}}/servicios/3"),
    paso("2. Reglas", "RF-06 · El detalle se borró y el costo bajó a 0", "GET", "/api/reservas/{{reservaE3}}",
         comprobar={"costoServicios": 0, "servicios": []}),

    paso("2. Reglas", "RN-11 · Email repetido con otras mayúsculas → 409", "POST", "/api/miembros",
         cuerpo={"documento": "1098000022", "nombreCompleto": "Otra Ana", "email": "ANA.GOMEZ@ejemplo.com",
                 "planId": 1, "usuario": "otraana", "contrasena": "otra123"}, estado=409, codigo="RN-11"),
    paso("2. Reglas", "RN-11 · Documento repetido → 409", "POST", "/api/miembros",
         cuerpo={"documento": "1098000001", "nombreCompleto": "Otra Ana", "email": "otra.ana@ejemplo.com",
                 "planId": 1, "usuario": "otraana", "contrasena": "otra123"}, estado=409, codigo="RN-11"),
    paso("2. Reglas", "RN-11 · Nombre de sala repetido en la misma sede → 409", "POST", "/api/salas",
         cuerpo={"nombre": "sala andes", "tipo": "MEETING_ROOM", "capacidad": 4, "tarifaHora": 20000, "sedeId": 1},
         estado=409, codigo="RN-11"),
    paso("2. Reglas", "RN-12 · Eliminar un plan con miembros → 409", "DELETE", "/api/planes/1", estado=409, codigo="RN-12"),
    paso("2. Reglas", "RN-12 · Eliminar un miembro con reservas → 409", "DELETE", "/api/miembros/1",
         estado=409, codigo="RN-12"),
    paso("2. Reglas", "RN-12 · Eliminar una sede con salas → 409", "DELETE", "/api/sedes/1", estado=409, codigo="RN-12"),

    # ------------------------------------------------------------------ 3. Permisos (RN-13)
    paso("3. Permisos", "MEMBER crea una sede → 403", "POST", "/api/sedes", "tokenAna",
         {"nombre": "X", "ciudad": "Y", "direccion": "Z", "horaApertura": "08:00", "horaCierre": "18:00"},
         estado=403, codigo="ACCESO_DENEGADO"),
    paso("3. Permisos", "RECEPTIONIST crea un plan → 403", "POST", "/api/planes", "tokenRecepcion",
         {"nombre": "Plus", "horasIncluidasMes": 20, "descuentoExcedente": 15, "maxReservasActivas": 4},
         estado=403, codigo="ACCESO_DENEGADO"),
    paso("3. Permisos", "MEMBER lista reservas por estado → 403", "GET", "/api/reservas?estado=PENDING", "tokenAna",
         estado=403),
    paso("3. Permisos", "MEMBER lista reservas de otro miembro → 403", "GET", "/api/reservas?miembroId=2", "tokenAna",
         estado=403),
    paso("3. Permisos", "MEMBER lista sus reservas → 200", "GET", "/api/reservas?miembroId=1", "tokenAna",
         todos=("miembroId", 1)),
    paso("3. Permisos", "MEMBER lista miembros → 403", "GET", "/api/miembros", "tokenAna", estado=403),
    paso("3. Permisos", "MEMBER ve la ficha de otro → 403", "GET", "/api/miembros/2", "tokenAna", estado=403),
    paso("3. Permisos", "MEMBER ve su consumo → 200", "GET", "/api/miembros/1/consumo", "tokenAna",
         comprobar={"miembroId": 1}),
    paso("3. Permisos", "MEMBER reserva a nombre de otro → 403", "POST", "/api/reservas", "tokenAna",
         reserva(2, 5, "D6", "09:00", "10:00"), estado=403),
    paso("3. Permisos", "MEMBER reserva a su nombre → 201", "POST", "/api/reservas", "tokenAna",
         reserva(1, 5, "D6", "09:00", "10:00"), estado=201, guardar={"reservaAna": "id"}),
    paso("3. Permisos", "MEMBER confirma su reserva → 403 (solo personal)", "POST",
         "/api/reservas/{{reservaAna}}/confirmar", "tokenAna", estado=403),
    paso("3. Permisos", "MEMBER cancela su reserva → 200", "POST", "/api/reservas/{{reservaAna}}/cancelar", "tokenAna",
         comprobar={"estado": "CANCELLED"}),

    # ------------------------------------------------------------------ 4. Errores y transacciones
    paso("4. Errores", "Falla a mitad de camino (servicio 999) → 404", "POST", "/api/reservas",
         cuerpo=reserva(5, 5, "D8", "10:00", "11:00", 2, [(1, 1), (999, 1)]), estado=404, codigo="NO_ENCONTRADO"),
    paso("4. Errores", "… y no quedó nada guardado (rollback)", "GET", "/api/reservas?salaId=5&fecha={{D8}}", largo=0),
    paso("4. Errores", "JSON mal formado → 400", "POST", "/api/sedes", cuerpo="{\"nombre\": ", estado=400,
         codigo="JSON_INVALIDO"),
    paso("4. Errores", "Ruta inexistente → 404 con el cuerpo estándar", "GET", "/api/no-existe", estado=404,
         codigo="RUTA_INEXISTENTE"),
    paso("4. Errores", "Método no permitido → 405 con el cuerpo estándar", "PATCH", "/api/sedes", estado=405,
         codigo="METODO_NO_PERMITIDO"),
    paso("4. Errores", "Recurso inexistente → 404", "GET", "/api/salas/999", estado=404, codigo="NO_ENCONTRADO"),
    paso("4. Errores", "Listar reservas sin filtro → 400", "GET", "/api/reservas", estado=400, codigo="SOLICITUD_INVALIDA"),
]


# ---------------------------------------------------------------------------- utilidades


def fechas():
    """Variables de fecha relativas a hoy: D0..D41, Dm1 y MES40 (yyyy-MM del día D40)."""
    hoy = dt.date.today()
    v = {"D%d" % i: (hoy + dt.timedelta(days=i)).isoformat() for i in range(0, 42)}
    v["Dm1"] = (hoy - dt.timedelta(days=1)).isoformat()
    v["MES40"] = (hoy + dt.timedelta(days=40)).strftime("%Y-%m")
    return v


def sustituir(valor, variables):
    if isinstance(valor, str):
        for k, v in variables.items():
            valor = valor.replace("{{%s}}" % k, str(v))
        return valor
    if isinstance(valor, dict):
        return {k: sustituir(v, variables) for k, v in valor.items()}
    if isinstance(valor, list):
        return [sustituir(v, variables) for v in valor]
    return valor


def numero_si_corresponde(valor):
    """Un id guardado en una variable ('{{miembroValentina}}') queda como texto al sustituirlo:
    en los campos terminados en Id se envía como número."""
    if isinstance(valor, dict):
        return {k: (int(v) if k.endswith("Id") and isinstance(v, str) and v.isdigit() else numero_si_corresponde(v))
                for k, v in valor.items()}
    if isinstance(valor, list):
        return [numero_si_corresponde(v) for v in valor]
    return valor


def llamar(metodo, ruta, token=None, cuerpo=None):
    datos = None
    encabezados = {}
    if cuerpo is not None:
        datos = (cuerpo if isinstance(cuerpo, str) else json.dumps(cuerpo)).encode("utf-8")
        encabezados["Content-Type"] = "application/json"
    if token:
        encabezados["Authorization"] = "Bearer " + token
    solicitud = urllib.request.Request(BASE + ruta, data=datos, method=metodo, headers=encabezados)
    try:
        with urllib.request.urlopen(solicitud, timeout=30) as r:
            texto = r.read().decode("utf-8")
            return r.status, (json.loads(texto) if texto else None)
    except urllib.error.HTTPError as e:
        texto = e.read().decode("utf-8")
        try:
            return e.code, json.loads(texto)
        except ValueError:
            return e.code, texto


def leer(objeto, camino):
    for parte in camino.split("."):
        objeto = objeto[int(parte)] if isinstance(objeto, list) else objeto[parte]
    return objeto


def iguales(a, b):
    if isinstance(b, (int, float)) and isinstance(a, (int, float)):
        return abs(a - b) < 0.001
    return a == b


def token_vencido():
    """JWT firmado con la clave correcta pero vencido hace una hora: debe dar 401."""
    def b64(x):
        return base64.urlsafe_b64encode(x).rstrip(b"=").decode()
    ahora = int(dt.datetime.now(dt.timezone.utc).timestamp())
    cabecera = b64(json.dumps({"alg": "HS512"}).encode())
    datos = b64(json.dumps({"sub": "admin", "rol": "ADMIN", "iat": ahora - 7200, "exp": ahora - 3600}).encode())
    firma = hmac.new(SECRETO_JWT.encode(), ("%s.%s" % (cabecera, datos)).encode(), hashlib.sha512).digest()
    return "%s.%s.%s" % (cabecera, datos, b64(firma))


# ---------------------------------------------------------------------------- ejecución


def ejecutar():
    variables = fechas()
    resultados = []

    for p in PASOS:
        token = None
        if p["token"] == "tokenAlterado":
            token = variables["tokenAdmin"] + "abc"
        elif p["token"]:
            token = variables[p["token"]]
        ruta = sustituir(p["ruta"], variables)
        cuerpo = p["cuerpo"]
        if isinstance(cuerpo, (dict, list)):
            cuerpo = numero_si_corresponde(sustituir(cuerpo, variables))
        estado, respuesta = llamar(p["metodo"], ruta, token, cuerpo)

        fallas = []
        if estado != p["estado"]:
            fallas.append("estado %s (se esperaba %s)" % (estado, p["estado"]))
        if p["codigo"] and not (isinstance(respuesta, dict) and respuesta.get("codigo") == p["codigo"]):
            fallas.append("codigo %s (se esperaba %s)" % (respuesta.get("codigo") if isinstance(respuesta, dict) else "-", p["codigo"]))
        if not fallas:
            for campo, esperado in p["comprobar"].items():
                try:
                    obtenido = leer(respuesta, campo)
                except (KeyError, IndexError, TypeError):
                    obtenido = "<no está>"
                if not iguales(obtenido, esperado):
                    fallas.append("%s = %s (se esperaba %s)" % (campo, obtenido, esperado))
            if p["todos"]:
                campo, esperado = p["todos"]
                if not isinstance(respuesta, list) or not respuesta:
                    fallas.append("se esperaba una lista no vacía")
                elif any(fila.get(campo) != esperado for fila in respuesta):
                    fallas.append("no todos tienen %s = %s" % (campo, esperado))
            if p["largo"] is not None and (not isinstance(respuesta, list) or len(respuesta) != p["largo"]):
                fallas.append("largo %s (se esperaba %s)" % (len(respuesta) if isinstance(respuesta, list) else "-", p["largo"]))
            for variable, campo in p["guardar"].items():
                variables[variable] = leer(respuesta, campo)

        detalle = respuesta.get("codigo", "") if isinstance(respuesta, dict) and "codigo" in respuesta else ""
        resultados.append((p["carpeta"], p["nombre"], estado, detalle, fallas))

    # E10c: token vencido (necesita firmar un JWT; Postman no lo hace sin scripts extra)
    estado, respuesta = llamar("GET", "/api/reservas/8", token_vencido())
    fallas = [] if estado == 401 else ["estado %s (se esperaba 401)" % estado]
    resultados.insert(_posicion(resultados, "E10b") + 1,
                      ("1. Escenarios", "E10c · Endpoint protegido con token vencido → 401", estado,
                       respuesta.get("codigo", "") if isinstance(respuesta, dict) else "", fallas))

    # E13: seis solicitudes simultáneas a la misma sala y horario
    resultados.insert(_posicion(resultados, "E12") + 1, probar_concurrencia(variables))
    return resultados


def _posicion(resultados, prefijo):
    for i, fila in enumerate(resultados):
        if fila[1].startswith(prefijo):
            return i
    return len(resultados) - 1


def probar_concurrencia(variables, cantidad=6):
    cuerpo = numero_si_corresponde(sustituir(reserva(3, 6, "D10", "10:00", "11:00"), variables))
    estados = []
    barrera = threading.Barrier(cantidad)

    def enviar():
        barrera.wait()  # todas salen a la vez
        estado, _ = llamar("POST", "/api/reservas", variables["tokenAdmin"], cuerpo)
        estados.append(estado)

    hilos = [threading.Thread(target=enviar) for _ in range(cantidad)]
    for h in hilos:
        h.start()
    for h in hilos:
        h.join()
    estados.sort()
    _, guardadas = llamar("GET", "/api/reservas?salaId=6&fecha=%s" % variables["D10"], variables["tokenAdmin"])
    fallas = []
    if estados.count(201) != 1 or estados.count(409) != cantidad - 1:
        fallas.append("estados %s (se esperaba un 201 y %d 409)" % (estados, cantidad - 1))
    if not isinstance(guardadas, list) or len(guardadas) != 1:
        fallas.append("quedaron %s reservas guardadas (se esperaba 1)" % (len(guardadas) if isinstance(guardadas, list) else "?"))
    return ("1. Escenarios", "E13 · %d solicitudes simultáneas, misma sala y horario → una 201 y el resto 409" % cantidad,
            "/".join(str(e) for e in estados), "RN-01", fallas)


def imprimir(resultados):
    carpeta = None
    for c, nombre, estado, codigo, fallas in resultados:
        if c != carpeta:
            print("\n" + c)
            carpeta = c
        marca = "✅" if not fallas else "❌"
        print("  %s %-86s %s %s" % (marca, nombre, estado, codigo))
        for f in fallas:
            print("       → " + f)
    total = len(resultados)
    malas = sum(1 for r in resultados if r[4])
    print("\n%d de %d pasos correctos" % (total - malas, total))
    return malas


def informe(resultados, archivo):
    lineas = ["# Resultados de las pruebas de aceptación", "",
              "Generado por `python3 pruebas/escenarios.py --informe %s` el %s, con la API arrancada con "
              "`--coworkhub.reloj.fijo=08:00` y los datos de `import.sql`." % (archivo, dt.date.today().isoformat()), ""]
    carpeta = None
    for c, nombre, estado, codigo, fallas in resultados:
        if c != carpeta:
            lineas += ["", "## " + c, "", "| | Paso | HTTP | Código |", "|---|---|---|---|"]
            carpeta = c
        lineas.append("| %s | %s | %s | %s |" % ("✅" if not fallas else "❌", nombre.replace("|", "\\|"), estado, codigo))
    total = len(resultados)
    malas = sum(1 for r in resultados if r[4])
    lineas += ["", "**%d de %d pasos correctos.**" % (total - malas, total), ""]
    with open(archivo, "w", encoding="utf-8") as f:
        f.write("\n".join(lineas))


# ---------------------------------------------------------------------------- colección de Postman


def js_acceso(camino):
    partes = []
    for parte in camino.split("."):
        partes.append("[%s]" % parte if parte.isdigit() else "[%s]" % json.dumps(parte))
    return "json" + "".join(partes)


def exportar(archivo):
    script_fechas = [
        "// Fechas relativas a hoy: D0 = hoy, D1 = mañana… Dm1 = ayer. La API debe arrancar con",
        "// --coworkhub.reloj.fijo=08:00 (hoy a las 8:00) para que las anticipaciones sean exactas.",
        "const dos = n => String(n).padStart(2, '0');",
        "const hoy = new Date();",
        "const iso = d => `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}`;",
        "for (let i = 0; i <= 41; i++) {",
        "  pm.collectionVariables.set('D' + i, iso(new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + i)));",
        "}",
        "pm.collectionVariables.set('Dm1', iso(new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - 1)));",
        "const d40 = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + 40);",
        "pm.collectionVariables.set('MES40', `${d40.getFullYear()}-${dos(d40.getMonth() + 1)}`);",
        "if (pm.collectionVariables.get('tokenAdmin')) {",
        "  pm.collectionVariables.set('tokenAlterado', pm.collectionVariables.get('tokenAdmin') + 'abc');",
        "}",
    ]
    carpetas = {}
    for p in PASOS:
        pruebas = ["pm.test('HTTP %d', () => pm.response.to.have.status(%d));" % (p["estado"], p["estado"])]
        necesita_json = p["codigo"] or p["comprobar"] or p["guardar"] or p["todos"] or p["largo"] is not None
        if necesita_json:
            pruebas.append("const json = pm.response.json();")
        if p["codigo"]:
            pruebas.append("pm.test('codigo %s', () => pm.expect(json.codigo).to.eql(%s));" % (p["codigo"], json.dumps(p["codigo"])))
        for campo, esperado in p["comprobar"].items():
            pruebas.append("pm.test('%s = %s', () => pm.expect(%s).to.eql(%s));"
                           % (campo, json.dumps(esperado), js_acceso(campo), json.dumps(esperado)))
        if p["todos"]:
            campo, esperado = p["todos"]
            pruebas.append("pm.test('lista no vacía', () => pm.expect(json).to.be.an('array').that.is.not.empty);")
            pruebas.append("pm.test('todos con %s = %s', () => json.forEach(f => pm.expect(f[%s]).to.eql(%s)));"
                           % (campo, esperado, json.dumps(campo), json.dumps(esperado)))
        if p["largo"] is not None:
            pruebas.append("pm.test('%d elementos', () => pm.expect(json).to.have.lengthOf(%d));" % (p["largo"], p["largo"]))
        for variable, campo in p["guardar"].items():
            pruebas.append("pm.collectionVariables.set(%s, %s);" % (json.dumps(variable), js_acceso(campo)))

        encabezados = []
        if p["cuerpo"] is not None:
            encabezados.append({"key": "Content-Type", "value": "application/json"})
        if p["token"]:
            encabezados.append({"key": "Authorization", "value": "Bearer {{%s}}" % p["token"]})
        ruta = p["ruta"]
        crudo = "{{baseUrl}}" + ruta
        camino, _, consulta = ruta.partition("?")
        url = {"raw": crudo, "host": ["{{baseUrl}}"], "path": [x for x in camino.split("/") if x]}
        if consulta:
            url["query"] = [{"key": k, "value": v} for k, _, v in (q.partition("=") for q in consulta.split("&"))]
        solicitud = {"method": p["metodo"], "header": encabezados, "url": url}
        if p["cuerpo"] is not None:
            texto = p["cuerpo"] if isinstance(p["cuerpo"], str) else json.dumps(p["cuerpo"], ensure_ascii=False, indent=2)
            # los ids guardados en variables van sin comillas para que lleguen como números
            for variable in ("miembroValentina", "miembroMateo"):
                texto = texto.replace('"{{%s}}"' % variable, "{{%s}}" % variable)
            solicitud["body"] = {"mode": "raw", "raw": texto, "options": {"raw": {"language": "json"}}}
        carpetas.setdefault(p["carpeta"], []).append({
            "name": p["nombre"],
            "event": [{"listen": "test", "script": {"type": "text/javascript", "exec": pruebas}}],
            "request": solicitud,
        })

    coleccion = {
        "info": {
            "name": "CoWorkHub · escenarios de aceptación",
            "description": "Los 14 escenarios del enunciado (E13, las solicitudes simultáneas, se ejecuta con "
                           "pruebas/escenarios.py porque Postman envía una solicitud a la vez), más una prueba por "
                           "regla de negocio, permisos y errores. Arranca la API recién iniciada con "
                           "--coworkhub.reloj.fijo=08:00 y ejecuta la colección completa en orden (Run collection).",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "event": [{"listen": "prerequest", "script": {"type": "text/javascript", "exec": script_fechas}}],
        "variable": [{"key": "baseUrl", "value": BASE}],
        "item": [{"name": nombre, "item": items} for nombre, items in carpetas.items()],
    }
    with open(archivo, "w", encoding="utf-8") as f:
        json.dump(coleccion, f, ensure_ascii=False, indent=2)
    print("Colección escrita en %s (%d solicitudes)" % (archivo, len(PASOS)))


if __name__ == "__main__":
    analizador = argparse.ArgumentParser(description="Pruebas de aceptación de CoWorkHub")
    analizador.add_argument("--exportar", metavar="ARCHIVO", help="solo escribe la colección de Postman")
    analizador.add_argument("--informe", metavar="ARCHIVO", help="además, escribe los resultados en Markdown")
    argumentos = analizador.parse_args()
    if argumentos.exportar:
        exportar(argumentos.exportar)
        sys.exit(0)
    filas = ejecutar()
    errores = imprimir(filas)
    if argumentos.informe:
        informe(filas, argumentos.informe)
    sys.exit(1 if errores else 0)
