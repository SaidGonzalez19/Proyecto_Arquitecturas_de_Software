package com.equipo.gestorproductos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equipo.gestorproductos.exception.ProductoNoEncontradoException;
import com.equipo.gestorproductos.model.Producto;
import com.equipo.gestorproductos.repository.ProductoRepository;

/**
 * Lógica de negocio del catálogo de productos.
 * <p>
 * Valida y normaliza el nombre y el precio, comprueba que los productos
 * existan, coordina las operaciones con {@link ProductoRepository} y define los
 * límites transaccionales. No conoce rutas, plantillas ni mensajes flash.
 * <p>
 * Toda la clase es transaccional; las consultas usan transacciones de solo
 * lectura. Las excepciones previstas son no verificadas, por lo que revierten
 * la transacción en la que se lanzan.
 */
@Service
@Transactional
public class ProductoService {

	/** Longitud máxima del nombre, tras recortarlo. */
	private static final int NOMBRE_MAX_LONGITUD = 100;

	/** Precio máximo permitido. El mínimo es cero. */
	private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

	/** Máximo de decimales significativos del precio. */
	private static final int PRECIO_ESCALA_MAXIMA = 2;

	private static final String MSG_ID_INVALIDO = "El ID del producto debe ser un número positivo";
	private static final String MSG_NOMBRE_OBLIGATORIO = "El nombre es obligatorio";
	private static final String MSG_NOMBRE_LARGO = "El nombre no puede exceder 100 caracteres";
	private static final String MSG_PRECIO_OBLIGATORIO = "El precio es obligatorio";
	private static final String MSG_PRECIO_RANGO = "El precio debe estar entre 0 y 99999999.99";
	private static final String MSG_PRECIO_DECIMALES = "El precio admite como máximo dos decimales";
	private static final String MSG_SELECCION_VACIA = "Selecciona al menos un producto";
	private static final String MSG_SELECCION_INVALIDA = "La selección contiene IDs no válidos";

	/** Acceso a los datos de los productos. */
	private final ProductoRepository productoRepository;

	/**
	 * Crea el servicio con su repositorio.
	 *
	 * @param productoRepository repositorio usado en todas las operaciones
	 */
	public ProductoService(ProductoRepository productoRepository) {
		this.productoRepository = productoRepository;
	}

	/**
	 * Devuelve todos los productos ordenados por ID ascendente.
	 *
	 * @return la lista de productos; vacía si no hay ninguno, nunca {@code null}
	 */
	@Transactional(readOnly = true)
	public List<Producto> listar() {
		return productoRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
	}

	/**
	 * Recupera un producto existente.
	 *
	 * @param id identificador del producto
	 * @return el producto con ese ID
	 * @throws IllegalArgumentException si el ID es nulo o no positivo
	 * @throws ProductoNoEncontradoException si no existe
	 */
	@Transactional(readOnly = true)
	public Producto obtenerPorId(Long id) {
		validarId(id);
		return buscarExistente(id);
	}

	/**
	 * Valida los datos, recorta el nombre y guarda un producto nuevo.
	 *
	 * @param nombre nombre capturado, con posibles espacios exteriores
	 * @param precio precio en MXN
	 * @return el producto guardado, con su ID generado
	 * @throws IllegalArgumentException si el nombre o el precio son inválidos
	 */
	public Producto crear(String nombre, BigDecimal precio) {
		String nombreLimpio = validarNombre(nombre);
		BigDecimal precioNormalizado = validarPrecio(precio);

		Producto producto = new Producto();
		producto.setNombre(nombreLimpio);
		producto.setPrecio(precioNormalizado);
		return productoRepository.save(producto);
	}

	/**
	 * Modifica un producto existente conservando su ID.
	 * <p>
	 * Se modifica la instancia ya cargada. Nunca se crea un {@code Producto}
	 * nuevo con ese ID: {@code save} podría fusionarlo e insertar un sustituto.
	 * Si el producto desaparece de forma concurrente, la actualización no afecta
	 * ninguna fila y la transacción se revierte sin insertar nada.
	 *
	 * @param id identificador del producto a modificar
	 * @param nombre nuevo nombre
	 * @param precio nuevo precio en MXN
	 * @return el producto actualizado, con el mismo ID
	 * @throws IllegalArgumentException si el ID, el nombre o el precio son inválidos
	 * @throws ProductoNoEncontradoException si el producto no existe
	 */
	public Producto actualizar(Long id, String nombre, BigDecimal precio) {
		validarId(id);
		Producto producto = buscarExistente(id);
		String nombreLimpio = validarNombre(nombre);
		BigDecimal precioNormalizado = validarPrecio(precio);

		producto.setNombre(nombreLimpio);
		producto.setPrecio(precioNormalizado);
		return productoRepository.save(producto);
	}

	/**
	 * Elimina un producto existente.
	 *
	 * @param id identificador del producto a eliminar
	 * @throws IllegalArgumentException si el ID es nulo o no positivo
	 * @throws ProductoNoEncontradoException si no existe
	 */
	public void eliminar(Long id) {
		validarId(id);
		Producto producto = buscarExistente(id);
		productoRepository.delete(producto);
	}

	/**
	 * Elimina todos los productos indicados o ninguno.
	 * <p>
	 * Se ejecuta en una sola transacción: se cargan los productos y se borran en
	 * ella. Si falta alguno, se lanza la excepción antes de borrar. Si algo
	 * falla después (por ejemplo, un cambio concurrente), la transacción se
	 * revierte completa. Cargar y borrar deben mantenerse en la misma
	 * transacción: con las entidades gestionadas, Hibernate detecta que un
	 * borrado no afectó ninguna fila y revierte todo.
	 *
	 * @param ids IDs seleccionados; los repetidos se procesan una sola vez
	 * @throws IllegalArgumentException si la lista es nula o vacía, o contiene
	 *         elementos nulos o no positivos
	 * @throws ProductoNoEncontradoException si falta alguno de los productos
	 *         (se indica el primero faltante en el orden recibido)
	 */
	public void eliminarSeleccionados(List<Long> ids) {
		Set<Long> idsUnicos = validarSeleccion(ids);

		List<Producto> encontrados = productoRepository.findAllById(idsUnicos);
		if (encontrados.size() < idsUnicos.size()) {
			Set<Long> existentes = encontrados.stream()
					.map(Producto::getId)
					.collect(Collectors.toSet());
			for (Long id : idsUnicos) {
				if (!existentes.contains(id)) {
					throw new ProductoNoEncontradoException(id);
				}
			}
		}

		productoRepository.deleteAll(encontrados);
	}

	/**
	 * Comprueba que el ID sea no nulo y positivo.
	 *
	 * @param id identificador a validar
	 * @throws IllegalArgumentException si es nulo o menor o igual que cero
	 */
	private void validarId(Long id) {
		if (id == null || id <= 0) {
			throw new IllegalArgumentException(MSG_ID_INVALIDO);
		}
	}

	/**
	 * Busca un producto e interpreta el resultado vacío del repositorio.
	 *
	 * @param id identificador del producto
	 * @return el producto existente
	 * @throws ProductoNoEncontradoException si no existe
	 */
	private Producto buscarExistente(Long id) {
		return productoRepository.findById(id)
				.orElseThrow(() -> new ProductoNoEncontradoException(id));
	}

	/**
	 * Valida el nombre y lo devuelve recortado. La longitud se cuenta en puntos
	 * de código, igual que {@code VARCHAR(100)} en MySQL con utf8mb4.
	 *
	 * @param nombre nombre capturado
	 * @return el nombre sin espacios exteriores
	 * @throws IllegalArgumentException si es nulo, queda vacío o excede 100 caracteres
	 */
	private String validarNombre(String nombre) {
		if (nombre == null) {
			throw new IllegalArgumentException(MSG_NOMBRE_OBLIGATORIO);
		}
		String recortado = nombre.strip();
		if (recortado.isEmpty()) {
			throw new IllegalArgumentException(MSG_NOMBRE_OBLIGATORIO);
		}
		if (recortado.codePointCount(0, recortado.length()) > NOMBRE_MAX_LONGITUD) {
			throw new IllegalArgumentException(MSG_NOMBRE_LARGO);
		}
		return recortado;
	}

	/**
	 * Valida el precio sin redondearlo y lo devuelve con escala 2.
	 * Admite ceros sobrantes ({@code 10.500}) pero no decimales significativos
	 * de más ({@code 10.505}).
	 *
	 * @param precio precio capturado
	 * @return el mismo valor con dos decimales
	 * @throws IllegalArgumentException si es nulo, está fuera de rango o tiene
	 *         más de dos decimales significativos
	 */
	private BigDecimal validarPrecio(BigDecimal precio) {
		if (precio == null) {
			throw new IllegalArgumentException(MSG_PRECIO_OBLIGATORIO);
		}
		if (precio.stripTrailingZeros().scale() > PRECIO_ESCALA_MAXIMA) {
			throw new IllegalArgumentException(MSG_PRECIO_DECIMALES);
		}
		if (precio.signum() < 0 || precio.compareTo(PRECIO_MAXIMO) > 0) {
			throw new IllegalArgumentException(MSG_PRECIO_RANGO);
		}
		return precio.setScale(PRECIO_ESCALA_MAXIMA, RoundingMode.UNNECESSARY);
	}

	/**
	 * Valida la selección y elimina repetidos conservando el orden recibido.
	 *
	 * @param ids IDs seleccionados
	 * @return los IDs sin repetidos
	 * @throws IllegalArgumentException si la lista es nula o vacía, o contiene
	 *         elementos nulos o no positivos
	 */
	private Set<Long> validarSeleccion(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			throw new IllegalArgumentException(MSG_SELECCION_VACIA);
		}
		Set<Long> idsUnicos = new LinkedHashSet<>();
		for (Long id : ids) {
			if (id == null || id <= 0) {
				throw new IllegalArgumentException(MSG_SELECCION_INVALIDA);
			}
			idsUnicos.add(id);
		}
		return idsUnicos;
	}

}
