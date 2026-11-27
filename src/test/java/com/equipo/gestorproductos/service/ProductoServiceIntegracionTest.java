package com.equipo.gestorproductos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.equipo.gestorproductos.exception.ProductoNoEncontradoException;
import com.equipo.gestorproductos.model.Producto;
import com.equipo.gestorproductos.repository.ProductoRepository;

/**
 * Pruebas de integración de {@link ProductoService} contra MySQL.
 * <p>
 * Requieren MySQL en ejecución, la tabla {@code productos} creada y la
 * conexión configurada, igual que {@code GestorProductosApplicationTests}.
 * <p>
 * La prueba no usa {@code @Transactional}: cada operación se confirma de
 * verdad. Solo toca los productos que ella misma crea (nombre con el prefijo
 * {@code IT-reversion-}) y los elimina al terminar, sin afectar otros datos
 * de la base.
 */
@SpringBootTest
class ProductoServiceIntegracionTest {

	@Autowired
	private ProductoService servicio;

	/** Repositorio real, vigilado para poder intervenir justo antes del borrado. */
	@MockitoSpyBean
	private ProductoRepository repositorio;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private final List<Long> idsCreados = new ArrayList<>();

	@AfterEach
	void limpiar() {
		Mockito.reset(repositorio);
		repositorio.deleteAllById(idsCreados);
		idsCreados.clear();
	}

	private Long crearProducto(String sufijo) {
		Producto producto = servicio.crear("IT-reversion-" + sufijo, new BigDecimal("1.00"));
		idsCreados.add(producto.getId());
		return producto.getId();
	}

	/**
	 * Provoca un fallo después de que la transacción ya ejecutó borrados.
	 * <p>
	 * El servicio carga los productos seleccionados y llama a
	 * {@code deleteAll}. Justo antes de que el borrado real se ejecute, otra
	 * transacción independiente elimina y confirma el último producto de la
	 * lista. Al confirmarse la transacción del servicio, Hibernate envía los
	 * {@code DELETE} en el orden de la lista: los primeros se ejecutan y el
	 * último no afecta ninguna fila, lo que provoca el fallo y la reversión.
	 * Después se consulta la base desde otra transacción.
	 */
	@Test
	void eliminarSeleccionadosRevierteLosBorradosPreviosSiFallaDespues() {
		Long id1 = crearProducto("1");
		Long id2 = crearProducto("2");
		Long id3 = crearProducto("3");
		Long idFueraDeLaSeleccion = crearProducto("fuera");

		AtomicReference<Long> idBorradoPorOtraTransaccion = new AtomicReference<>();
		TransactionTemplate otraTransaccion = new TransactionTemplate(transactionManager);
		otraTransaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		doAnswer(invocacion -> {
			Iterable<Producto> entidades = invocacion.getArgument(0);
			Long ultimo = null;
			for (Iterator<Producto> it = entidades.iterator(); it.hasNext();) {
				ultimo = it.next().getId();
			}
			final Long idConcurrente = ultimo;
			idBorradoPorOtraTransaccion.set(idConcurrente);
			otraTransaccion.executeWithoutResult(estado -> repositorio.deleteById(idConcurrente));
			return invocacion.callRealMethod();
		}).when(repositorio).deleteAll(any());

		RuntimeException error = assertThrows(RuntimeException.class,
				() -> servicio.eliminarSeleccionados(List.of(id1, id2, id3)));

		// El fallo debe ser técnico (cambio concurrente), no una excepción de negocio.
		// Normalmente es ObjectOptimisticLockingFailureException.
		assertFalse(error instanceof ProductoNoEncontradoException
				|| error instanceof IllegalArgumentException,
				"Se esperaba un fallo técnico de persistencia, pero fue: " + error);

		Long idConcurrente = idBorradoPorOtraTransaccion.get();
		assertNotNull(idConcurrente, "El borrado concurrente no se ejecutó");

		// Consulta desde otra transacción, ya terminada la operación.
		Set<Long> presentes = new TransactionTemplate(transactionManager).execute(estado ->
				repositorio.findAllById(List.of(id1, id2, id3, idFueraDeLaSeleccion)).stream()
						.map(Producto::getId)
						.collect(Collectors.toSet()));

		// Sigue en la tabla todo lo que no borró la otra transacción: los borrados
		// previos del servicio se revirtieron y el producto ajeno no se tocó.
		Set<Long> esperados = new HashSet<>(List.of(id1, id2, id3, idFueraDeLaSeleccion));
		esperados.remove(idConcurrente);
		assertEquals(esperados, presentes);
	}

}
