package com.equipo.gestorproductos;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class GestorProductosApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void iniciaConConexionAMySql() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
			assertEquals("gestor_productos", connection.getCatalog());
		}
	}

}
