// Miembros (RF-02 y RF-10), solo para ADMIN y RECEPTIONIST: registrar miembro y cuenta, editar,
// suspender, reactivar, eliminar y ver el consumo mensual.

import { api, consulta } from "../api.js";
import {
    abrirModal, avisar, avisarError, cerrarModal, confirmar, esc, etiqueta, leerFormulario, mesActual,
    mientrasEnvia, opciones,
} from "../ui.js";

export async function vistaMiembros(contenido) {
    const planes = await api.obtener("/api/planes");

    contenido.innerHTML = `
        <div class="cabecera-vista">
            <div><h1>Miembros</h1><p>Clientes del coworking, su plan y su cuenta de acceso.</p></div>
            <button class="boton boton--primario" id="nuevo-miembro">Registrar miembro</button>
        </div>
        <section class="tarjeta">
            <form id="form-filtro" class="formulario formulario--fila">
                <label class="campo"><span>Nombre</span><input name="nombre" placeholder="Parte del nombre"></label>
                <label class="campo"><span>Estado</span>
                    <select name="estado"><option value="">Todos</option><option value="ACTIVE">Activos</option><option value="SUSPENDED">Suspendidos</option></select>
                </label>
                <button class="boton" type="submit">Filtrar</button>
            </form>
        </section>
        <section class="tarjeta" id="lista"></section>`;

    const filtro = contenido.querySelector("#form-filtro");
    const lista = contenido.querySelector("#lista");
    let miembros = [];

    const cargar = async () => {
        miembros = await api.obtener(`/api/miembros${consulta(leerFormulario(filtro))}`);
        lista.innerHTML = tabla(miembros);
    };

    filtro.addEventListener("submit", (e) => { e.preventDefault(); cargar().catch(avisarError); });
    contenido.querySelector("#nuevo-miembro").onclick = () => formulario(null, planes, cargar);

    lista.addEventListener("click", async (e) => {
        const boton = e.target.closest("[data-accion]");
        if (!boton) return;
        const miembro = miembros.find((m) => m.id === Number(boton.dataset.id));
        try {
            switch (boton.dataset.accion) {
                case "editar": return formulario(miembro, planes, cargar);
                case "consumo": return verConsumo(miembro);
                case "suspender":
                    await api.parchear(`/api/miembros/${miembro.id}/suspender`);
                    avisar(`${miembro.nombreCompleto} quedó suspendido: no podrá reservar (RN-05).`);
                    return cargar();
                case "activar":
                    await api.parchear(`/api/miembros/${miembro.id}/activar`);
                    avisar(`${miembro.nombreCompleto} está activo de nuevo.`);
                    return cargar();
                case "eliminar":
                    if (!(await confirmar("Eliminar miembro", `¿Eliminar a ${miembro.nombreCompleto} y su cuenta? No se puede si tiene reservas (RN-12).`, "Eliminar"))) return;
                    await api.eliminar(`/api/miembros/${miembro.id}`);
                    avisar("Miembro eliminado.");
                    return cargar();
            }
        } catch (err) {
            avisarError(err);
        }
    });

    return cargar();
}

function tabla(miembros) {
    if (miembros.length === 0) return '<p class="vacio">No hay miembros con ese filtro.</p>';
    return `
        <div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>#</th><th>Nombre</th><th>Documento</th><th>Plan</th><th>Usuario</th><th>Estado</th><th></th></tr></thead>
            <tbody>${miembros.map((m) => `
                <tr>
                    <td>${m.id}</td>
                    <td>${esc(m.nombreCompleto)}<div class="secundario">${esc(m.email)}</div></td>
                    <td>${esc(m.documento)}</td>
                    <td>${esc(m.planNombre)}</td>
                    <td>${m.usuario ? esc(m.usuario) : '<span class="secundario">sin cuenta</span>'}</td>
                    <td>${etiqueta(m.estado)}</td>
                    <td class="acciones">
                        <button class="boton boton--chico" data-accion="consumo" data-id="${m.id}">Consumo</button>
                        <button class="boton boton--chico" data-accion="editar" data-id="${m.id}">Editar</button>
                        ${m.estado === "ACTIVE"
                            ? `<button class="boton boton--chico" data-accion="suspender" data-id="${m.id}">Suspender</button>`
                            : `<button class="boton boton--chico" data-accion="activar" data-id="${m.id}">Activar</button>`}
                        <button class="boton boton--chico boton--peligro" data-accion="eliminar" data-id="${m.id}">Eliminar</button>
                    </td>
                </tr>`).join("")}
            </tbody>
        </table></div>`;
}

/** Registrar (miembro = null) o editar nombre, email y plan. */
function formulario(miembro, planes, recargar) {
    const nuevo = miembro === null;
    const cuerpo = abrirModal(nuevo ? "Registrar miembro" : `Editar a ${miembro.nombreCompleto}`, `
        <form class="formulario" id="form-miembro">
            ${nuevo ? '<label class="campo"><span>Documento</span><input name="documento" required maxlength="20"></label>' : ""}
            <label class="campo"><span>Nombre completo</span><input name="nombreCompleto" required maxlength="120" value="${esc(miembro?.nombreCompleto)}"></label>
            <label class="campo"><span>Email</span><input name="email" type="email" required maxlength="120" value="${esc(miembro?.email)}"></label>
            <label class="campo"><span>Plan</span><select name="planId" data-tipo="numero">${opciones(planes, (p) => p.id, (p) => `${p.nombre} · ${p.horasIncluidasMes} h/mes`, miembro?.planId)}</select></label>
            ${nuevo ? `
            <fieldset class="formulario" style="border:1px solid var(--borde);border-radius:8px;padding:1rem">
                <legend class="secundario">&nbsp;Cuenta para iniciar sesión (rol MEMBER)&nbsp;</legend>
                <label class="campo"><span>Usuario</span><input name="usuario" required maxlength="50" autocomplete="off"></label>
                <label class="campo"><span>Contraseña</span><input name="contrasena" type="password" required minlength="6" autocomplete="new-password"></label>
            </fieldset>` : ""}
            <div class="acciones-formulario">
                <button class="boton" type="button" data-cerrar>Cancelar</button>
                <button class="boton boton--primario" type="submit">${nuevo ? "Registrar" : "Guardar"}</button>
            </div>
        </form>`);

    cuerpo.querySelector("[data-cerrar]").onclick = cerrarModal;
    const form = cuerpo.querySelector("#form-miembro");
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        mientrasEnvia(form, async () => {
            try {
                const datos = leerFormulario(form);
                if (nuevo) await api.crear("/api/miembros", datos);
                else await api.actualizar(`/api/miembros/${miembro.id}`, datos);
                cerrarModal();
                avisar(nuevo ? "Miembro y cuenta registrados." : "Cambios guardados.");
                await recargar();
            } catch (err) {
                avisarError(err);
            }
        });
    });
}

async function verConsumo(miembro) {
    const mostrar = async (mes) => {
        try {
            const c = await api.obtener(`/api/miembros/${miembro.id}/consumo${consulta({ mes })}`);
            const cuerpo = abrirModal(`Consumo de ${miembro.nombreCompleto}`, `
                <div class="bloque">
                    <label class="campo"><span>Mes</span><input type="month" id="mes" value="${esc(c.mes)}"></label>
                    <dl class="datos">
                        <dt>Plan</dt><dd>${esc(miembro.planNombre)}</dd>
                        <dt>Horas incluidas</dt><dd>${esc(c.horasIncluidas)}</dd>
                        <dt>Horas usadas</dt><dd>${esc(c.horasUsadas)}</dd>
                        <dt>Horas restantes</dt><dd>${esc(c.horasRestantes)}</dd>
                        <dt>Horas excedentes</dt><dd>${esc(c.horasExcedentes)}</dd>
                    </dl>
                </div>`);
            cuerpo.querySelector("#mes").onchange = (e) => mostrar(e.target.value);
        } catch (err) {
            avisarError(err);
        }
    };
    return mostrar(mesActual());
}
