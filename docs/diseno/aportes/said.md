## 3. Arquitectura

### 3.1. Organización por capas y MVC

El sistema será una aplicación web organizada en tres capas: presentación, lógica de negocio y acceso a datos. Se ejecutará como una sola aplicación Spring Boot, conectada a MySQL.

| Capa | Elementos principales | Responsabilidad |
| --- | --- | --- |
| Presentación | ProductoController y plantillas Thymeleaf con HTML/CSS/JS | Recibir las acciones del usuario y mostrar los resultados. |
| Lógica de negocio | ProductoService | Aplicar las reglas y coordinar las operaciones y transacciones. |
| Acceso a datos | ProductoRepository, Spring Data JPA y MySQL Connector/J | Consultar, guardar y eliminar los productos almacenados en MySQL. |

La clase Producto representará los datos del catálogo y será utilizada por las distintas capas.

El manejo de errores se apoyará en ProductoNoEncontradoException, lanzada por el servicio cuando un producto no exista, y ProductoExceptionHandler, que atenderá esa excepción desde presentación para comunicar el problema al usuario.

La interacción web seguirá el patrón Modelo–Vista–Controlador (MVC). El controlador recibirá las solicitudes del navegador y solicitará las operaciones al servicio. Las vistas Thymeleaf generarán las páginas HTML con los datos proporcionados por el controlador. El modelo comprenderá los datos y las reglas de la aplicación, organizados mediante la entidad, el servicio y el repositorio.

### 3.2. Justificación de las decisiones arquitectónicas

La arquitectura por capas permitirá separar la presentación, la lógica de negocio y el acceso a datos. Cada parte tendrá una responsabilidad definida, lo que facilitará localizar errores, revisar cambios y distribuir el trabajo del equipo.

La lógica de negocio se concentrará en ProductoService para que las operaciones apliquen las mismas reglas independientemente de la pantalla que las solicite. El acceso a MySQL quedará concentrado en ProductoRepository, evitando que los controladores tengan que realizar operaciones de persistencia directamente.

El patrón MVC organizará la interacción web mediante la separación entre los datos de la aplicación, las vistas y los controladores. Esto permitirá modificar la presentación de las pantallas manteniendo las operaciones del catálogo en sus clases correspondientes.

Spring Boot facilitará la configuración y ejecución del proyecto mediante un servidor web integrado. Spring MVC y Thymeleaf permitirán atender las solicitudes y generar páginas HTML dentro de la misma aplicación, una solución adecuada para un catálogo basado en listados y formularios.

Spring Data JPA facilitará las operaciones habituales de persistencia y la correspondencia entre los objetos Java y los registros de MySQL. Esta separación permitirá trabajar con los productos desde el servicio sin incluir allí los detalles de acceso a la base de datos.

El sistema se ejecutará como una sola aplicación conectada a una base de datos. Esta organización es proporcional al alcance del proyecto y simplificará su configuración, integración y ejecución local.

### 3.3. Relaciones y comunicación entre las partes

Las solicitudes que requieran consultar o modificar productos seguirán este recorrido:

Navegador → ProductoController → ProductoService → ProductoRepository → MySQL

ProductoController recibirá la solicitud y los datos enviados por el navegador. Después solicitará la operación correspondiente a ProductoService, que aplicará las reglas de negocio y utilizará ProductoRepository para acceder a los registros.

Los resultados regresarán al controlador a través del servicio. Para mostrar una pantalla, el controlador proporcionará los datos al modelo de la vista y Thymeleaf generará la página HTML que recibirá el navegador. Cuando corresponda actualizar el listado tras una operación, el controlador realizará una redirección.

Las excepciones previstas se comunicarán desde la capa que las detecte y se atenderán en presentación para generar una respuesta comprensible para el usuario.