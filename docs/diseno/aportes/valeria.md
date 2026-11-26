## 1. Descripción de funcionalidades

### 1.1. Objetivo
Desarrollar una aplicación web con Java y Spring que permita consultar, registrar, 
modificar y eliminar productos almacenados en MySQL, mediante una interfaz sencilla. 
La aplicación se ejecuta de forma local y se utiliza desde un navegador.

### 1.2. Alcance
El alcance inicial comprende las operaciones del catálogo. La búsqueda por nombre y la paginación quedan como tentativas,
una vez completadas las funcionalidades principales. El sistema tiene un único módulo: la gestión del catálogo de productos. 
Cada producto es un registro independiente con identificador, nombre y precio. Existe un solo actor: el **usuario de la aplicación**.
Quedan fuera de esta versión la gestión de usuarios, las ventas y las existencias.

### 1.3. Requisitos funcionales
| Identificador | Funcionalidad | Comportamiento esperado |
| --- | --- | --- |
| RF-01 | Consultar productos | Al entrar a la aplicación se muestra una tabla con el identificador, nombre y precio de los productos registrados. |
| RF-02 | Crear producto | El botón superior «Nuevo producto» abre un formulario para capturar nombre y precio. El identificador se genera automáticamente. |
| RF-03 | Editar producto | Cada fila tiene una opción para abrir el formulario con los datos actuales y guardar sus cambios, conservando el identificador. |
| RF-04 | Eliminar producto | Cada fila tiene una opción para borrar ese producto, previa confirmación. |
| RF-05 | Eliminar seleccionados | Cada fila tiene una casilla. El botón superior «Eliminar seleccionados» permite borrar los productos marcados, previa confirmación. |
| RF-06 | Seleccionar todos | La casilla del encabezado permite marcar o desmarcar todos los productos del listado. |
| RF-07 | Eliminar todos | Al marcar todos los productos con la casilla del encabezado, el botón «Eliminar seleccionados» permite borrarlos todos, previa confirmación. |

Después de guardar o eliminar, la aplicación vuelve al listado y muestra un mensaje con el resultado. Los formularios permiten
cancelar y regresar sin guardar; cancelar una eliminación conserva los registros.

### 1.4. Reglas del producto
| Campo | Regla |
| --- | --- |
| `id` | Se genera automáticamente. El usuario no lo captura ni lo modifica. |
| `nombre` | Obligatorio, de 1 a 100 caracteres después de recortar los espacios exteriores. Se permiten nombres repetidos. |
| `precio` | Obligatorio, entre 0 y 99999999.99, con hasta dos decimales. Se interpreta en pesos mexicanos (MXN) y se muestra con dos decimales. |

Reglas generales de comportamiento:

- Los datos se validan en el servidor.
- Los precios inválidos se rechazan; no se redondean para ajustarlos al límite de decimales.
- Si un formulario contiene datos inválidos, se muestran los errores y se conserva lo capturado.
- Si no hay productos, se muestra «No hay productos registrados» y sigue disponible la creación.
- Si se intenta eliminar sin seleccionar registros, se solicita seleccionar al menos uno («Selecciona al menos un producto»).
- La eliminación múltiple se realiza en una sola operación: si falla o alguno de los productos ya no existe, se deshacen los cambios para evitar un borrado parcial.
- Al abrir para edición, modificar o eliminar un producto inexistente, la aplicación comunica el problema y permite regresar al listado.
- Una selección con identificadores repetidos se procesa una sola vez.

### 1.5. Diagrama de casos de uso
![Diagrama de casos de uso del Gestor de Productos](docs/diagramas/CasosdeUso_GestordeProductos.png)

### 1.6. Descripción de los casos de uso y resultados esperados

#### CU-01. Consultar productos (RF-01)
- **Actor:** Usuario.
- **Precondiciones:** la aplicación está en ejecución.
- **Flujo principal:**
    1. El usuario entra a la aplicación.
    2. El sistema obtiene los productos ordenados por identificador ascendente.
    3. El sistema muestra la tabla con identificador, nombre y precio (MXN, dos decimales), una casilla y las opciones 
       editar y eliminar en cada fila, y los botones superiores «Nuevo producto» y «Eliminar seleccionados».
- **Alternativas:**
    - *A1. Listado vacío:* si no existen productos registrados, el sistema muestra el mensaje «No hay productos registrados» 
           y mantiene disponible la opción «Nuevo producto».
    - *A2. Error al consultar los productos:* si ocurre un problema al obtener los datos, el sistema muestra un mensaje 
           indicando que no fue posible cargar el listado, sin exponer detalles técnicos internos.
- **Resultado esperado:** el usuario ve el catálogo actual, o el aviso de listado vacío.

#### CU-02. Crear producto (RF-02)
- **Actor:** Usuario.
- **Precondiciones:** el usuario se encuentra en el listado de productos.
- **Flujo principal:**
    1. El usuario selecciona la opción «Nuevo producto».
    2. El sistema muestra el formulario vacío en modo creación.
    3. El usuario captura nombre y precio del producto y envía el formulario.
    4. El sistema valida los datos ingresados y elimina los espacios en blanco al inicio y al final del nombre.
    5. El sistema guarda el producto con un identificador generado automáticamente.
    6. El sistema regresa al listado y muestra un mensaje de éxito.
- **Alternativas:**
    - *A1. Datos inválidos:* si el nombre está vacío o supera los 100 caracteres, o si el precio está vacío, no es numérico, 
           está fuera del rango permitido o tiene más de dos decimales, el sistema muestra los errores correspondientes y 
           conserva los datos capturados. El producto no se guarda y el precio no se redondea.
    - *A2. Cancelación del registro:* si el usuario cancela, el sistema regresa al listado sin guardar.
    - *A3. Error al guardar:* si ocurre un fallo de persistencia, el sistema comunica un mensaje general de error, 
           sin mostrar detalles internos.
- **Resultado esperado:** el nuevo producto aparece en el listado con su identificador. Los nombres repetidos están permitidos.

#### CU-03. Editar producto (RF-03)
- **Actor:** Usuario.
- **Precondiciones:** el usuario se encuentra en el listado de productos y el producto que desea modificar existe.
- **Flujo principal:**
    1. El usuario selecciona la opción «Editar» correspondiente al producto que desea modificar.
    2. El sistema muestra el formulario en modo edición con los datos actuales del producto.
    3. El usuario modifica nombre, precio o ambos, y envía el formulario.
    4. El sistema valida los datos ingresados.
    5. El sistema actualiza el producto conservando su identificador.
    6. El sistema regresa al listado y muestra un mensaje de éxito.
- **Alternativas:**
    - *A1. Datos inválidos:* si el nombre está vacío o supera los 100 caracteres, o si el precio está 
           vacío, no es numérico, está fuera del rango permitido o tiene más de dos decimales, el sistema muestra los 
           errores correspondientes y conserva los datos capturados. No se guardan los cambios y el precio no se redondea.
    - *A2. Cancelación de la edición:* si el usuario cancela, el sistema regresa al listado sin guardar los cambios.
    - *A3. Producto inexistente:* si el producto no existe al abrir el formulario de edición o 
           al guardar los cambios, el sistema no realiza la operación, regresa al listado y muestra un mensaje indicando
           que no se encontró el producto.
    - *A4. Error al actualizar:* si ocurre un fallo de persistencia, el sistema comunica un mensaje general de error, 
           sin mostrar detalles internos. La información original del producto se conserva.
- **Resultado esperado:** el producto queda actualizado y aparece en el listado con la nueva información, 
    su identificador no cambia.

#### CU-04. Eliminar producto (RF-04)
- **Actor:** Usuario.
- **Precondiciones:** el usuario se encuentra en el listado de productos y el producto que desea eliminar existe.
- **Flujo principal:**
    1. El usuario selecciona la opción «Eliminar» correspondiente al producto que desea borrar.
    2. El sistema solicita confirmación.
    3. El usuario confirma la eliminación.
    4. El sistema elimina el producto.
    5. El sistema regresa al listado y muestra un mensaje de éxito.
- **Alternativas:**
    - *A1. Cancelación de la eliminación:* si el usuario cancela en la confirmación, el sistema no realiza la eliminación 
           y regresa al listado con el producto conservado.
    - *A2. Producto inexistente:* si el producto no existe al confirmar la eliminación, el sistema no realiza la operación, 
           regresa al listado y muestra un mensaje indicando que no se encontró el producto.
    - *A3. Error al eliminar:* si ocurre un fallo de persistencia, el sistema regresa al listado y muestra un mensaje 
           general de error, sin mostrar detalles internos. El registro permanece sin cambios y el listado conserva 
           la información disponible.
- **Resultado esperado:** el producto deja de aparecer en el listado. Los demás productos permanecen sin modificaciones.

#### CU-05. Eliminar seleccionados (RF-05)
- **Actor:** Usuario.
- **Precondiciones:** el usuario se encuentra en el listado de productos.
- **Flujo principal:**
    1. El usuario marca las casillas correspondientes a uno o más productos que desea eliminar.
    2. El usuario selecciona la opción «Eliminar seleccionados».
    3. El sistema solicita confirmación.
    4. El usuario confirma la operación.
    5. El sistema elimina todos los productos seleccionados en una sola operación.
    6. El sistema regresa al listado y muestra un mensaje de éxito.
- **Alternativas:**
    - *A1. Sin selección:* si no hay productos marcados, el sistema muestra «Selecciona al menos un producto» y no elimina nada.
    - *A2. Cancelación:* si el usuario cancela en la confirmación, no se envía la operación y no se elimina ningún producto.
    - *A3. Producto inexistente:* si alguno de los productos seleccionados no existe, el sistema revierte todos los cambios, 
           regresa al listado y muestra un mensaje indicando que no se encontró el producto.
    - *A4. Error durante la eliminación:* si ocurre un fallo de persistencia, el sistema revierte todos los cambios, 
           regresa al listado y muestra un mensaje general de error, sin mostrar detalles internos. Ningún registro debe 
           quedar eliminado parcialmente.
- **Resultado esperado:** desaparecen del listado los productos seleccionados. Si ocurre algún error, no se elimina ninguno.

#### CU-06. Seleccionar o desmarcar todos (RF-06)
- **Actor:** Usuario.
- **Precondiciones:** el usuario se encuentra en el listado de productos.
- **Flujo principal:**
    1. El usuario marca la casilla de selección ubicada en el encabezado de la tabla.
    2. El sistema marca automáticamente las casillas de todos los productos que aparecen en el listado.
    3. Si el usuario vuelve a pulsar la casilla del encabezado, el sistema las desmarca todas.
- **Alternativas:**
    - *A1. Listado vacío:* si no existen productos registrados, el listado muestra «No hay productos registrados» y no hay
           productos que seleccionar.
    - *A2. Selección parcial:* si el usuario ha seleccionado manualmente uno o varios productos y posteriormente marca la
           casilla del encabezado, el sistema selecciona todos los productos del listado.
- **Resultado esperado:** todas las casillas de los productos quedan seleccionadas o desmarcadas según la acción del usuario. 
    Esta operación únicamente modifica el estado de selección y no elimina ni modifica los registros.

#### CU-07. Eliminar todos (RF-07)
- **Actor:** Usuario.
- **Precondiciones:** el usuario se encuentra en el listado de productos.
- **Flujo principal:**
    1. El usuario marca la casilla de selección ubicada en el encabezado de la tabla para seleccionar todos los productos.
    2. El usuario selecciona la opción «Eliminar seleccionados».
    3. El sistema solicita confirmación.
    4. El usuario confirma la operación.
    5. El sistema elimina todos los productos seleccionados de la base de datos mediante una sola operación.
    6. El sistema regresa al listado, muestra el mensaje «No hay productos registrados» y un mensaje de éxito.
- **Alternativas:**
    - *A1. Listado vacío:* si no existen productos registrados, no hay nada que seleccionar. Si el usuario pulsa «Eliminar
       seleccionados», el sistema muestra «Selecciona al menos un producto» y no elimina nada.
    - *A2. Cancelación de la eliminación:* si el usuario cancela en la confirmación, el sistema no elimina ningún 
       producto y regresa al listado.
    - *A3. Error durante la eliminación:* si ocurre un problema al eliminar o alguno de los productos ya no existe, 
       el sistema revierte la operación completa, no elimina ninguno y muestra un mensaje indicando que no fue posible completar la eliminación.
- **Resultado esperado:** todos los productos registrados dejan de aparecer en el listado y se eliminan de la base de datos. 
    El listado queda vacío y muestra el mensaje «No hay productos registrados». Si ocurre algún error, todos los productos se conservan.
