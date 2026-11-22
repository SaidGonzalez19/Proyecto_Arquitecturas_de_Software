package com.equipo.gestorproductos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación Gestor de Productos.
 */
@SpringBootApplication
public class GestorProductosApplication {

	/**
	 * Inicia Spring y el servidor web integrado.
	 *
	 * @param args argumentos de ejecución
	 */
	public static void main(String[] args) {
		SpringApplication.run(GestorProductosApplication.class, args);
	}

}
