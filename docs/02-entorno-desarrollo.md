# Entorno de desarrollo

## 1. Versiones y proyecto

| Elemento | Versión acordada |
| --- | --- |
| Java | JDK 17; comprobación local con Temurin 17.0.19 |
| Spring Boot | 4.2.0 |
| Maven | 3.9.16, mediante Maven Wrapper |
| MySQL | 8.4.13, rama 8.4 LTS |

El proyecto se generó con Spring Initializr, usando Maven, Java y empaquetado JAR. El paquete raíz es `com.equipo.gestorproductos` y el identificador Maven es `com.equipo:gestor-productos`.

Cada integrante necesita JDK 17 y MySQL. Maven Wrapper (`mvnw` y `mvnw.cmd`) descarga y utiliza la versión de Maven fijada en `.mvn/wrapper/maven-wrapper.properties`; no instala Java. La primera ejecución requiere conexión a internet.

## 2. Dependencias

El archivo `pom.xml` define las dependencias y la construcción. Su elemento `parent` fija Spring Boot 4.2.0 y administra las versiones compatibles de las bibliotecas.

Un *starter* reúne las bibliotecas necesarias para una función. Se incluyen:

| Dependencia | Finalidad |
| --- | --- |
| `spring-boot-starter-webmvc` | Controladores Spring MVC y servidor Tomcat integrado. |
| `spring-boot-starter-thymeleaf` | Generación de vistas HTML con datos de Java. |
| `spring-boot-starter-data-jpa` | Entidades, repositorios y persistencia mediante Hibernate. |
| `spring-boot-starter-validation` | Validación declarativa de datos. |
| `mysql-connector-j` | Conexión JDBC con MySQL; se utiliza durante la ejecución (`runtime`). |
| Starters de prueba de JPA, Thymeleaf, Validation y MVC | Herramientas de comprobación; se utilizan únicamente en pruebas (`test`). |

## 3. Preparar MySQL

1. Iniciar el servidor MySQL 8.4.13 y conectarse desde MySQL Workbench o su cliente de terminal.
2. Ejecutar `SELECT VERSION();` para comprobar la versión del servidor. El cliente de XAMPP puede corresponder a MariaDB y no confirma que se esté usando MySQL.
3. Ejecutar `sql/00-crear-base.sql` para crear la base vacía `gestor_productos`. Las tablas se incorporarán durante el desarrollo de persistencia.
4. Disponer de un usuario local con permisos sobre esa base.

En la raíz del repositorio, copiar `application-local.properties.example` como `application-local.properties` y sustituir el usuario y la contraseña por los de la instalación local. Ajustar host o puerto si son distintos de `localhost:3306`.

El archivo personal está excluido de Git. Se carga desde la raíz del proyecto mediante `spring.config.import`, por lo que los comandos deben ejecutarse en esa carpeta.

También se puede configurar la conexión mediante `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, sin crear el archivo personal. Si existe el archivo, sus propiedades de conexión tienen prioridad sobre esas alternativas.

## 4. Configuración de Spring

`src/main/resources/application.properties` contiene la configuración común:

| Propiedad | Función |
| --- | --- |
| `spring.application.name` | Nombre de la aplicación. |
| `spring.config.import` | Carga el archivo personal si existe; `optional:` permite que el archivo esté ausente. La conexión válida con MySQL sigue siendo necesaria. |
| `spring.datasource.url` | Dirección JDBC del servidor y nombre de la base. |
| `spring.datasource.username` / `password` | Datos del usuario con el que se establece la conexión. |
| `spring.jpa.hibernate.ddl-auto=none` | Impide que Hibernate cree o modifique automáticamente las tablas. |
| `spring.jpa.open-in-view=false` | Evita mantener el acceso JPA abierto durante la generación de las vistas. Los datos para la vista deberán prepararse desde el servicio. |

Las expresiones `${VARIABLE:valor}` leen una variable de entorno y usan el valor indicado después de `:` si está ausente.

## 5. Ejecutar y comprobar

Primero, comprobar que el Wrapper utiliza Java 17:

```bash
# macOS
java -version
./mvnw -version

# Windows PowerShell
java -version
.\mvnw.cmd -version
```

Con MySQL iniciado y la conexión configurada:

```bash
# macOS
./mvnw clean verify
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

`clean verify` compila, ejecuta las pruebas y genera el JAR en `target/`. La prueba inicial carga Spring y comprueba que la conexión sea con MySQL y con la base `gestor_productos`. Requiere una conexión real y no modifica registros.

`spring-boot:run` inicia la aplicación y Tomcat en el puerto 8080. La consola debe mostrar `Started GestorProductosApplication`. Se detiene con `Ctrl+C`.

Esta base todavía no tiene pantallas ni rutas del catálogo. Por ello, abrir `http://localhost:8080/` devuelve HTTP 404; la página de productos se incorporará durante el desarrollo de la interfaz y los controladores.

## 6. Abrir en IntelliJ IDEA

Abrir la carpeta del repositorio y cargar `pom.xml` como proyecto Maven. Seleccionar JDK 17 tanto en **Project SDK** como en el JDK del **Maven Runner**, y utilizar Maven Wrapper como distribución de Maven.

Después de configurar MySQL, ejecutar `GestorProductosApplication`. El directorio de trabajo de la configuración de ejecución debe ser la raíz del repositorio para que se encuentre el archivo personal de conexión.

## 7. Clase principal y prueba inicial

`GestorProductosApplication` contiene `main`, el punto de entrada de Java. Su llamada a `SpringApplication.run(...)` prepara Spring e inicia el servidor web.

La anotación `@SpringBootApplication` indica que esa es la clase de configuración principal: habilita la configuración automática según las dependencias e inicia la búsqueda de componentes desde su paquete. Los futuros paquetes `controller`, `service`, `repository`, `model` y `exception` se ubicarán debajo de `com.equipo.gestorproductos`.

En la prueba, `@SpringBootTest` carga la aplicación completa, `@Autowired` solicita a Spring la conexión administrada (`DataSource`) y `@Test` identifica el método que ejecuta JUnit. `DataSource` proporciona conexiones con la base de datos.

## Referencias

- [Requisitos de Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html).
- [Organización del código](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html).
- [Maven Wrapper](https://maven.apache.org/tools/wrapper/).
- [Manual de MySQL 8.4](https://dev.mysql.com/doc/refman/8.4/en/).
