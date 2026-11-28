package com.equipo.gestorproductos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.equipo.gestorproductos.model.Producto;

/**
 * Acceso a la tabla {@code productos} mediante Spring Data JPA.
 * <p>
 * Spring genera la implementación en tiempo de ejecución. Por ahora no declara
 * métodos propios; usa las operaciones heredadas de {@link JpaRepository}:
 * <ul>
 * <li>{@code findAll(Sort)}: lista los productos con el orden indicado.</li>
 * <li>{@code findById(Long)}: devuelve un {@code Optional} vacío si no existe.</li>
 * <li>{@code findAllById(Iterable<Long>)}: puede devolver menos productos de
 * los solicitados; los inexistentes se omiten sin error.</li>
 * <li>{@code save(Producto)}: inserta si el ID es nulo; actualiza si ya tiene ID.</li>
 * <li>{@code delete(Producto)}: elimina la entidad indicada.</li>
 * <li>{@code deleteAll(Iterable<Producto>)}: elimina varias entidades dentro de
 * la transacción en curso.</li>
 * </ul>
 * Este repositorio solo accede a los datos: la validación y la interpretación
 * de los resultados ausentes corresponden a {@code ProductoService}.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
