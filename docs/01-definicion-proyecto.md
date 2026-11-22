# Definición del proyecto: Gestor de Productos

**Asignatura:** Arquitecturas de Software  
**Equipo:** Said Gonzalez, Fabio Gonzalez, Ramón Pacheco y Valeria Couoh

## 1. Nombre, objetivo y alcance

**Nombre provisional:** Gestor de Productos.

**Objetivo:** desarrollar una aplicación web con Java y Spring que permita consultar, registrar, modificar y eliminar productos almacenados en MySQL, mediante una interfaz sencilla.

El sistema tendrá un único módulo: la gestión del catálogo de productos. Cada producto será un registro independiente con identificador, nombre y precio. La aplicación se ejecutará localmente y se utilizará desde un navegador.

El alcance inicial comprende las operaciones del catálogo. La búsqueda por nombre y la paginación quedan como tentativas una vez completadas las funcionalidades principales. La gestión de usuarios, ventas y existencias queda fuera de esta versión.

## 2. Requisitos funcionales

| Identificador | Funcionalidad | Comportamiento esperado |
| --- | --- | --- |
| RF-01 | Consultar productos | Al entrar a la aplicación, se mostrará una tabla con el identificador, nombre y precio de los productos registrados. |
| RF-02 | Crear producto | El botón superior «Nuevo producto» abrirá un formulario para capturar nombre y precio. El identificador se generará automáticamente. |
| RF-03 | Editar producto | Cada fila tendrá una opción para abrir el formulario con los datos actuales y guardar sus cambios, conservando el identificador. |
| RF-04 | Eliminar producto | Cada fila tendrá una opción para borrar ese producto, previa confirmación. |
| RF-05 | Eliminar seleccionados | Cada fila tendrá una casilla. El botón superior «Eliminar seleccionados» permitirá borrar los productos marcados, previa confirmación. |
| RF-06 | Seleccionar todos | La casilla del encabezado permitirá marcar o desmarcar todos los productos del listado. |
| RF-07 | Eliminar todos | Al marcar todos los productos mediante la casilla del encabezado, el botón «Eliminar seleccionados» permitirá borrarlos todos, previa confirmación. |

Después de guardar o eliminar, la aplicación volverá al listado y mostrará un mensaje con el resultado. Los formularios permitirán cancelar y regresar sin guardar; cancelar una eliminación conservará los registros.

## 3. Datos y reglas del producto

**Base de datos propuesta:** `gestor_productos`.  
**Tabla:** `productos`.

| Campo | Tipo en MySQL | Tipo en Java | Regla |
| --- | --- | --- | --- |
| `id` | `BIGINT` | `Long` | Llave primaria autoincremental. El usuario no lo captura ni lo modifica. |
| `nombre` | `VARCHAR(100)` | `String` | Obligatorio, de 1 a 100 caracteres después de recortar espacios exteriores. |
| `precio` | `DECIMAL(10,2)` | `BigDecimal` | Obligatorio, entre 0 y 99999999.99, con hasta dos decimales. |

El precio se representará con `BigDecimal` para trabajar con valores decimales. Los importes se interpretarán como pesos mexicanos y se mostrarán con dos decimales.

Se aplicarán las siguientes reglas:

- El nombre se guardará sin espacios sobrantes al principio o al final y no podrá quedar vacío. Se permitirán nombres repetidos.
- Los datos se validarán en el servidor. Los precios inválidos se rechazarán sin redondearlos para ajustarlos al límite de decimales.
- Si un formulario contiene datos inválidos, se mostrarán los errores y se conservará lo capturado.
- Si no hay productos, se mostrará «No hay productos registrados» y seguirá disponible la creación.
- Si se intenta eliminar sin seleccionar registros, se solicitará seleccionar al menos uno.
- La eliminación múltiple se realizará en una sola transacción: si falla la operación o alguno de los productos ya no existe, se desharán los cambios para evitar un borrado parcial.

Para los intentos de abrir para edición, modificar o eliminar un producto inexistente, se definirá `ProductoNoEncontradoException`. La aplicación comunicará el problema y permitirá regresar al listado.

## 4. Herramientas de desarrollo

| Herramienta | Uso en el proyecto |
| --- | --- |
| Java | Lenguaje de programación. |
| Spring Boot | Configuración y arranque de la aplicación, con servidor web integrado. |
| Spring MVC | Recepción de solicitudes del navegador y coordinación de las respuestas web. |
| Thymeleaf | Construcción de páginas HTML con los datos enviados desde Java. |
| Spring Data JPA | Acceso a los datos mediante entidades y repositorios Java. |
| MySQL y MySQL Connector/J | Almacenamiento de productos y conexión de la aplicación con la base de datos. |
| Maven | Gestión de dependencias y compilación del proyecto. |
| Git y GitHub | Control de versiones, revisión e integración de cambios. |
| IntelliJ IDEA | Entorno de desarrollo de referencia. |
| Javadoc | Documentación de clases y métodos y generación de su consulta en HTML. |

Las versiones y la configuración común se definirán durante la preparación del proyecto base.

## 5. Organización por capas

La aplicación se organizará de la siguiente manera:

| Parte | Elementos previstos | Responsabilidad |
| --- | --- | --- |
| Presentación | Páginas HTML y `ProductoController` | Mostrar formularios y resultados y recibir las acciones del usuario. |
| Lógica de negocio | `ProductoService` | Aplicar reglas, comprobar la existencia de productos y coordinar operaciones y transacciones. |
| Acceso a datos | `ProductoRepository` | Consultar, guardar y eliminar productos en MySQL. |
| Modelo de datos | `Producto` | Representar los atributos del producto y su relación con la tabla. |
| Manejo de errores | Excepción y manejador correspondiente | Convertir los errores previstos en respuestas comprensibles. |

Al guardar un producto, el controlador recibirá los datos del formulario, el servicio aplicará las reglas y el repositorio realizará el guardado en MySQL. Después se mostrará nuevamente el listado. El modelo `Producto` se utilizará en las distintas capas.

Los paquetes previstos son `controller`, `service`, `repository`, `model` y `exception`.

## 6. Distribución del trabajo

| Integrante | Responsabilidad principal |
| --- | --- |
| Said Gonzalez | Dirección del proyecto, estructura, lógica de negocio, controladores e integración. |
| Fabio Gonzalez | Interfaz, formularios y selección de registros. |
| Ramón Pacheco | Base de datos, entidad `Producto` y persistencia. |
| Valeria Couoh | Pruebas, coordinación documental y demostración. |

Cada integrante documentará sus propias partes y aportará la información para los diagramas. Valeria organizará el documento conjunto. Los cambios en nombres de atributos, métodos y otros elementos compartidos se acordarán entre los responsables antes de implementarse.

## 7. Acuerdos de colaboración

`develop` será la rama predeterminada y de integración; `main` conservará las versiones estables. Cada tarea tendrá una rama creada desde `develop` actualizado, con un nombre como `feat/listado-productos` o `docs/definicion-proyecto`.

Los cambios se propondrán mediante un pull request (PR) hacia `develop`, con una descripción y las comprobaciones realizadas. Las observaciones se atenderán y sus conversaciones se resolverán antes del merge; las ramas terminadas se eliminarán.

Las protecciones acordadas requieren PR, una aprobación y conversaciones resueltas, e invalidan las aprobaciones cuando cambia el código revisado. También restringen borrados y bloquean force push. 

Las versiones estables pasarán de `develop` a `main` mediante PR y **Create a merge commit**. El origen `develop` se verificará manualmente.

Los commits seguirán el formato `tipo: descripción breve` o `tipo(ambito): descripción breve`, con el tipo en inglés y la descripción en español, en infinitivo y sin punto final.

| Tipo | Uso |
| --- | --- |
| `feat` | Nueva funcionalidad. |
| `fix` | Corrección de un fallo. |
| `docs` | Documentación. |
| `chore` | Mantenimiento y configuración. |
| `test` | Pruebas. |
| `refactor` | Reorganización sin cambiar el comportamiento. |
| `style` | Formato del código. |

Ejemplo: `docs: definir alcance y acuerdos del proyecto`.

Los cambios independientes se separarán en commits. Antes de integrar se revisarán el funcionamiento y las dependencias. Las credenciales, la configuración personal del IDE y los archivos generados se mantendrán fuera del control de versiones. Las comprobaciones y la revisión de mensajes serán manuales inicialmente.

## 8. Etapas de trabajo

| Etapa | Trabajo previsto |
| --- | --- |
| Definición | Establecer alcance, funcionalidades, datos y acuerdos de colaboración. |
| Preparación técnica | Generar el proyecto base, fijar versiones y dependencias y comprobar el arranque. |
| Diseño | Acordar pantallas y contratos entre las partes y elaborar diagramas iniciales de componentes, clases y secuencias. |
| Desarrollo | Implementar el CRUD, la selección múltiple, las validaciones y el manejo de errores. |
| Integración y documentación | Probar recorridos completos y actualizar diagramas y documentación técnica. |
| Entrega | Preparar PDF, código fuente, documentación Javadoc generada, video en YouTube y ZIP grupal; ensayar la demostración. |

