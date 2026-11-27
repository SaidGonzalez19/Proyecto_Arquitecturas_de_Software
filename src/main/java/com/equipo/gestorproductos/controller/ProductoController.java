package com.equipo.gestorproductos.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.equipo.gestorproductos.model.Producto;
import com.equipo.gestorproductos.service.ProductoService;

/**
 * Presenta el catálogo y recibe los formularios de productos. Las escrituras
 * se delegan al servicio y solo se anuncia éxito cuando la operación termina.
 */
@Controller
public class ProductoController {

    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");
    private static final String MENSAJE_ERROR_PERSISTENCIA =
            "No fue posible completar la operación. Inténtalo de nuevo";
    private static final String VOLVER_AL_LISTADO = "redirect:/productos";

    private final ProductoService productoService;

    /**
     * @param productoService servicio que administra el catálogo
     */
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * Limita la captura a nombre y precio. Un precio sin valor queda pendiente
     * de validación y un texto no numérico queda registrado como error de enlace.
     *
     * @param binder enlazador del atributo de formulario {@code producto}
     */
    @InitBinder("producto")
    public void configurarFormulario(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "precio");
        binder.registerCustomEditor(BigDecimal.class, "precio", new CustomNumberEditor(BigDecimal.class, true) {
            @Override
            public void setAsText(String texto) {
                // BigDecimal exige punto decimal y no convierte «1 2» en «12».
                setValue(texto == null || texto.isBlank() ? null : new BigDecimal(texto.strip()));
            }
        });
    }

    /** @return redirección desde la raíz al listado */
    @GetMapping("/")
    public String inicio() {
        return VOLVER_AL_LISTADO;
    }

    /**
     * @param model recibe los productos ordenados por ID que entrega el servicio
     * @return vista del listado, también cuando el catálogo está vacío
     */
    @GetMapping("/productos")
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listar());
        return "productos/listado";
    }

    /**
     * @param model modelo del formulario
     * @return formulario de creación con un producto vacío
     */
    @GetMapping("/productos/nuevo")
    public String mostrarCreacion(Model model) {
        return prepararFormulario(new Producto(), "crear", model);
    }

    /**
     * Guarda un producto nuevo o vuelve a mostrar la captura si hay errores.
     *
     * @param producto datos enviados por el usuario
     * @param resultado errores de enlace y validación
     * @param model modelo del formulario
     * @param atributosRedireccion mensaje para la siguiente petición
     * @return formulario de creación o redirección al listado
     */
    @PostMapping("/productos")
    public String crear(@ModelAttribute("producto") Producto producto, BindingResult resultado,
                        Model model, RedirectAttributes atributosRedireccion) {
        validarFormulario(producto, resultado);
        if (!resultado.hasErrors()) {
            try {
                productoService.crear(producto.getNombre(), producto.getPrecio());
                atributosRedireccion.addFlashAttribute("mensajeExito", "Producto creado correctamente");
                return VOLVER_AL_LISTADO;
            } catch (IllegalArgumentException excepcion) {
                resultado.reject("producto.invalido", excepcion.getMessage());
            } catch (DataAccessException | TransactionException excepcion) {
                resultado.reject("producto.persistencia", MENSAJE_ERROR_PERSISTENCIA);
            }
        }
        return prepararFormulario(producto, "crear", model);
    }

    /**
     * @param id identificador solicitado en la ruta
     * @param model modelo que recibirá el producto existente
     * @param atributosRedireccion mensaje si el ID es inválido
     * @return formulario precargado o redirección al listado
     */
    @GetMapping("/productos/{id}/editar")
    public String mostrarEdicion(@PathVariable("id") Long id, Model model,
                                 RedirectAttributes atributosRedireccion) {
        try {
            return prepararFormulario(productoService.obtenerPorId(id), "editar", model);
        } catch (IllegalArgumentException excepcion) {
            atributosRedireccion.addFlashAttribute("mensajeError", excepcion.getMessage());
            return VOLVER_AL_LISTADO;
        }
    }

    /**
     * Actualiza el ID de la ruta. Si el guardado falla, conserva ese ID y la
     * captura para que el usuario pueda corregirla o volver a intentarlo.
     *
     * @param id identificador de la ruta
     * @param producto nuevos valores capturados
     * @param resultado errores de enlace y validación
     * @param model modelo del formulario
     * @param atributosRedireccion mensaje para la siguiente petición
     * @return formulario de edición o redirección al listado
     */
    @PostMapping("/productos/{id}")
    public String actualizar(@PathVariable("id") Long id, @ModelAttribute("producto") Producto producto,
                             BindingResult resultado, Model model, RedirectAttributes atributosRedireccion) {
        producto.setId(id);
        validarFormulario(producto, resultado);
        if (!resultado.hasErrors()) {
            try {
                productoService.actualizar(id, producto.getNombre(), producto.getPrecio());
                atributosRedireccion.addFlashAttribute("mensajeExito", "Producto actualizado correctamente");
                return VOLVER_AL_LISTADO;
            } catch (IllegalArgumentException excepcion) {
                resultado.reject("producto.invalido", excepcion.getMessage());
            } catch (DataAccessException | TransactionException excepcion) {
                resultado.reject("producto.persistencia", MENSAJE_ERROR_PERSISTENCIA);
            }
        }
        return prepararFormulario(producto, "editar", model);
    }

    /**
     * @param id identificador del producto cuya eliminación se confirmó
     * @param atributosRedireccion resultado de la operación
     * @return redirección al listado
     */
    @PostMapping("/productos/{id}/eliminar")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes atributosRedireccion) {
        try {
            productoService.eliminar(id);
            atributosRedireccion.addFlashAttribute("mensajeExito", "Producto eliminado correctamente");
        } catch (IllegalArgumentException excepcion) {
            atributosRedireccion.addFlashAttribute("mensajeError", excepcion.getMessage());
        } catch (DataAccessException | TransactionException excepcion) {
            atributosRedireccion.addFlashAttribute("mensajeError", MENSAJE_ERROR_PERSISTENCIA);
        }
        return VOLVER_AL_LISTADO;
    }

    /**
     * Envía la selección al servicio en una sola operación, incluso al seleccionar
     * todo el listado. El servicio comprueba los IDs y gestiona la transacción.
     *
     * @param ids valores repetidos del campo {@code ids}, o nulo si no se envió
     * @param atributosRedireccion resultado de la operación
     * @return redirección al listado
     */
    @PostMapping("/productos/eliminar-seleccionados")
    public String eliminarSeleccionados(@RequestParam(name = "ids", required = false) List<Long> ids,
                                        RedirectAttributes atributosRedireccion) {
        try {
            productoService.eliminarSeleccionados(ids == null ? List.of() : ids);
            atributosRedireccion.addFlashAttribute("mensajeExito", "Productos seleccionados eliminados correctamente");
        } catch (IllegalArgumentException excepcion) {
            atributosRedireccion.addFlashAttribute("mensajeError", excepcion.getMessage());
        } catch (DataAccessException | TransactionException excepcion) {
            atributosRedireccion.addFlashAttribute("mensajeError", MENSAJE_ERROR_PERSISTENCIA);
        }
        return VOLVER_AL_LISTADO;
    }

    /**
     * Valida el nombre recortado y el precio sin modificar la captura. La regla
     * de dos decimales se aplica a la escala del valor recibido, igual que en
     * el servicio: {@code 10.5} y {@code 10.50} se aceptan; {@code 10.500} y
     * {@code 12.345} se rechazan.
     */
    private void validarFormulario(Producto producto, BindingResult resultado) {
        String nombre = producto.getNombre() == null ? "" : producto.getNombre().strip();
        if (nombre.isEmpty() || nombre.codePointCount(0, nombre.length()) > 100) {
            resultado.rejectValue("nombre", "producto.nombre.longitud");
        }

        if (resultado.hasFieldErrors("precio")) {
            return;
        }
        BigDecimal precio = producto.getPrecio();
        if (precio == null) {
            resultado.rejectValue("precio", "producto.precio.obligatorio");
        } else if (precio.scale() > 2) {
            resultado.rejectValue("precio", "producto.precio.decimales");
        } else if (precio.signum() < 0 || precio.compareTo(PRECIO_MAXIMO) > 0) {
            resultado.rejectValue("precio", "producto.precio.rango");
        }
    }

    /** Reutiliza la instancia enlazada para conservar sus valores y su BindingResult. */
    private String prepararFormulario(Producto producto, String modo, Model model) {
        model.addAttribute("producto", producto);
        model.addAttribute("modo", modo);
        return "productos/formulario";
    }
}
