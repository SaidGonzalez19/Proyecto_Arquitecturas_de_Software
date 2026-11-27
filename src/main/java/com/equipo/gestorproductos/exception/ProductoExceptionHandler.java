package com.equipo.gestorproductos.exception;

import com.equipo.gestorproductos.controller.ProductoController;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

/** Traduce los fallos de consulta y de producto inexistente en respuestas de la interfaz web. */
@ControllerAdvice(assignableTypes = ProductoController.class)
public class ProductoExceptionHandler {

    private static final String MENSAJE_ERROR_PERSISTENCIA =
            "No fue posible completar la operación. Inténtalo de nuevo";

    /** Crea el manejador que Spring aplica al controlador del catálogo. */
    public ProductoExceptionHandler() {
    }

    /**
     * @param excepcion error de negocio cuyo mensaje identifica el producto
     * @param atributosRedireccion recibe el mensaje que se mostrará en el listado
     * @return redirección al catálogo
     */
    @ExceptionHandler(ProductoNoEncontradoException.class)
    public String manejarProductoNoEncontrado(ProductoNoEncontradoException excepcion,
            RedirectAttributes atributosRedireccion) {
        atributosRedireccion.addFlashAttribute("mensajeError", excepcion.getMessage());
        return "redirect:/productos";
    }

    /**
     * @param excepcion fallo de acceso a datos o de transacción que impidió consultar
     * @return respuesta HTML 503 con un enlace para volver a intentar la consulta
     */
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<String> manejarErrorPersistencia(RuntimeException excepcion) {
        return construirRespuestaError();
    }

    /** Construye una página independiente de las plantillas y de la base de datos. */
    private ResponseEntity<String> construirRespuestaError() {
        String contexto = ServletUriComponentsBuilder.fromCurrentContextPath().build().getPath();
        String html = """
                <!doctype html>
                <html lang="es-MX">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Catálogo no disponible | Gestor de Productos</title>
                    <link rel="stylesheet" href="%1$s/css/estilos.css">
                </head>
                <body>
                    <nav class="barra" aria-label="Navegación principal">
                        <div class="contenedor navegacion">
                            <a class="marca" href="%1$s/productos">Gestor de Productos</a>
                            <a href="%1$s/productos">Productos</a>
                        </div>
                    </nav>
                    <main class="contenedor contenido">
                        <h1 class="solo-lectores">Catálogo no disponible</h1>
                        <p class="alerta error" role="alert">%2$s</p>
                        <a href="%1$s/productos">Volver al listado</a>
                    </main>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(contexto), MENSAJE_ERROR_PERSISTENCIA);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .body(html);
    }
}
