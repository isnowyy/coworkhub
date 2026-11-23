// Inicio: para un miembro, su consumo del mes y sus próximas reservas; para el personal, un resumen.

import { api, consulta } from "../api.js";
import { dinero, esc, etiqueta, fecha, horario, mesActual } from "../ui.js";

export async function vistaInicio(contenido, sesion) {
    if (sesion.rol === "MEMBER") return inicioMiembro(contenido, sesion);
    return inicioPersonal(contenido, sesion);
}

async function inicioMiembro(contenido, sesion) {
    const [miembro, consumo, reservas] = await Promise.all([
        api.obtener(`/api/miembros/${sesion.miembroId}`),
        api.obtener(`/api/miembros/${sesion.miembroId}/consumo${consulta({ mes: mesActual() })}`),
        api.obtener(`/api/reservas${consulta({ miembroId: sesion.miembroId })}`),
    ]);

    const proximas = reservas
        .filter((r) => (r.estado === "PENDING" || r.estado === "CONFIRMED") && new Date(r.inicio) > new Date())
        .sort((a, b) => a.inicio.localeCompare(b.inicio));

    const incluidas = Number(consumo.horasIncluidas);
    const usadas = Number(consumo.horasUsadas);
    const porcentaje = incluidas > 0 ? Math.min(100, (usadas / incluidas) * 100) : 100;
    const excedida = Number(consumo.horasExcedentes) > 0;

    contenido.innerHTML = `
        <div class="cabecera-vista">
            <div>
                <h1>Hola, ${esc(miembro.nombreCompleto.split(" ")[0])}</h1>
                <p>Plan ${esc(miembro.planNombre)} · ${etiqueta(miembro.estado)}</p>
            </div>
            <a class="boton boton--primario" href="#disponibilidad">Buscar una sala libre</a>
        </div>

        <div class="metricas">
            ${metrica(consumo.horasIncluidas, "Horas incluidas este mes")}
            ${metrica(consumo.horasUsadas, "Horas usadas")}
            ${metrica(consumo.horasRestantes, "Horas restantes")}
            ${metrica(consumo.horasExcedentes, "Horas excedentes (se cobran)")}
        </div>

        <section class="tarjeta">
            <div class="tarjeta__titulo"><h2>Consumo de ${esc(consumo.mes)}</h2></div>
            <div class="barra-progreso ${excedida ? "barra-progreso--excedida" : ""}"><span style="width:${porcentaje}%"></span></div>
            <p class="secundario">Las reservas canceladas con 24 h o más de anticipación devuelven sus horas (RN-08).</p>
        </section>

        <section class="tarjeta">
            <div class="tarjeta__titulo">
                <h2>Próximas reservas</h2>
                <a href="#reservas">Ver todas</a>
            </div>
            ${tablaReservasCortas(proximas)}
        </section>`;
}

async function inicioPersonal(contenido, sesion) {
    const [pendientes, confirmadas, suspendidos, salas] = await Promise.all([
        api.obtener(`/api/reservas${consulta({ estado: "PENDING" })}`),
        api.obtener(`/api/reservas${consulta({ estado: "CONFIRMED" })}`),
        api.obtener(`/api/miembros${consulta({ estado: "SUSPENDED" })}`),
        api.obtener("/api/salas"),
    ]);

    const porConfirmar = pendientes
        .filter((r) => new Date(r.inicio) > new Date())
        .sort((a, b) => a.inicio.localeCompare(b.inicio));

    contenido.innerHTML = `
        <div class="cabecera-vista">
            <div>
                <h1>Panel de ${sesion.rol === "ADMIN" ? "administración" : "recepción"}</h1>
                <p>Resumen de la red de coworking.</p>
            </div>
            <a class="boton boton--primario" href="#nueva-reserva">Nueva reserva</a>
        </div>

        <div class="metricas">
            ${metrica(pendientes.length, "Reservas pendientes")}
            ${metrica(confirmadas.length, "Reservas confirmadas")}
            ${metrica(salas.filter((s) => s.activa).length, "Salas activas")}
            ${metrica(suspendidos.length, "Miembros suspendidos")}
        </div>

        <section class="tarjeta">
            <div class="tarjeta__titulo">
                <h2>Pendientes por confirmar</h2>
                <a href="#reservas?estado=PENDING">Gestionar</a>
            </div>
            ${tablaReservasCortas(porConfirmar, true)}
        </section>`;
}

const metrica = (valor, nombre) => `
    <div class="metrica"><div class="metrica__valor">${esc(valor)}</div><div class="metrica__nombre">${esc(nombre)}</div></div>`;

function tablaReservasCortas(reservas, conMiembro = false) {
    if (reservas.length === 0) return '<p class="vacio">No hay reservas por ahora.</p>';
    return `
        <div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>Fecha</th><th>Horario</th><th>Sala</th>${conMiembro ? "<th>Miembro</th>" : ""}<th>Estado</th><th class="num">Total</th></tr></thead>
            <tbody>${reservas.slice(0, 8).map((r) => `
                <tr>
                    <td>${esc(fecha(r.inicio))}</td>
                    <td>${esc(horario(r.inicio, r.fin))}</td>
                    <td>${esc(r.sala)}<div class="secundario">${esc(r.sede)}</div></td>
                    ${conMiembro ? `<td>${esc(r.miembro)}</td>` : ""}
                    <td>${etiqueta(r.estado)}</td>
                    <td class="num">${dinero(r.costoTotal)}</td>
                </tr>`).join("")}
            </tbody>
        </table></div>`;
}
