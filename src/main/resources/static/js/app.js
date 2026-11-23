// Punto de entrada del frontend: inicio de sesión, menú según el rol y navegación por #rutas.

import { cerrarSesion, cuandoExpire, iniciarSesion, sesionActual } from "./api.js";
import { avisar, avisarError, cerrarModal, esc, etiqueta } from "./ui.js";
import { vistaInicio } from "./vistas/inicio.js";
import { vistaDisponibilidad } from "./vistas/disponibilidad.js";
import { vistaNuevaReserva, vistaReservas } from "./vistas/reservas.js";
import { vistaMiembros } from "./vistas/miembros.js";
import { vistaCatalogos } from "./vistas/catalogos.js";

const PERSONAL = ["ADMIN", "RECEPTIONIST"];

/** Secciones del menú. `roles` vacío = cualquier usuario con sesión (RN-13 lo aplica el backend). */
const SECCIONES = [
    { ruta: "inicio", nombre: "Inicio", icono: "⌂", roles: [], vista: vistaInicio },
    { ruta: "disponibilidad", nombre: "Disponibilidad", icono: "◷", roles: [], vista: vistaDisponibilidad },
    { ruta: "nueva-reserva", nombre: "Nueva reserva", icono: "+", roles: [], vista: vistaNuevaReserva },
    { ruta: "reservas", nombre: "Reservas", icono: "≡", roles: [], vista: vistaReservas },
    { ruta: "miembros", nombre: "Miembros", icono: "☺", roles: PERSONAL, vista: vistaMiembros },
    { ruta: "catalogos", nombre: "Catálogos", icono: "▦", roles: [], vista: vistaCatalogos },
];

const USUARIOS_DEMO = [
    { usuario: "admin", contrasena: "admin123", rol: "ADMIN" },
    { usuario: "recepcion", contrasena: "recep123", rol: "RECEPTIONIST" },
    { usuario: "ana", contrasena: "ana123", rol: "MEMBER" },
    { usuario: "luis", contrasena: "luis123", rol: "MEMBER" },
];

const $ = (id) => document.getElementById(id);

function seccionesPermitidas(sesion) {
    return SECCIONES.filter((s) => s.roles.length === 0 || s.roles.includes(sesion.rol));
}

// ---------------------------------------------------------------- Inicio de sesión

function mostrarLogin() {
    cerrarModal();
    $("pantalla-app").hidden = true;
    $("pantalla-login").hidden = false;
    $("usuarios-demo").innerHTML = USUARIOS_DEMO
        .map((u) => `<button type="button" class="chip" data-usuario="${u.usuario}">${u.usuario} · ${u.rol}</button>`)
        .join("");
}

$("usuarios-demo").addEventListener("click", (e) => {
    const chip = e.target.closest("[data-usuario]");
    if (!chip) return;
    const demo = USUARIOS_DEMO.find((u) => u.usuario === chip.dataset.usuario);
    const form = $("form-login");
    form.usuario.value = demo.usuario;
    form.contrasena.value = demo.contrasena;
    form.requestSubmit();
});

$("form-login").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    const error = $("error-login");
    error.hidden = true;
    try {
        await iniciarSesion(form.usuario.value.trim(), form.contrasena.value);
        form.reset();
        location.hash = "#inicio";
        mostrarApp();
    } catch (err) {
        error.textContent = err.status === 401 ? "Usuario o contraseña incorrectos." : err.message;
        error.hidden = false;
    }
});

$("boton-salir").addEventListener("click", () => {
    cerrarSesion();
    location.hash = "";
    mostrarLogin();
});

cuandoExpire(() => {
    cerrarSesion();
    mostrarLogin();
    avisar("Tu sesión venció. Vuelve a iniciar sesión.");
});

// ---------------------------------------------------------------- Aplicación

function mostrarApp() {
    const sesion = sesionActual();
    if (!sesion) return mostrarLogin();
    $("pantalla-login").hidden = true;
    $("pantalla-app").hidden = false;
    $("usuario-actual").innerHTML = `${esc(sesion.usuario)} ${etiqueta(sesion.rol)}`;
    navegar();
}

function navegar() {
    const sesion = sesionActual();
    if (!sesion) return mostrarLogin();

    const [ruta, ...resto] = location.hash.replace(/^#/, "").split("?");
    const parametros = new URLSearchParams(resto.join("?"));
    const permitidas = seccionesPermitidas(sesion);
    const seccion = permitidas.find((s) => s.ruta === ruta) ?? permitidas[0];

    $("menu").innerHTML = permitidas
        .map((s) => `<a class="menu__item" href="#${s.ruta}" ${s === seccion ? 'aria-current="page"' : ""}>
                        <span class="menu__icono" aria-hidden="true">${s.icono}</span>${s.nombre}</a>`)
        .join("");
    $("menu").classList.remove("menu--abierto");
    $("boton-menu").setAttribute("aria-expanded", "false");

    const contenido = $("contenido");
    contenido.innerHTML = '<p class="cargando">Cargando…</p>';
    seccion.vista(contenido, sesion, parametros).catch((err) => {
        if (err.status === 401) return; // cuandoExpire ya volvió al inicio de sesión
        contenido.innerHTML = `<div class="tarjeta"><p class="error-formulario">${esc(err.message)}</p></div>`;
        avisarError(err);
    });
    contenido.focus({ preventScroll: true });
}

$("boton-menu").addEventListener("click", () => {
    const abierto = $("menu").classList.toggle("menu--abierto");
    $("boton-menu").setAttribute("aria-expanded", String(abierto));
});

window.addEventListener("hashchange", navegar);

if (sesionActual()) mostrarApp();
else mostrarLogin();
