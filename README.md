# CoWorkHub

API REST con Spring Boot para una red de coworking: sedes, salas, planes de membresía,
miembros, disponibilidad y reservas. Evita las reservas duplicadas (incluso si llegan dos a la
vez), controla las horas que consume cada miembro, cobra las cancelaciones con una sola regla y
limita a cada usuario a lo que le corresponde.

No tiene interfaz gráfica: se usa con Swagger UI, Postman/Insomnia o `curl`.

| | |
|---|---|
| Stack | Java 21 · Spring Boot 4.1.2 · Spring Data JPA · MySQL 8 · Spring Security + JWT · springdoc-openapi · Maven |
| Swagger UI | <http://localhost:8080/doc/swagger-ui.html> |
| Documento OpenAPI | <http://localhost:8080/v3/api-docs> (copia en [`docs/openapi.json`](docs/openapi.json)) |
| Salud | <http://localhost:8080/actuator/health> |
| Análisis | [`docs/analisis.md`](docs/analisis.md): casos de uso, glosario, diagrama ER, trazabilidad y decisiones |

---

## 1. Requisitos

- **Java 21** o superior y **Maven 3.9** (o el `./mvnw` incluido).
- **MySQL 8**, instalado o con **Docker**.

## 2. Crear la base de datos

La aplicación se conecta a la base `coworkhub` con el usuario `coworkhub` / `coworkhub`, por
defecto en el **puerto 3307** (el 3306 suele estar ocupado por un MySQL instalado).

### Opción A — Con Docker (recomendada)

```bash
docker run -d --name coworkhub-mysql \
  -e MYSQL_DATABASE=coworkhub \
  -e MYSQL_USER=coworkhub \
  -e MYSQL_PASSWORD=coworkhub \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3307:3306 \
  -v coworkhub-datos:/var/lib/mysql \
  mysql:8.4
```

En Windows escríbelo en una sola línea, sin las `\`. La primera vez tarda unos segundos: espera
a que `docker logs coworkhub-mysql` muestre `ready for connections`.

| Para… | Comando |
|---|---|
| Ver si está corriendo | `docker ps` |
| Detenerlo / iniciarlo | `docker stop coworkhub-mysql` · `docker start coworkhub-mysql` |
| Abrir la consola de MySQL | `docker exec -it coworkhub-mysql mysql -u coworkhub -pcoworkhub coworkhub` |
| Borrarlo con sus datos | `docker rm -f coworkhub-mysql` y `docker volume rm coworkhub-datos` |

### Opción B — Con MySQL instalado

Como `root`, crea la base y el usuario:

```sql
CREATE DATABASE coworkhub CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'coworkhub'@'localhost' IDENTIFIED BY 'coworkhub';
GRANT ALL PRIVILEGES ON coworkhub.* TO 'coworkhub'@'localhost';
FLUSH PRIVILEGES;
```

Si tu MySQL está en el puerto 3306, arranca la aplicación con la variable `COWORKHUB_DB_PORT=3306`
(ver la sección 3).

## 3. Ejecutar

```bash
mvn spring-boot:run
```

Al arrancar, Hibernate crea las tablas y carga los datos de prueba de
[`src/main/resources/import.sql`](src/main/resources/import.sql) (`ddl-auto=create`: **cada
arranque empieza de cero** con los mismos datos).

La conexión se puede cambiar sin tocar el código, con variables de entorno:

| Variable | Por defecto | Para qué |
|---|---|---|
| `COWORKHUB_DB_PORT` | `3307` | Puerto de MySQL |
| `COWORKHUB_DB_USER` / `COWORKHUB_DB_PASSWORD` | `coworkhub` / `coworkhub` | Credenciales de la base |
| `COWORKHUB_JWT_SECRETO` | clave educativa del archivo | Clave de firma de los JWT |

```bash
COWORKHUB_DB_PORT=3306 mvn spring-boot:run
```

### Reloj fijo (para repetir las pruebas)

Las reglas de anticipación (RN-03 y RN-08) dependen de la hora actual. Para que las pruebas den
siempre lo mismo, el "ahora" de la aplicación se puede congelar:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--coworkhub.reloj.fijo=08:00
```

`08:00` significa **hoy a las 8:00** (las fechas de `import.sql` también son relativas a hoy).
También acepta una fecha completa, por ejemplo `2030-06-03T08:00:00`. Sin el parámetro, usa la
hora real.

## 4. Usuarios de prueba

| Usuario | Contraseña | Rol | Miembro |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | — |
| `recepcion` | `recep123` | `RECEPTIONIST` | — |
| `ana` | `ana123` | `MEMBER` | 1 · Ana Gómez (plan Profesional) |
| `luis` | `luis123` | `MEMBER` | 2 · Luis Pérez (plan Flex) |

Además hay tres miembros sin cuenta: Marta Rueda (3, Empresarial), Carlos Díaz (4, **suspendido**)
y Sofía Ardila (5, Profesional). Los datos son ficticios.

Datos cargados: 2 sedes, 7 salas (Sala Morrorico está inactiva), 5 equipamientos, 4 servicios
adicionales, 3 planes, 5 miembros, 4 cuentas y 15 reservas en los cuatro estados, algunas con
servicios.

## 5. Usar la API

### Con Swagger UI

1. Abre <http://localhost:8080/doc/swagger-ui.html>.
2. En **Autenticación**, ejecuta `POST /auth/login` con `{"usuario": "admin", "contrasena": "admin123"}`.
3. Copia el `token` de la respuesta, pulsa **Authorize** (arriba a la derecha), pégalo y acepta.
4. Ya puedes probar cualquier endpoint. Con el selector **Select a definition** ves la API por
   áreas: autenticación, catálogos, miembros, y disponibilidad y reservas.

### Con curl

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"usuario":"admin","contrasena":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
```

```bash
curl -s localhost:8080/api/salas?sedeId=1 -H "Authorization: Bearer $TOKEN"
```

### Endpoints

| Área | Rutas |
|---|---|
| Autenticación | `POST /auth/login` (público) · `GET /auth/perfil` |
| Catálogos | `/api/sedes`, `/api/salas` (`?sedeId=`), `/api/equipamientos`, `/api/servicios-adicionales`, `/api/planes`: `GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` |
| Miembros | `GET /api/miembros?nombre=&estado=` · `GET/PUT/DELETE /api/miembros/{id}` · `POST /api/miembros` · `PATCH /api/miembros/{id}/suspender` · `PATCH /api/miembros/{id}/activar` · `GET /api/miembros/{id}/consumo?mes=yyyy-MM` |
| Disponibilidad | `GET /api/disponibilidad?sedeId=&fecha=&horaInicio=&horaFin=` (+ `capacidadMinima`, `equipamientoIds`) |
| Reservas | `POST /api/reservas` · `GET /api/reservas/{id}` · `GET /api/reservas?miembroId=` · `?salaId=&fecha=` · `?estado=` · `POST/DELETE /api/reservas/{id}/servicios[/{servicioId}]` · `POST /api/reservas/{id}/confirmar` · `/cancelar` · `/completar` |
| Plataforma | `GET /actuator/health` (público) · Swagger (público) |

Todo error responde con el mismo cuerpo, donde `codigo` es la regla violada o el tipo de error:

```json
{"error": "La sala ya está reservada en ese horario", "codigo": "RN-01", "timestamp": "2026-11-17T13:00:00Z"}
```

| Código HTTP | Cuándo |
|---|---|
| `200` / `201` | Éxito (`201` al crear; `DELETE` responde `200` sin cuerpo) |
| `400` | Datos faltantes o con formato inválido |
| `401` | Sin token, o con un token alterado o vencido; credenciales incorrectas en el login |
| `403` | El usuario no tiene permiso |
| `404` | El recurso o la ruta no existen |
| `409` | Se viola una regla de negocio o hay un duplicado |

## 6. Pruebas

### Escenarios de aceptación

[`pruebas/escenarios.py`](pruebas/escenarios.py) ejecuta los **14 escenarios del enunciado** y
una prueba por cada regla de negocio, permiso y tipo de error (**78 pasos**). Solo necesita
Python 3. La aplicación debe estar **recién arrancada** y con el reloj a las 8:00:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--coworkhub.reloj.fijo=08:00
```

En otra terminal:

```bash
python3 pruebas/escenarios.py
```

El último resultado está en [`docs/resultados-pruebas.md`](docs/resultados-pruebas.md)
(78 de 78 correctos). Para volver a generarlo, agrega `--informe docs/resultados-pruebas.md`.

| # | Escenario | Resultado esperado |
|---|---|---|
| 1 | Reservar una sala libre con 2 servicios | `201` con el costo calculado |
| 2 | Reservar en un horario que se cruza con otra reserva | `409` `RN-01` |
| 3 | Reservar de 12:00 a 13:00 cuando otra va de 10:00 a 12:00 | `201` |
| 4 | Reservar 45 min, o fuera del horario de la sede | `409` `RN-02` / `RN-03` |
| 5 | Cancelar con 30 h de anticipación | `200`, cargo 0 |
| 6 | Cancelar con 5 h de anticipación | `200`, cargo del 50 % |
| 7 | Cancelar con 1 h de anticipación | `409` `RN-08` |
| 8 | Confirmar una reserva cancelada | `409` `RN-09` |
| 9 | Un miembro consulta la reserva de otro | `403` |
| 10 | Endpoint protegido sin token, con token alterado o vencido | `401` |
| 11 | Eliminar una sala con reservas futuras | `409` `RN-12` |
| 12 | Crear una sede sin nombre | `400` |
| 13 | **6** solicitudes simultáneas para la misma sala y horario | Una `201` y cinco `409` |
| 14 | Listar miembros con `?estado=SUSPENDED` | `200` solo con suspendidos |

### Colección de Postman / Insomnia

[`docs/coworkhub.postman_collection.json`](docs/coworkhub.postman_collection.json) tiene las
mismas solicitudes con sus pruebas (código HTTP, `codigo` del error y valores esperados). Se
importa en Postman o en Insomnia (*Import → File*). Arranca la API recién iniciada con el reloj
a las 8:00 y ejecuta la colección completa, en orden (*Run collection*). Las fechas se calculan
solas a partir de hoy.

El escenario 13 (solicitudes simultáneas) y el token vencido solo están en el script de Python:
Postman envía las solicitudes de una en una y no firma tokens sin scripts adicionales. La
colección se regenera con `python3 pruebas/escenarios.py --exportar docs/coworkhub.postman_collection.json`.

### Pruebas unitarias

```bash
mvn test
```

`CalculadoraCostoReservaTest` comprueba las fórmulas de dinero (RN-07 y RN-08) sin base de datos.

### Sin N+1

Con `spring.jpa.show-sql=true` (activado), cada listado emite **una sola** consulta, con sus
`join`, en vez de una por fila. Medido: `GET /api/reservas?estado=PENDING` (10 filas),
`GET /api/salas` (7 filas) y `GET /api/miembros` (7 filas) hacen 1 consulta cada uno.

## 7. Estructura

```text
src/main/java/com/coworkhub
├── Main.java
├── config/              beans (Clock, PasswordEncoder), OpenAPI, seguridad y permisos
├── jwt/                 UtilJwt (crear y validar tokens) y FiltroAutenticacionJwt
├── presentacion/
│   ├── controlador/     controladores REST (solo llaman a servicios)
│   ├── dto/             records de entrada (Solicitud…) y salida (Respuesta…)
│   └── excepcion/       ManejadorGlobalDeExcepciones (@RestControllerAdvice)
├── negocio/
│   ├── model/           entidades JPA y enums
│   ├── service/         servicios (interfaz + impl) y CalculadoraCostoReserva
│   └── excepcion/       excepciones propias
└── persistence/
    └── repository/      repositorios Spring Data JPA
```

## 8. ⚠️ Advertencia de seguridad (educativa)

Este repositorio es un proyecto académico. Para que arranque sin configurar nada, incluye
valores que **nunca** deben ir en un repositorio real:

- La **clave de firma JWT** en `application.properties`. Quien la conozca puede fabricar tokens
  válidos de cualquier usuario. En producción va en la variable `COWORKHUB_JWT_SECRETO` o en un
  gestor de secretos.
- La **contraseña de la base de datos** (`coworkhub`) y las de los usuarios de prueba. Las de
  los usuarios están guardadas con BCrypt en `import.sql`, nunca en texto plano.
- Todos los datos personales son **ficticios**.
