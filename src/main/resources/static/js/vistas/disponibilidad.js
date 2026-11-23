// Disponibilidad (RF-03): salas libres de una sede en una fecha y un rango horario.

import { api, consulta } from "../api.js";
import { avisarError, dinero, esc, fechaLocal, horaCorta, leerFormulario, NOMBRES, opciones, opcionesDeHora } from "../ui.js";

export async function vistaDisponibilidad(contenido) {
    const [sedes, equipamientos] = await Promise.all([api.obtener("/api/sedes"), api.obtener("/api/equipamientos")]);

    contenido.innerHTML = `
        <div class="cabecera-vista">
            <div>
                <h1>Disponibilidad</h1>
                <p>Salas activas sin reservas que se crucen con el horario elegido.</p>
            </div>
        </div>

        <section class="tarjeta">
            <form id="form-disponibilidad" class="formulario">
                <div class="formulario formulario--fila">
                    <label class="campo"><span>Sede</span>
                        <select name="sedeId" required>${opciones(sedes, (s) => s.id, (s) => `${s.nombre} (${horaCorta(s.horaApertura)}–${horaCorta(s.horaCierre)})`)}</select>
                    </label>
                    <label class="campo"><span>Fecha</span><input name="fecha" type="date" required value="${fechaLocal(1)}" min="${fechaLocal()}"></label>
                    <label class="campo"><span>Desde</span><select name="horaInicio">${opcionesDeHora(6, 22, "10:00")}</select></label>
                    <label class="campo"><span>Hasta</span><select name="horaFin">${opcionesDeHora(6, 22, "12:00")}</select></label>
                    <label class="campo"><span>Capacidad mínima</span><input name="capacidadMinima" type="number" min="1" data-tipo="numero" placeholder="Opcional"></label>
                </div>
                <div class="campo"><span>Equipamiento necesario</span>
                    <div class="grupo-checks">${equipamientos.map((e) => `
                        <label><input type="checkbox" name="equipamientoIds" value="${e.id}" data-lista> ${esc(e.nombre)}</label>`).join("")}
                    </div>
                </div>
                <div class="acciones-formulario"><button class="boton boton--primario" type="submit">Buscar salas libres</button></div>
            </form>
        </section>

        <section id="resultado" class="tarjeta" hidden></section>`;

    const form = contenido.querySelector("#form-disponibilidad");
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const filtro = leerFormulario(form);
        try {
            const salas = await api.obtener(`/api/disponibilidad${consulta(filtro)}`);
            mostrarResultado(contenido.querySelector("#resultado"), salas, filtro);
        } catch (err) {
            avisarError(err);
        }
    });
    form.requestSubmit();
}

function mostrarResultado(seccion, salas, filtro) {
    seccion.hidden = false;
    const enlace = (sala) => `#nueva-reserva?${new URLSearchParams({
        sedeId: filtro.sedeId, salaId: sala.id, fecha: filtro.fecha, inicio: filtro.horaInicio, fin: filtro.horaFin,
        asistentes: filtro.capacidadMinima ?? 1,
    })}`;

    seccion.innerHTML = `
        <div class="tarjeta__titulo"><h2>${salas.length} ${salas.length === 1 ? "sala libre" : "salas libres"}</h2>
            <span class="secundario">${esc(filtro.fecha)} · ${esc(filtro.horaInicio)} – ${esc(filtro.horaFin)}</span></div>
        ${salas.length === 0 ? '<p class="vacio">No hay salas libres con esos filtros. Prueba otro horario u otra sede.</p>' : `
        <div class="rejilla-salas">${salas.map((s) => `
            <article class="tarjeta sala">
                <h3>${esc(s.nombre)}</h3>
                <p class="secundario">${esc(NOMBRES[s.tipo] ?? s.tipo)} · hasta ${s.capacidad} ${s.capacidad === 1 ? "persona" : "personas"}</p>
                <p class="secundario">${s.equipamiento.map((e) => esc(e.nombre)).join(" · ") || "Sin equipamiento"}</p>
                <div class="sala__pie">
                    <strong>${dinero(s.tarifaHora)} / hora</strong>
                    <a class="boton boton--primario boton--chico" href="${enlace(s)}">Reservar</a>
                </div>
            </article>`).join("")}
        </div>`}`;
}
