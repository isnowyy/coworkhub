// Cliente de la API REST de CoWorkHub. Guarda la sesión (JWT) y traduce las respuestas de error
// al cuerpo estándar del backend: {"error", "codigo", "timestamp"}.

const CLAVE_SESION = "coworkhub.sesion";

/** Error que devolvió la API, con el código HTTP y el código de la regla (RN-01, NO_ENCONTRADO…). */
export class ErrorApi extends Error {
    constructor(status, cuerpo) {
        super(cuerpo?.error ?? `La solicitud falló (${status})`);
        this.status = status;
        this.codigo = cuerpo?.codigo ?? "";
    }
}

let alExpirar = () => {};

/** Función que se llama cuando la API responde 401 (token vencido o alterado). */
export function cuandoExpire(funcion) {
    alExpirar = funcion;
}

export function sesionActual() {
    try {
        const sesion = JSON.parse(sessionStorage.getItem(CLAVE_SESION));
        if (!sesion || new Date(sesion.expiraEn) <= new Date()) return null;
        return sesion;
    } catch {
        return null;
    }
}

export function cerrarSesion() {
    sessionStorage.removeItem(CLAVE_SESION);
}

/** POST /auth/login. Guarda el token, el usuario, el rol y el miembro (si lo tiene). */
export async function iniciarSesion(usuario, contrasena) {
    const respuesta = await solicitar("POST", "/auth/login", { usuario, contrasena }, false);
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(respuesta));
    return respuesta;
}

async function solicitar(metodo, ruta, cuerpo, conToken = true) {
    const encabezados = { Accept: "application/json" };
    if (cuerpo !== undefined) encabezados["Content-Type"] = "application/json";
    const sesion = sesionActual();
    if (conToken && sesion) encabezados.Authorization = `Bearer ${sesion.token}`;

    const respuesta = await fetch(ruta, {
        method: metodo,
        headers: encabezados,
        body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    });

    const texto = await respuesta.text();
    const datos = texto ? JSON.parse(texto) : null;

    if (!respuesta.ok) {
        const error = new ErrorApi(respuesta.status, datos);
        if (respuesta.status === 401 && conToken) alExpirar(error);
        throw error;
    }
    return datos;
}

/** Arma "?a=1&b=2" omitiendo los valores vacíos; las listas se repiten (equipamientoIds=1&equipamientoIds=2). */
export function consulta(parametros) {
    const p = new URLSearchParams();
    for (const [clave, valor] of Object.entries(parametros)) {
        if (valor === undefined || valor === null || valor === "") continue;
        if (Array.isArray(valor)) valor.forEach((v) => p.append(clave, v));
        else p.append(clave, valor);
    }
    const texto = p.toString();
    return texto ? `?${texto}` : "";
}

export const api = {
    obtener: (ruta) => solicitar("GET", ruta),
    crear: (ruta, cuerpo) => solicitar("POST", ruta, cuerpo),
    accion: (ruta) => solicitar("POST", ruta),
    actualizar: (ruta, cuerpo) => solicitar("PUT", ruta, cuerpo),
    parchear: (ruta) => solicitar("PATCH", ruta),
    eliminar: (ruta) => solicitar("DELETE", ruta),
};
