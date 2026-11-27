package com.equipo.gestorproductos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.equipo.gestorproductos.exception.ProductoNoEncontradoException;
import com.equipo.gestorproductos.model.Producto;
import com.equipo.gestorproductos.repository.ProductoRepository;

/**
 * Pruebas unitarias de {@link ProductoService}, con el repositorio simulado.
 * No requieren base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

	@Mock
	private ProductoRepository repositorio;

	@InjectMocks
	private ProductoService servicio;

	private Producto producto(Long id, String nombre, String precio) {
		Producto p = new Producto();
		p.setId(id);
		p.setNombre(nombre);
		p.setPrecio(new BigDecimal(precio));
		return p;
	}

	// ---- listar ----

	@Test
	void listarPideOrdenAscendentePorId() {
		when(repositorio.findAll(Sort.by(Sort.Direction.ASC, "id"))).thenReturn(List.of());

		List<Producto> resultado = servicio.listar();

		assertNotNull(resultado);
		assertTrue(resultado.isEmpty());
	}

	// ---- obtenerPorId ----

	@Test
	void obtenerPorIdDevuelveElProducto() {
		Producto existente = producto(1L, "Lápiz", "5.00");
		when(repositorio.findById(1L)).thenReturn(Optional.of(existente));

		assertEquals(existente, servicio.obtenerPorId(1L));
	}

	@Test
	void obtenerPorIdInexistenteLanzaExcepcionConMensaje() {
		when(repositorio.findById(9L)).thenReturn(Optional.empty());

		ProductoNoEncontradoException e = assertThrows(ProductoNoEncontradoException.class,
				() -> servicio.obtenerPorId(9L));

		assertEquals("No se encontró el producto con ID 9", e.getMessage());
	}

	@Test
	void obtenerPorIdRechazaIdsInvalidos() {
		assertThrows(IllegalArgumentException.class, () -> servicio.obtenerPorId(null));
		assertThrows(IllegalArgumentException.class, () -> servicio.obtenerPorId(0L));
		assertThrows(IllegalArgumentException.class, () -> servicio.obtenerPorId(-3L));
		verify(repositorio, never()).findById(any());
	}

	// ---- crear ----

	@Test
	void crearRecortaElNombreYNormalizaElPrecio() {
		when(repositorio.save(any(Producto.class))).thenAnswer(invocacion -> {
			Producto p = invocacion.getArgument(0);
			p.setId(1L);
			return p;
		});

		Producto creado = servicio.crear("  Cuaderno  ", new BigDecimal("10.500"));

		assertEquals(1L, creado.getId());
		assertEquals("Cuaderno", creado.getNombre());
		assertEquals(new BigDecimal("10.50"), creado.getPrecio());
	}

	@Test
	void crearAceptaLosLimitesDelPrecio() {
		when(repositorio.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		assertEquals(new BigDecimal("0.00"), servicio.crear("A", BigDecimal.ZERO).getPrecio());
		assertEquals(new BigDecimal("99999999.99"),
				servicio.crear("A", new BigDecimal("99999999.99")).getPrecio());
	}

	@Test
	void crearRechazaNombresInvalidosSinGuardar() {
		assertThrows(IllegalArgumentException.class, () -> servicio.crear(null, BigDecimal.ONE));
		assertThrows(IllegalArgumentException.class, () -> servicio.crear("   ", BigDecimal.ONE));
		assertThrows(IllegalArgumentException.class, () -> servicio.crear("x".repeat(101), BigDecimal.ONE));
		verify(repositorio, never()).save(any());
	}

	@Test
	void crearAceptaNombreDe100Caracteres() {
		when(repositorio.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		Producto creado = servicio.crear("x".repeat(100), BigDecimal.ONE);

		assertEquals(100, creado.getNombre().length());
	}

	@Test
	void crearRechazaPreciosInvalidosSinRedondearNiGuardar() {
		assertThrows(IllegalArgumentException.class, () -> servicio.crear("A", null));
		assertThrows(IllegalArgumentException.class, () -> servicio.crear("A", new BigDecimal("-0.01")));
		assertThrows(IllegalArgumentException.class, () -> servicio.crear("A", new BigDecimal("100000000")));
		assertThrows(IllegalArgumentException.class, () -> servicio.crear("A", new BigDecimal("10.505")));
		verify(repositorio, never()).save(any());
	}

	// ---- actualizar ----

	@Test
	void actualizarConservaElIdYModificaLaInstanciaExistente() {
		Producto existente = producto(4L, "Viejo", "1.00");
		when(repositorio.findById(4L)).thenReturn(Optional.of(existente));
		when(repositorio.save(existente)).thenReturn(existente);

		Producto actualizado = servicio.actualizar(4L, " Nuevo ", new BigDecimal("2.5"));

		assertEquals(4L, actualizado.getId());
		assertEquals("Nuevo", actualizado.getNombre());
		assertEquals(new BigDecimal("2.50"), actualizado.getPrecio());
	}

	@Test
	void actualizarInexistenteNoGuardaNada() {
		when(repositorio.findById(7L)).thenReturn(Optional.empty());

		assertThrows(ProductoNoEncontradoException.class,
				() -> servicio.actualizar(7L, "A", BigDecimal.ONE));
		verify(repositorio, never()).save(any());
	}

	@Test
	void actualizarConDatosInvalidosNoModificaNiGuarda() {
		Producto existente = producto(4L, "Viejo", "1.00");
		when(repositorio.findById(4L)).thenReturn(Optional.of(existente));

		assertThrows(IllegalArgumentException.class,
				() -> servicio.actualizar(4L, "", BigDecimal.ONE));

		assertEquals("Viejo", existente.getNombre());
		verify(repositorio, never()).save(any());
	}

	// ---- eliminar ----

	@Test
	void eliminarBorraElProductoExistente() {
		Producto existente = producto(2L, "A", "1.00");
		when(repositorio.findById(2L)).thenReturn(Optional.of(existente));

		servicio.eliminar(2L);

		verify(repositorio).delete(existente);
	}

	@Test
	void eliminarInexistenteLanzaExcepcionSinBorrar() {
		when(repositorio.findById(2L)).thenReturn(Optional.empty());

		assertThrows(ProductoNoEncontradoException.class, () -> servicio.eliminar(2L));
		verify(repositorio, never()).delete(any(Producto.class));
	}

	// ---- eliminarSeleccionados ----

	@Test
	void eliminarSeleccionadosRechazaSeleccionNulaOVacia() {
		IllegalArgumentException vacia = assertThrows(IllegalArgumentException.class,
				() -> servicio.eliminarSeleccionados(List.of()));
		IllegalArgumentException nula = assertThrows(IllegalArgumentException.class,
				() -> servicio.eliminarSeleccionados(null));

		assertEquals("Selecciona al menos un producto", vacia.getMessage());
		assertEquals("Selecciona al menos un producto", nula.getMessage());
		verify(repositorio, never()).deleteAll(any());
	}

	@Test
	void eliminarSeleccionadosRechazaElementosInvalidos() {
		assertThrows(IllegalArgumentException.class,
				() -> servicio.eliminarSeleccionados(java.util.Arrays.asList(1L, null)));
		assertThrows(IllegalArgumentException.class,
				() -> servicio.eliminarSeleccionados(List.of(1L, 0L)));
		verify(repositorio, never()).findAllById(any());
	}

	@Test
	void eliminarSeleccionadosProcesaLosRepetidosUnaSolaVez() {
		Producto uno = producto(1L, "A", "1.00");
		Producto dos = producto(2L, "B", "2.00");
		when(repositorio.findAllById(Set.of(1L, 2L))).thenReturn(List.of(uno, dos));

		servicio.eliminarSeleccionados(List.of(1L, 2L, 1L));

		verify(repositorio).deleteAll(List.of(uno, dos));
	}

	@Test
	void eliminarSeleccionadosConUnIdFaltanteRechazaTodoSinBorrar() {
		Producto uno = producto(1L, "A", "1.00");
		when(repositorio.findAllById(Set.of(1L, 2L))).thenReturn(List.of(uno));

		ProductoNoEncontradoException e = assertThrows(ProductoNoEncontradoException.class,
				() -> servicio.eliminarSeleccionados(List.of(1L, 2L)));

		assertEquals("No se encontró el producto con ID 2", e.getMessage());
		verify(repositorio, never()).deleteAll(any());
	}

}
