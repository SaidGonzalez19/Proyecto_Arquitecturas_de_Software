package com.equipo.gestorproductos.exception;

/**
 * Indica que se intentó consultar, modificar o eliminar un producto que no
 * existe. La lanza únicamente {@code ProductoService}; el manejador web
 * ({@code ProductoExceptionHandler}) la convierte en una respuesta para el
 * usuario.
 * <p>
 * Es una excepción no verificada: al lanzarse dentro de una operación
 * transaccional provoca la reversión de la transacción.
 */
public class ProductoNoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje
	 * «No se encontró el producto con ID {id}».
	 *
	 * @param id identificador del producto que no se localizó
	 */
	public ProductoNoEncontradoException(Long id) {
		super("No se encontró el producto con ID " + id);
	}

}
