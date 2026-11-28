package com.equipo.gestorproductos.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BindingResult;

import com.equipo.gestorproductos.exception.ProductoExceptionHandler;
import com.equipo.gestorproductos.exception.ProductoNoEncontradoException;
import com.equipo.gestorproductos.model.Producto;
import com.equipo.gestorproductos.service.ProductoService;

/** Comprueba las rutas, el enlace de datos y los mensajes de presentación sin depender de MySQL. */
class ProductoControllerTest {

    private static final String ERROR_GENERAL = "No fue posible completar la operación. Inténtalo de nuevo";
    private ProductoService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(ProductoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProductoController(servicio))
                .setControllerAdvice(new ProductoExceptionHandler()).build();
    }

    @Test
    void inicioRedirigeYElListadoAceptaUnaColeccionVacia() throws Exception {
        when(servicio.listar()).thenReturn(List.of());
        mvc.perform(get("/")).andExpect(redirectedUrl("/productos"));
        mvc.perform(get("/productos"))
                .andExpect(status().isOk()).andExpect(view().name("productos/listado"))
                .andExpect(model().attribute("productos", List.of()));
    }

    @Test
    void crearAbreUnFormularioVacioYEditarPrecargaElProducto() throws Exception {
        MvcResult nuevo = mvc.perform(get("/productos/nuevo"))
                .andExpect(view().name("productos/formulario")).andExpect(model().attribute("modo", "crear"))
                .andReturn();
        Producto vacio = (Producto) nuevo.getModelAndView().getModel().get("producto");
        assertNull(vacio.getId());
        assertNull(vacio.getNombre());
        assertNull(vacio.getPrecio());

        Producto existente = new Producto();
        existente.setId(2L);
        existente.setNombre("Mouse óptico");
        existente.setPrecio(new BigDecimal("189.50"));
        when(servicio.obtenerPorId(2L)).thenReturn(existente);
        mvc.perform(get("/productos/2/editar"))
                .andExpect(view().name("productos/formulario")).andExpect(model().attribute("modo", "editar"))
                .andExpect(model().attribute("producto", existente));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "10.5", "10.50", "99999999.99"})
    void crearAceptaLosLimitesYLosPreciosConHastaDosDecimales(String precio) throws Exception {
        String nombre = "  " + "🍎".repeat(100) + "  ";
        mvc.perform(post("/productos").param("nombre", nombre).param("precio", precio).param("id", "90"))
                .andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attribute("mensajeExito", "Producto creado correctamente"));
        verify(servicio).crear(nombre, new BigDecimal(precio));
    }

    @Test
    void actualizarUsaElIdDeLaRuta() throws Exception {
        mvc.perform(post("/productos/2").param("id", "90").param("nombre", "Mouse").param("precio", "9.50"))
                .andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attribute("mensajeExito", "Producto actualizado correctamente"));
        verify(servicio).actualizar(2L, "Mouse", new BigDecimal("9.50"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"doce", "1,25", "1 2"})
    void unaConversionFallidaConservaElTextoElModoYElId(String precio) throws Exception {
        MvcResult respuesta = mvc.perform(post("/productos/2").param("id", "90")
                        .param("nombre", "Mouse").param("precio", precio))
                .andExpect(view().name("productos/formulario")).andExpect(model().attribute("modo", "editar"))
                .andExpect(model().attributeHasFieldErrorCode("producto", "precio", "typeMismatch"))
                .andReturn();
        var datos = respuesta.getModelAndView().getModel();
        var errores = (BindingResult) datos.get(BindingResult.MODEL_KEY_PREFIX + "producto");
        assertEquals(1, errores.getFieldErrorCount("precio"));
        assertEquals(precio, errores.getFieldError("precio").getRejectedValue());
        assertEquals(2L, ((Producto) datos.get("producto")).getId());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-0.01", "100000000", "12.345", "10.500"})
    void capturaInvalidaDevuelveAmbosErroresSinGuardar(String precio) throws Exception {
        mvc.perform(post("/productos").param("nombre", "  ").param("precio", precio))
                .andExpect(view().name("productos/formulario")).andExpect(model().attribute("modo", "crear"))
                .andExpect(model().attributeHasFieldErrors("producto", "nombre", "precio"));
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/productos", "/productos/2"})
    void unPrecioConMasDeDosDecimalesSeRechazaAunqueSeanCeros(String ruta) throws Exception {
        mvc.perform(post(ruta).param("nombre", "Mouse").param("precio", "10.500"))
                .andExpect(view().name("productos/formulario"))
                .andExpect(model().attributeHasFieldErrorCode("producto", "precio", "producto.precio.decimales"));
        verifyNoInteractions(servicio);
    }

    @Test
    void crearRechazaUnNombreMayorDeCienCaracteres() throws Exception {
        mvc.perform(post("/productos").param("nombre", "x".repeat(101)).param("precio", "1"))
                .andExpect(model().attributeHasFieldErrors("producto", "nombre"));
        verifyNoInteractions(servicio);
    }

    @Test
    void unaReglaDelServicioDevuelveElFormularioSinRedirigir() throws Exception {
        when(servicio.crear("A", BigDecimal.ONE)).thenThrow(new IllegalArgumentException("Dato inválido"));
        MvcResult respuesta = mvc.perform(post("/productos").param("nombre", "A").param("precio", "1"))
                .andExpect(view().name("productos/formulario")).andExpect(model().attribute("modo", "crear"))
                .andExpect(flash().attributeCount(0)).andReturn();
        assertEquals("Dato inválido", errorGlobal(respuesta));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/productos", "/productos/2"})
    void unFalloAlGuardarConservaElFormularioConUnMensajeGeneral(String ruta) throws Exception {
        var fallo = new CannotCreateTransactionException("detalle interno");
        when(servicio.crear("A", BigDecimal.ONE)).thenThrow(fallo);
        when(servicio.actualizar(2L, "A", BigDecimal.ONE)).thenThrow(fallo);
        boolean edicion = ruta.endsWith("/2");
        MvcResult respuesta = mvc.perform(post(ruta).param("nombre", "A").param("precio", "1"))
                .andExpect(view().name("productos/formulario"))
                .andExpect(model().attribute("modo", edicion ? "editar" : "crear"))
                .andExpect(flash().attributeCount(0)).andReturn();
        Producto captura = (Producto) respuesta.getModelAndView().getModel().get("producto");
        assertEquals(edicion ? Long.valueOf(2) : null, captura.getId());
        assertEquals("A", captura.getNombre());
        assertEquals(BigDecimal.ONE, captura.getPrecio());
        assertEquals(ERROR_GENERAL, errorGlobal(respuesta));
    }

    @Test
    void eliminacionesIndividualYMultipleDevuelvenSuMensaje() throws Exception {
        mvc.perform(post("/productos/2/eliminar")).andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attribute("mensajeExito", "Producto eliminado correctamente"));
        verify(servicio).eliminar(2L);
        mvc.perform(post("/productos/eliminar-seleccionados").param("ids", "3", "1"))
                .andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attribute("mensajeExito", "Productos seleccionados eliminados correctamente"));
        verify(servicio).eliminarSeleccionados(List.of(3L, 1L));
    }

    @Test
    void sinParametroIdsElServicioRecibeUnaListaVacia() throws Exception {
        doThrow(new IllegalArgumentException("Selecciona al menos un producto"))
                .when(servicio).eliminarSeleccionados(List.of());
        mvc.perform(post("/productos/eliminar-seleccionados")).andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attribute("mensajeError", "Selecciona al menos un producto"))
                .andExpect(flash().attributeCount(1));
        verify(servicio).eliminarSeleccionados(List.of());
    }

    @Test
    void productoInexistenteSeComunicaEnEdicionActualizacionYAmbosBorrados() throws Exception {
        var ausente = new ProductoNoEncontradoException(99L);
        when(servicio.obtenerPorId(99L)).thenThrow(ausente);
        when(servicio.actualizar(99L, "A", BigDecimal.ONE)).thenThrow(ausente);
        doThrow(ausente).when(servicio).eliminar(99L);
        doThrow(ausente).when(servicio).eliminarSeleccionados(List.of(1L, 99L));
        for (var peticion : List.of(get("/productos/99/editar"),
                post("/productos/99").param("nombre", "A").param("precio", "1"),
                post("/productos/99/eliminar"), post("/productos/eliminar-seleccionados").param("ids", "1", "99"))) {
            mvc.perform(peticion).andExpect(redirectedUrl("/productos"))
                    .andExpect(flash().attribute("mensajeError", "No se encontró el producto con ID 99"))
                    .andExpect(flash().attributeCount(1));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/productos/2/eliminar", "/productos/eliminar-seleccionados"})
    void falloAlEliminarRedirigeConErrorSinAnunciarExito(String ruta) throws Exception {
        var fallo = new DataAccessResourceFailureException("detalle interno");
        doThrow(fallo).when(servicio).eliminar(2L);
        doThrow(fallo).when(servicio).eliminarSeleccionados(List.of(2L));
        mvc.perform(post(ruta).param("ids", "2")).andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attributeCount(1)).andExpect(flash().attribute("mensajeError", ERROR_GENERAL));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/productos", "/productos/2/editar"})
    void falloDeConsultaDevuelveHtml503ConEnlaceAlListado(String ruta) throws Exception {
        when(servicio.listar()).thenThrow(new DataAccessResourceFailureException("detalle interno"));
        when(servicio.obtenerPorId(2L)).thenThrow(new CannotCreateTransactionException("detalle interno"));
        mvc.perform(get("/catalogo" + ruta).contextPath("/catalogo"))
                .andExpect(status().isServiceUnavailable()).andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("href=\"/catalogo/productos\"")))
                .andExpect(content().string(containsString(ERROR_GENERAL)))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }

    @Test
    void unIdNoNumericoSeRechazaAntesDelServicio() throws Exception {
        mvc.perform(get("/productos/abc/editar")).andExpect(status().isBadRequest());
        mvc.perform(post("/productos/eliminar-seleccionados").param("ids", "1", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    private String errorGlobal(MvcResult respuesta) {
        var datos = respuesta.getModelAndView().getModel();
        var errores = (BindingResult) datos.get(BindingResult.MODEL_KEY_PREFIX + "producto");
        return errores.getGlobalError().getDefaultMessage();
    }
}