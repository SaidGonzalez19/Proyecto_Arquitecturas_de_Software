package com.equipo.gestorproductos.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Producto del catálogo. Cada instancia corresponde a una fila de la tabla
 * {@code productos} de la base {@code gestor_productos}.
 * <p>
 * Es un objeto de datos: no valida ni recorta valores. Las reglas del nombre
 * y del precio las aplica {@code ProductoService}. Los precios están en pesos
 * mexicanos (MXN) y se permiten nombres repetidos.
 */
@Entity
@Table(name = "productos")
public class Producto {

	/** Identificador generado por MySQL; es {@code null} hasta guardar el producto. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Nombre del producto, de 1 a 100 caracteres (ya recortado por el servicio). */
	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	/** Precio en MXN, entre 0 y 99999999.99 con dos decimales. */
	@Column(name = "precio", nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	/**
	 * Crea un producto vacío, con todos sus atributos en {@code null}. Lo exige JPA.
	 */
	public Producto() {
	}

	/**
	 * Devuelve el identificador del producto.
	 *
	 * @return el ID, o {@code null} si el producto aún no se ha guardado
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Asigna el identificador. El usuario no puede capturarlo ni modificarlo;
	 * el servicio nunca lo asigna a partir de datos del formulario.
	 *
	 * @param id identificador del producto
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Devuelve el nombre del producto.
	 *
	 * @return el nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna el nombre sin validarlo ni recortarlo.
	 *
	 * @param nombre nombre del producto
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Devuelve el precio del producto.
	 *
	 * @return el precio en MXN
	 */
	public BigDecimal getPrecio() {
		return precio;
	}

	/**
	 * Asigna el precio sin validarlo ni redondearlo.
	 *
	 * @param precio precio en MXN
	 */
	public void setPrecio(BigDecimal precio) {
		this.precio = precio;
	}

}
