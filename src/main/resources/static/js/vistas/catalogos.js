// Catálogos (RF-01): sedes, salas, equipamiento, servicios adicionales y planes.
// Cualquier usuario con sesión los consulta; solo ADMIN crea, edita y elimina (RN-13).

import { api } from "../api.js";
import {
    abrirModal, avisar, avisarError, cerrarModal, confirmar, dinero, esc, horaCorta, leerFormulario, mientrasEnvia,
    NOMBRES, opciones,
} from "../ui.js";

/**
 * Cada catálogo describe su ruta, sus columnas y su formulario. `campos` arma el formulario de
 * creación/edición; `aSolicitud` convierte lo leído al DTO de entrada que espera la API.
 */
const CATALOGOS = [
    {
        id: "sedes", nombre: "Sedes", singular: "sede", nuevo: "Nueva sede", ruta: "/api/sedes",
        columnas: ["Nombre", "Ciudad", "Dirección", "Horario"],
        fila: (s) => [esc(s.nombre), esc(s.ciudad), esc(s.direccion), `${horaCorta(s.horaApertura)} – ${horaCorta(s.horaCierre)}`],
        campos: (s) => `
            ${texto("nombre", "Nombre", s?.nombre, 100)}
            ${texto("ciudad", "Ciudad", s?.ciudad, 80)}
            ${texto("direccion", "Dirección", s?.direccion, 150)}
            <div class="formulario formulario--fila">
                <label class="campo"><span>Abre</span><input name="horaApertura" type="time" required value="${horaCorta(s?.horaApertura ?? "07:00")}"></label>
                <label class="campo"><span>Cierra</span><input name="horaCierre" type="time" required value="${horaCorta(s?.horaCierre ?? "20:00")}"></label>
            </div>`,
        aSolicitud: (d) => d,
    },
    {
        id: "salas", nombre: "Salas", singular: "sala", nuevo: "Nueva sala", ruta: "/api/salas", necesita: ["sedes", "equipamientos"],
        columnas: ["Nombre", "Sede", "Tipo", "Capacidad", "Tarifa/hora", "Equipamiento", "Estado"],
        fila: (s) => [esc(s.nombre), esc(s.sedeNombre), esc(NOMBRES[s.tipo]), s.capacidad, dinero(s.tarifaHora),
            `<span class="secundario">${s.equipamiento.map((e) => esc(e.nombre)).join(", ") || "—"}</span>`,
            s.activa ? '<span class="etiqueta etiqueta--ACTIVE">Activa</span>' : '<span class="etiqueta etiqueta--CANCELLED">Inactiva</span>'],
        campos: (s, aux) => `
            ${texto("nombre", "Nombre", s?.nombre, 80)}
            <div class="formulario formulario--fila">
                <label class="campo"><span>Sede</span><select name="sedeId" data-tipo="numero">${opciones(aux.sedes, (x) => x.id, (x) => x.nombre, s?.sedeId)}</select></label>
                <label class="campo"><span>Tipo</span><select name="tipo">${["MEETING_ROOM", "PRIVATE_OFFICE", "DESK"].map((t) => `<option value="${t}" ${s?.tipo === t ? "selected" : ""}>${NOMBRES[t]}</option>`).join("")}</select></label>
                ${numero("capacidad", "Capacidad", s?.capacidad, 1)}
                ${numero("tarifaHora", "Tarifa por hora (COP)", s?.tarifaHora, 1)}
            </div>
            <label class="campo--check"><input type="checkbox" name="activa" ${s?.activa === false ? "" : "checked"}> Sala activa (se puede reservar)</label>
            <div class="campo"><span>Equipamiento</span><div class="grupo-checks">${aux.equipamientos.map((e) => `
                <label><input type="checkbox" name="equipamientoIds" value="${e.id}" data-lista ${s?.equipamiento.some((x) => x.id === e.id) ? "checked" : ""}> ${esc(e.nombre)}</label>`).join("")}
            </div></div>`,
        aSolicitud: (d) => d,
    },
    {
        id: "equipamientos", nombre: "Equipamiento", singular: "equipamiento", nuevo: "Nuevo equipamiento", ruta: "/api/equipamientos",
        columnas: ["Nombre"],
        fila: (e) => [esc(e.nombre)],
        campos: (e) => texto("nombre", "Nombre", e?.nombre, 80),
        aSolicitud: (d) => d,
    },
    {
        id: "servicios", nombre: "Servicios adicionales", singular: "servicio", nuevo: "Nuevo servicio", ruta: "/api/servicios-adicionales",
        columnas: ["Nombre", "Precio unitario"],
        fila: (s) => [esc(s.nombre), dinero(s.precioUnitario)],
        campos: (s) => `${texto("nombre", "Nombre", s?.nombre, 80)}${numero("precioUnitario", "Precio unitario (COP)", s?.precioUnitario, 0)}
            <p class="secundario">Cambiar el precio no altera las reservas existentes: cada una guarda el precio vigente al agregarlo (RN-07).</p>`,
        aSolicitud: (d) => d,
    },
    {
        id: "planes", nombre: "Planes", singular: "plan", nuevo: "Nuevo plan", ruta: "/api/planes",
        columnas: ["Nombre", "Horas al mes", "Descuento excedente", "Máx. reservas activas"],
        fila: (p) => [esc(p.nombre), p.horasIncluidasMes, `${p.descuentoExcedente} %`, p.maxReservasActivas],
        campos: (p) => `
            ${texto("nombre", "Nombre", p?.nombre, 80)}
            <div class="formulario formulario--fila">
                ${numero("horasIncluidasMes", "Horas incluidas al mes", p?.horasIncluidasMes, 0)}
                ${numero("descuentoExcedente", "Descuento excedente (%)", p?.descuentoExcedente, 0, 100)}
                ${numero("maxReservasActivas", "Máx. reservas activas", p?.maxReservasActivas, 1)}
            </div>`,
        aSolicitud: (d) => d,
    },
];

const texto = (nombre, etiquetaCampo, valor, max) =>
    `<label class="campo"><span>${etiquetaCampo}</span><input name="${nombre}" required maxlength="${max}" value="${esc(valor)}"></label>`;

const numero = (nombre, etiquetaCampo, valor, min, max) =>
    `<label class="campo"><span>${etiquetaCampo}</span><input name="${nombre}" type="number" required data-tipo="numero"
        min="${min}" ${max === undefined ? "" : `max="${max}"`} step="any" value="${esc(valor)}"></label>`;

export async function vistaCatalogos(contenido, sesion, parametros) {
    const esAdmin = sesion.rol === "ADMIN";
    const actual = CATALOGOS.find((c) => c.id === parametros.get("ver")) ?? CATALOGOS[0];

    contenido.innerHTML = `
        <div class="cabecera-vista">
            <div><h1>Catálogos</h1><p>${esAdmin ? "Crea, edita y elimina los datos maestros." : "Consulta de los datos maestros. Solo el administrador los modifica."}</p></div>
            ${esAdmin ? `<button class="boton boton--primario" id="nuevo">${esc(actual.nuevo)}</button>` : ""}
        </div>
        <div class="pestanas" role="tablist">${CATALOGOS.map((c) => `
            <a class="pestana" role="tab" href="#catalogos?ver=${c.id}" aria-selected="${c === actual}">${c.nombre}</a>`).join("")}
        </div>
        <section class="tarjeta" id="lista"><p class="cargando">Cargando…</p></section>`;

    const lista = contenido.querySelector("#lista");
    const aux = {};
    let registros = [];

    const cargar = async () => {
        const [datos, ...extra] = await Promise.all([
            api.obtener(actual.ruta),
            ...(actual.necesita ?? []).map((id) => api.obtener(CATALOGOS.find((c) => c.id === id).ruta)),
        ]);
        (actual.necesita ?? []).forEach((id, i) => { aux[id] = extra[i]; });
        registros = datos;
        lista.innerHTML = registros.length === 0 ? '<p class="vacio">Todavía no hay registros.</p>' : `
            <div class="tabla-contenedor"><table class="tabla">
                <thead><tr><th>#</th>${actual.columnas.map((c) => `<th>${c}</th>`).join("")}${esAdmin ? "<th></th>" : ""}</tr></thead>
                <tbody>${registros.map((r) => `
                    <tr><td>${r.id}</td>${actual.fila(r).map((celda) => `<td>${celda}</td>`).join("")}
                        ${esAdmin ? `<td class="acciones">
                            <button class="boton boton--chico" data-editar="${r.id}">Editar</button>
                            <button class="boton boton--chico boton--peligro" data-eliminar="${r.id}">Eliminar</button></td>` : ""}
                    </tr>`).join("")}
                </tbody>
            </table></div>`;
    };

    if (esAdmin) {
        contenido.querySelector("#nuevo").onclick = () => formulario(actual, null, aux, cargar);
        lista.addEventListener("click", async (e) => {
            const editar = e.target.closest("[data-editar]");
            const eliminar = e.target.closest("[data-eliminar]");
            if (editar) return formulario(actual, registros.find((r) => r.id === Number(editar.dataset.editar)), aux, cargar);
            if (!eliminar) return;
            const registro = registros.find((r) => r.id === Number(eliminar.dataset.eliminar));
            if (!(await confirmar(`Eliminar ${actual.singular}`, `¿Eliminar "${registro.nombre}"? No se puede si está en uso (RN-12).`, "Eliminar"))) return;
            try {
                await api.eliminar(`${actual.ruta}/${registro.id}`);
                avisar(`Se eliminó "${registro.nombre}".`);
                await cargar();
            } catch (err) {
                avisarError(err);
            }
        });
    }

    return cargar();
}

function formulario(catalogo, registro, aux, recargar) {
    const nuevo = registro === null;
    const cuerpo = abrirModal(nuevo ? catalogo.nuevo : `Editar ${catalogo.singular}`, `
        <form class="formulario" id="form-catalogo" novalidate>
            ${catalogo.campos(registro, aux)}
            <div class="acciones-formulario">
                <button class="boton" type="button" data-cerrar>Cancelar</button>
                <button class="boton boton--primario" type="submit">${nuevo ? "Crear" : "Guardar"}</button>
            </div>
        </form>`);

    cuerpo.querySelector("[data-cerrar]").onclick = cerrarModal;
    const form = cuerpo.querySelector("#form-catalogo");
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        mientrasEnvia(form, async () => {
            try {
                const solicitud = catalogo.aSolicitud(leerFormulario(form));
                if (nuevo) await api.crear(catalogo.ruta, solicitud);
                else await api.actualizar(`${catalogo.ruta}/${registro.id}`, solicitud);
                cerrarModal();
                avisar(nuevo ? `Se creó "${solicitud.nombre}".` : "Cambios guardados.");
                await recargar();
            } catch (err) {
                avisarError(err); // la validación la hace el backend (400) y llega con su mensaje
            }
        });
    });
}
