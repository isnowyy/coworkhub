// Reservas: crear (RF-04), listar y consultar (RF-05), servicios (RF-06), confirmar (RF-07),
// cancelar (RF-08) y completar (RF-09). Las reglas (RN-01 a RN-10) las aplica el backend: aquí solo
// se muestran los botones que tienen sentido para el rol y el estado, y el error que devuelva la API.

import { api, consulta } from "../api.js";
import {
    abrirModal, avisar, avisarError, confirmar, dinero, esc, etiqueta, fecha, fechaLocal, horario,
    leerFormulario, mientrasEnvia, NOMBRES, opciones, opcionesDeHora,
} from "../ui.js";

const esPersonal = (sesion) => sesion.rol === "ADMIN" || sesion.rol === "RECEPTIONIST";

// ---------------------------------------------------------------- Listado

export async function vistaReservas(contenido, sesion, parametros) {
    if (!esPersonal(sesion)) {
        contenido.innerHTML = cabecera("Mis reservas", "Tus reservas, de la más reciente a la más antigua.");
        const seccion = document.createElement("section");
        seccion.className = "tarjeta";
        contenido.append(seccion);
        const cargar = async () => {
            const reservas = await api.obtener(`/api/reservas${consulta({ miembroId: sesion.miembroId })}`);
            seccion.innerHTML = tablaReservas(reservas, false);
        };
        engancharTabla(seccion, sesion, cargar);
        return cargar();
    }

    const [miembros, sedes, salas] = await Promise.all([
        api.obtener("/api/miembros"), api.obtener("/api/sedes"), api.obtener("/api/salas"),
    ]);
    const nombreSede = Object.fromEntries(sedes.map((s) => [s.id, s.nombre]));

    contenido.innerHTML = `
        ${cabecera("Reservas", "Filtra por estado, por miembro o por sala y fecha.")}
        <section class="tarjeta">
            <form id="form-filtro" class="formulario formulario--fila">
                <label class="campo"><span>Filtrar por</span>
                    <select name="tipo">
                        <option value="estado">Estado</option>
                        <option value="miembro">Miembro</option>
                        <option value="sala">Sala y fecha</option>
                    </select>
                </label>
                <label class="campo" data-filtro="estado"><span>Estado</span>
                    <select name="estado">${["PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"].map((e) => `<option value="${e}">${NOMBRES[e]}</option>`).join("")}</select>
                </label>
                <label class="campo" data-filtro="miembro" hidden><span>Miembro</span>
                    <select name="miembroId">${opciones(miembros, (m) => m.id, (m) => m.nombreCompleto)}</select>
                </label>
                <label class="campo" data-filtro="sala" hidden><span>Sala</span>
                    <select name="salaId">${opciones(salas, (s) => s.id, (s) => `${s.nombre} · ${nombreSede[s.sedeId]}`)}</select>
                </label>
                <label class="campo" data-filtro="sala" hidden><span>Fecha</span><input type="date" name="fecha" value="${fechaLocal(1)}"></label>
                <button class="boton boton--primario" type="submit">Ver reservas</button>
            </form>
        </section>
        <section class="tarjeta" id="lista"></section>`;

    const form = contenido.querySelector("#form-filtro");
    const lista = contenido.querySelector("#lista");
    if (parametros.get("estado")) form.estado.value = parametros.get("estado");

    const mostrarCampos = () => {
        form.querySelectorAll("[data-filtro]").forEach((campo) => { campo.hidden = campo.dataset.filtro !== form.tipo.value; });
    };
    form.tipo.addEventListener("change", mostrarCampos);

    const cargar = async () => {
        const f = leerFormulario(form);
        const filtro = f.tipo === "estado" ? { estado: f.estado }
            : f.tipo === "miembro" ? { miembroId: f.miembroId }
            : { salaId: f.salaId, fecha: f.fecha };
        const reservas = await api.obtener(`/api/reservas${consulta(filtro)}`);
        lista.innerHTML = tablaReservas(reservas, true);
    };
    form.addEventListener("submit", (e) => { e.preventDefault(); cargar().catch(avisarError); });
    engancharTabla(lista, sesion, cargar);
    return cargar();
}

const cabecera = (titulo, texto) => `
    <div class="cabecera-vista">
        <div><h1>${esc(titulo)}</h1><p>${esc(texto)}</p></div>
        <a class="boton boton--primario" href="#nueva-reserva">Nueva reserva</a>
    </div>`;

function tablaReservas(reservas, conMiembro) {
    if (reservas.length === 0) return '<p class="vacio">No hay reservas con ese filtro.</p>';
    const ordenadas = [...reservas].sort((a, b) => b.inicio.localeCompare(a.inicio));
    return `
        <div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>#</th><th>Fecha</th><th>Horario</th><th>Sala</th>${conMiembro ? "<th>Miembro</th>" : ""}
                <th class="num">Asist.</th><th>Estado</th><th class="num">Total</th><th></th></tr></thead>
            <tbody>${ordenadas.map((r) => `
                <tr>
                    <td>${r.id}</td>
                    <td>${esc(fecha(r.inicio))}</td>
                    <td>${esc(horario(r.inicio, r.fin))}</td>
                    <td>${esc(r.sala)}<div class="secundario">${esc(r.sede)}</div></td>
                    ${conMiembro ? `<td>${esc(r.miembro)}</td>` : ""}
                    <td class="num">${r.asistentes}</td>
                    <td>${etiqueta(r.estado)}</td>
                    <td class="num">${dinero(r.costoTotal)}${Number(r.cargoCancelacion) > 0 ? `<div class="secundario">cargo ${dinero(r.cargoCancelacion)}</div>` : ""}</td>
                    <td class="acciones"><button class="boton boton--chico" data-ver="${r.id}">Ver</button></td>
                </tr>`).join("")}
            </tbody>
        </table></div>`;
}

function engancharTabla(contenedor, sesion, recargar) {
    contenedor.addEventListener("click", (e) => {
        const boton = e.target.closest("[data-ver]");
        if (boton) abrirDetalle(Number(boton.dataset.ver), sesion, recargar);
    });
}

// ---------------------------------------------------------------- Detalle y acciones

async function abrirDetalle(id, sesion, recargar) {
    let reserva;
    let servicios;
    try {
        [reserva, servicios] = await Promise.all([api.obtener(`/api/reservas/${id}`), api.obtener("/api/servicios-adicionales")]);
    } catch (err) {
        return avisarError(err);
    }

    const pendiente = reserva.estado === "PENDING";
    const activa = pendiente || reserva.estado === "CONFIRMED";
    const personal = esPersonal(sesion);

    const cuerpo = abrirModal(`Reserva #${reserva.id}`, `
        <div class="bloque">
            <dl class="datos">
                <dt>Estado</dt><dd>${etiqueta(reserva.estado)}</dd>
                <dt>Miembro</dt><dd>${esc(reserva.miembro)}</dd>
                <dt>Sala</dt><dd>${esc(reserva.sala)} · ${esc(reserva.sede)}</dd>
                <dt>Fecha</dt><dd>${esc(fecha(reserva.inicio))}, ${esc(horario(reserva.inicio, reserva.fin))}</dd>
                <dt>Asistentes</dt><dd>${reserva.asistentes}</dd>
                <dt>Horas del plan</dt><dd>${(reserva.minutosDelPlanConsumidos / 60).toLocaleString("es-CO")} h consumidas</dd>
            </dl>
            ${costos(reserva)}
        </div>

        <div class="bloque">
            <h3>Servicios adicionales</h3>
            ${reserva.servicios.length === 0 ? '<p class="secundario">Sin servicios.</p>' : `
            <table class="tabla">
                <thead><tr><th>Servicio</th><th class="num">Cant.</th><th class="num">Precio aplicado</th><th class="num">Subtotal</th>${pendiente ? "<th></th>" : ""}</tr></thead>
                <tbody>${reserva.servicios.map((s) => `
                    <tr><td>${esc(s.servicio)}</td><td class="num">${s.cantidad}</td><td class="num">${dinero(s.precioUnitarioAplicado)}</td>
                        <td class="num">${dinero(s.subtotal)}</td>
                        ${pendiente ? `<td class="acciones"><button class="boton boton--chico boton--peligro" data-quitar="${s.servicioId}">Quitar</button></td>` : ""}</tr>`).join("")}
                </tbody>
            </table>`}
            ${pendiente ? `
            <form id="form-servicio" class="formulario formulario--fila">
                <label class="campo"><span>Agregar servicio</span><select name="servicioId">${opciones(servicios, (s) => s.id, (s) => `${s.nombre} · ${dinero(s.precioUnitario)}`)}</select></label>
                <label class="campo"><span>Cantidad</span><input name="cantidad" type="number" min="1" value="1" data-tipo="numero" required></label>
                <button class="boton" type="submit">Agregar</button>
            </form>
            <p class="secundario">Los servicios solo se cambian mientras la reserva está pendiente (RN-10). El precio queda fijo al agregarlo (RN-07).</p>` : ""}
        </div>

        <div class="bloque">
            <div class="acciones-formulario">
                ${activa ? '<button class="boton boton--peligro" data-accion="cancelar">Cancelar reserva</button>' : ""}
                ${personal && pendiente ? '<button class="boton boton--primario" data-accion="confirmar">Confirmar</button>' : ""}
                ${personal && reserva.estado === "CONFIRMED" ? '<button class="boton boton--primario" data-accion="completar">Marcar completada</button>' : ""}
            </div>
            ${activa ? '<p class="secundario">Cancelar con 24 h o más no tiene cargo; entre 24 h y 2 h se cobra el 50 % de la sala; con menos de 2 h no se puede (RN-08).</p>' : ""}
        </div>`);

    const tras = async (mensaje) => {
        avisar(mensaje);
        await recargar?.().catch(avisarError);
        abrirDetalle(id, sesion, recargar);
    };

    cuerpo.querySelector("#form-servicio")?.addEventListener("submit", (e) => {
        e.preventDefault();
        const form = e.currentTarget;
        mientrasEnvia(form, async () => {
            try {
                await api.crear(`/api/reservas/${id}/servicios`, leerFormulario(form));
                await tras("Servicio agregado y costo recalculado.");
            } catch (err) { avisarError(err); }
        });
    });

    // onclick (y no addEventListener): el cuerpo de la modal se reutiliza y no deben acumularse manejadores.
    cuerpo.onclick = async (e) => {
        const quitar = e.target.closest("[data-quitar]");
        const accion = e.target.closest("[data-accion]")?.dataset.accion;
        try {
            if (quitar) {
                await api.eliminar(`/api/reservas/${id}/servicios/${quitar.dataset.quitar}`);
                await tras("Servicio quitado.");
            } else if (accion === "cancelar") {
                if (!(await confirmar("Cancelar la reserva", "¿Seguro que quieres cancelarla? Si faltan menos de 24 h se cobra el 50 % de la sala.", "Sí, cancelar"))) {
                    return abrirDetalle(id, sesion, recargar);
                }
                const cancelada = await api.accion(`/api/reservas/${id}/cancelar`);
                await tras(Number(cancelada.cargoCancelacion) > 0
                    ? `Reserva cancelada con un cargo de ${dinero(cancelada.cargoCancelacion)}.`
                    : "Reserva cancelada sin cargo. Las horas volvieron al plan.");
            } else if (accion === "confirmar") {
                await api.accion(`/api/reservas/${id}/confirmar`);
                await tras("Reserva confirmada.");
            } else if (accion === "completar") {
                await api.accion(`/api/reservas/${id}/completar`);
                await tras("Reserva completada.");
            }
        } catch (err) {
            avisarError(err);
        }
    };
}

function costos(r) {
    return `
        <div class="costos">
            <div><span>Sala</span><span>${dinero(r.costoSala)}</span></div>
            <div><span>Servicios</span><span>${dinero(r.costoServicios)}</span></div>
            <div class="total"><span>Total</span><span>${dinero(r.costoTotal)}</span></div>
            ${Number(r.cargoCancelacion) > 0 ? `<div><span>Cargo por cancelación</span><span>${dinero(r.cargoCancelacion)}</span></div>` : ""}
        </div>`;
}

// ---------------------------------------------------------------- Nueva reserva

export async function vistaNuevaReserva(contenido, sesion, parametros) {
    const personal = esPersonal(sesion);
    const [sedes, salas, servicios, miembros] = await Promise.all([
        api.obtener("/api/sedes"),
        api.obtener("/api/salas"),
        api.obtener("/api/servicios-adicionales"),
        personal ? api.obtener(`/api/miembros${consulta({ estado: "ACTIVE" })}`) : Promise.resolve([]),
    ]);

    const sedeInicial = parametros.get("sedeId") ?? sedes[0]?.id;

    contenido.innerHTML = `
        <div class="cabecera-vista">
            <div><h1>Nueva reserva</h1><p>El costo se calcula con las horas del plan, el excedente y los servicios.</p></div>
            <a class="boton" href="#disponibilidad">Ver disponibilidad</a>
        </div>

        <section class="tarjeta">
            <form id="form-reserva" class="formulario">
                <div class="formulario formulario--fila">
                    ${personal ? `<label class="campo"><span>Miembro</span>
                        <select name="miembroId" data-tipo="numero" required>${opciones(miembros, (m) => m.id, (m) => `${m.nombreCompleto} · ${m.planNombre}`)}</select></label>` : ""}
                    <label class="campo"><span>Sede</span><select name="sedeId">${opciones(sedes, (s) => s.id, (s) => s.nombre, sedeInicial)}</select></label>
                    <label class="campo"><span>Sala</span><select name="salaId" data-tipo="numero" required></select></label>
                    <label class="campo"><span>Asistentes</span><input name="asistentes" type="number" min="1" required data-tipo="numero" value="${esc(parametros.get("asistentes") ?? 1)}"></label>
                </div>
                <div class="formulario formulario--fila">
                    <label class="campo"><span>Fecha</span><input name="fecha" type="date" required value="${esc(parametros.get("fecha") ?? fechaLocal(1))}"></label>
                    <label class="campo"><span>Desde</span><select name="inicio">${opcionesDeHora(6, 22, parametros.get("inicio") ?? "10:00")}</select></label>
                    <label class="campo"><span>Hasta</span><select name="fin">${opcionesDeHora(6, 22, parametros.get("fin") ?? "12:00")}</select></label>
                </div>
                <div class="campo"><span>Servicios adicionales (cantidad)</span>
                    <div class="formulario formulario--fila">${servicios.map((s) => `
                        <label class="campo"><span>${esc(s.nombre)} · ${dinero(s.precioUnitario)}</span>
                            <input type="number" min="0" value="0" data-servicio="${s.id}"></label>`).join("")}
                    </div>
                </div>
                <div class="acciones-formulario"><button class="boton boton--primario" type="submit">Crear reserva</button></div>
            </form>
        </section>
        <section id="creada" class="tarjeta" hidden></section>`;

    const form = contenido.querySelector("#form-reserva");
    const llenarSalas = () => {
        const deLaSede = salas.filter((s) => String(s.sedeId) === form.sedeId.value);
        form.salaId.innerHTML = opciones(deLaSede, (s) => s.id,
            (s) => `${s.nombre} · ${s.capacidad} pers. · ${dinero(s.tarifaHora)}/h${s.activa ? "" : " · inactiva"}`,
            parametros.get("salaId"));
    };
    form.sedeId.addEventListener("change", llenarSalas);
    llenarSalas();

    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const datos = leerFormulario(form);
        const solicitud = {
            miembroId: personal ? datos.miembroId : sesion.miembroId,
            salaId: datos.salaId,
            inicio: `${datos.fecha}T${datos.inicio}:00`,
            fin: `${datos.fecha}T${datos.fin}:00`,
            asistentes: datos.asistentes,
            servicios: [...form.querySelectorAll("[data-servicio]")]
                .map((campo) => ({ servicioId: Number(campo.dataset.servicio), cantidad: Number(campo.value) }))
                .filter((item) => item.cantidad > 0),
        };
        mientrasEnvia(form, async () => {
            try {
                const reserva = await api.crear("/api/reservas", solicitud);
                avisar(`Reserva #${reserva.id} creada.`);
                mostrarCreada(contenido.querySelector("#creada"), reserva, sesion);
            } catch (err) {
                avisarError(err);
            }
        });
    });
}

function mostrarCreada(seccion, reserva, sesion) {
    seccion.hidden = false;
    seccion.innerHTML = `
        <div class="tarjeta__titulo"><h2>Reserva #${reserva.id} creada</h2>${etiqueta(reserva.estado)}</div>
        <div class="bloque">
            <p>${esc(reserva.sala)} · ${esc(reserva.sede)} — ${esc(fecha(reserva.inicio))}, ${esc(horario(reserva.inicio, reserva.fin))}</p>
            ${costos(reserva)}
            <p class="secundario">${(reserva.minutosDelPlanConsumidos / 60).toLocaleString("es-CO")} h cubiertas por el plan de ${esc(reserva.miembro)}.</p>
            <div class="acciones-formulario"><button class="boton" data-ver="${reserva.id}">Ver detalle</button></div>
        </div>`;
    seccion.querySelector("[data-ver]").onclick = () => abrirDetalle(reserva.id, sesion);
    seccion.scrollIntoView({ behavior: "smooth", block: "nearest" });
}
