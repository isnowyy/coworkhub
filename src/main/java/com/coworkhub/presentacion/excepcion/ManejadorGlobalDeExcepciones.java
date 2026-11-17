package com.coworkhub.presentacion.excepcion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.coworkhub.negocio.excepcion.RecursoNoEncontradoException;
import com.coworkhub.negocio.excepcion.ReglaNegocioException;
import com.coworkhub.negocio.excepcion.SolicitudInvalidaException;
import com.coworkhub.presentacion.dto.RespuestaError;

/**
 * Único lugar donde las excepciones se traducen a códigos HTTP. Los servicios lanzan sus
 * propias excepciones y no saben nada de HTTP. Todas las respuestas usan RespuestaError, incluso
 * los errores de Spring MVC (JSON roto, ruta inexistente, método no permitido).
 */
@RestControllerAdvice
public class ManejadorGlobalDeExcepciones {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalDeExcepciones.class);

    // ---------------------------------------------------------------- 409: reglas de negocio

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaError> reglaDeNegocio(ReglaNegocioException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), e.getCodigo());
    }

    /** Un único que se coló entre la verificación y el guardado (dos altas a la vez). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> integridad(DataIntegrityViolationException e) {
        LOG.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "El dato ya existe o está en uso por otro registro", "RN-11");
    }

    /** Otra solicitud tenía bloqueada la sala demasiado tiempo. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<RespuestaError> bloqueo(PessimisticLockingFailureException e) {
        return responder(HttpStatus.CONFLICT, "La sala se está reservando en este momento; intenta de nuevo", "RN-01");
    }

    // ---------------------------------------------------------------- 404

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), "NO_ENCONTRADO");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespuestaError> rutaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "La ruta /" + e.getResourcePath() + " no existe", "RUTA_INEXISTENTE");
    }

    // ---------------------------------------------------------------- 400: datos faltantes o inválidos

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException e) {
        FieldError campo = e.getBindingResult().getFieldError();
        String mensaje = campo == null ? "Datos inválidos" : campo.getField() + ": " + campo.getDefaultMessage();
        return responder(HttpStatus.BAD_REQUEST, mensaje, "VALIDACION");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<RespuestaError> validacionDeParametros(HandlerMethodValidationException e) {
        String mensaje = e.getAllErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("Parámetros inválidos");
        return responder(HttpStatus.BAD_REQUEST, mensaje, "VALIDACION");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> jsonInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud falta, no es un JSON válido o tiene un valor con formato incorrecto",
                "JSON_INVALIDO");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespuestaError> parametroFaltante(MissingServletRequestParameterException e) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro '" + e.getParameterName() + "'",
                "PARAMETRO_INVALIDO");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> parametroConFormatoInvalido(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "El valor '" + e.getValue() + "' no es válido para '"
                + e.getName() + "'", "PARAMETRO_INVALIDO");
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<RespuestaError> solicitudInvalida(SolicitudInvalidaException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), "SOLICITUD_INVALIDA");
    }

    // ---------------------------------------------------------------- 405 y 415

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RespuestaError> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "El método " + e.getMethod() + " no está permitido en esta ruta",
                "METODO_NO_PERMITIDO");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RespuestaError> tipoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El cuerpo debe enviarse como application/json",
                "TIPO_NO_SOPORTADO");
    }

    // ---------------------------------------------------------------- 500: lo inesperado

    /** Nunca filtra detalles internos: el detalle queda solo en el log del servidor. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception e) {
        LOG.error("Error inesperado", e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", "ERROR_INTERNO");
    }

    private static ResponseEntity<RespuestaError> responder(HttpStatus estado, String mensaje, String codigo) {
        return ResponseEntity.status(estado).body(RespuestaError.de(mensaje, codigo));
    }
}
