// Utilidades de interfaz: escapar texto, formatear dinero y fechas, avisos y ventana modal.

/** Escapa un valor para insertarlo en HTML (los datos vienen de la API, nunca se confía en ellos). */
export function esc(valor) {
    return String(valor ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

const pesos = new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });
export const dinero = (valor) => pesos.format(Number(valor ?? 0));

const formatoFecha = new Intl.DateTimeFormat("es-CO", { weekday: "short", day: "numeric", month: "short", year: "numeric" });
export const fecha = (iso) => formatoFecha.format(new Date(iso));
export const hora = (iso) => String(iso).slice(11, 16);
export const horario = (inicio, fin) => `${hora(inicio)} – ${hora(fin)}`;
export const horaCorta = (texto) => String(texto ?? "").slice(0, 5);

/** Fecha local en formato yyyy-MM-dd, desplazada `dias` desde hoy. */
export function fechaLocal(dias = 0) {
    const d = new Date();
    d.setDate(d.getDate() + dias);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export const mesActual = () => fechaLocal().slice(0, 7);

/** Horas en pasos de 30 minutos (RN-02), para los selectores de inicio y fin. */
export function opcionesDeHora(desde = 6, hasta = 22, seleccionada = "") {
    const opciones = [];
    for (let h = desde; h <= hasta; h++) {
        for (const m of ["00", "30"]) {
            if (h === hasta && m === "30") break;
            const valor = `${String(h).padStart(2, "0")}:${m}`;
            opciones.push(`<option value="${valor}" ${valor === seleccionada ? "selected" : ""}>${valor}</option>`);
        }
    }
    return opciones.join("");
}

export const NOMBRES = {
    PENDING: "Pendiente",
    CONFIRMED: "Confirmada",
    COMPLETED: "Completada",
    CANCELLED: "Cancelada",
    ACTIVE: "Activo",
    SUSPENDED: "Suspendido",
    ADMIN: "Administrador",
    RECEPTIONIST: "Recepción",
    MEMBER: "Miembro",
    MEETING_ROOM: "Sala de reuniones",
    PRIVATE_OFFICE: "Oficina privada",
    DESK: "Escritorio",
};

export const etiqueta = (valor) => `<span class="etiqueta etiqueta--${esc(valor)}">${esc(NOMBRES[valor] ?? valor)}</span>`;

export function opciones(lista, valorDe, textoDe, seleccionado, vacio) {
    const inicio = vacio === undefined ? "" : `<option value="">${esc(vacio)}</option>`;
    return inicio + lista
        .map((x) => `<option value="${esc(valorDe(x))}" ${String(valorDe(x)) === String(seleccionado ?? "") ? "selected" : ""}>${esc(textoDe(x))}</option>`)
        .join("");
}

// ---------------------------------------------------------------- Avisos

export function avisar(mensaje) {
    mostrarAviso(`<strong>${esc(mensaje)}</strong>`, "");
}

/** Muestra un error de la API con su código (por ejemplo, RN-01) o un error de red. */
export function avisarError(error) {
    const codigo = error.codigo ? `<code>${esc(error.codigo)}</code>` : "";
    const estado = error.status ? `HTTP ${error.status}` : "Error";
    mostrarAviso(`<strong>${esc(error.message)}</strong><span class="secundario">${estado}${codigo}</span>`, "aviso--error");
}

function mostrarAviso(html, clase) {
    const aviso = document.createElement("div");
    aviso.className = `aviso ${clase}`;
    aviso.innerHTML = html;
    const enModal = document.getElementById("modal").open;
    document.getElementById(enModal ? "avisos-modal" : "avisos").append(aviso);
    setTimeout(() => aviso.remove(), 5000);
}

// ---------------------------------------------------------------- Modal

const modal = () => document.getElementById("modal");

/** Abre la ventana modal con `html` y devuelve su cuerpo para enganchar eventos. */
export function abrirModal(titulo, html) {
    document.getElementById("modal-titulo").textContent = titulo;
    const cuerpo = document.getElementById("modal-cuerpo");
    cuerpo.innerHTML = html;
    if (!modal().open) modal().showModal();
    return cuerpo;
}

export function cerrarModal() {
    if (modal().open) modal().close();
}

/** Pide confirmación antes de una acción irreversible. */
export function confirmar(titulo, mensaje, textoBoton = "Confirmar") {
    return new Promise((resolver) => {
        const cuerpo = abrirModal(titulo, `
            <div class="bloque">
                <p>${esc(mensaje)}</p>
                <div class="acciones-formulario">
                    <button class="boton" data-respuesta="no">Volver</button>
                    <button class="boton boton--primario" data-respuesta="si">${esc(textoBoton)}</button>
                </div>
            </div>`);
        const responder = (valor) => { cerrarModal(); resolver(valor); };
        cuerpo.querySelector('[data-respuesta="si"]').onclick = () => responder(true);
        cuerpo.querySelector('[data-respuesta="no"]').onclick = () => responder(false);
        modal().addEventListener("close", () => resolver(false), { once: true });
    });
}

/** Lee un formulario como objeto. Los campos con data-tipo="numero" se convierten a número. */
export function leerFormulario(form) {
    const datos = {};
    for (const campo of form.elements) {
        if (!campo.name || campo.type === "submit") continue;
        if (campo.type === "checkbox") {
            if (campo.dataset.lista !== undefined) {
                datos[campo.name] ??= [];
                if (campo.checked) datos[campo.name].push(Number(campo.value));
            } else {
                datos[campo.name] = campo.checked;
            }
            continue;
        }
        const valor = campo.value.trim();
        datos[campo.name] = campo.dataset.tipo === "numero" ? (valor === "" ? null : Number(valor)) : valor;
    }
    return datos;
}

/** Deshabilita el botón de envío mientras corre `trabajo`, para no mandar la misma solicitud dos veces. */
export async function mientrasEnvia(form, trabajo) {
    const boton = form.querySelector('[type="submit"]');
    if (boton) boton.disabled = true;
    try {
        return await trabajo();
    } finally {
        if (boton) boton.disabled = false;
    }
}
