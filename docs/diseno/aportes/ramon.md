# 6. Descripción de clases e interfaces Java

> Aportación de Ramón Pacheco: `ProductoService`, `ProductoRepository`, `Producto` y `ProductoNoEncontradoException`.
> Paquete raíz: `com.equipo.gestorproductos`. Base técnica: Java 17, Spring Boot 4.2.0 (Spring Data JPA, Hibernate, Jakarta Persistence), MySQL 8.4.

## 6.1. Descripción individual según el formato solicitado

### ProductoService
- Tipo: clase.
- Paquete: `com.equipo.gestorproductos.service`
- Componente: Gestión de productos.
- Descripción y responsabilidad: concentra la lógica de negocio del catálogo. Valida y normaliza nombre y precio, comprueba que los productos existan, coordina las operaciones con `ProductoRepository` y define los límites transaccionales. Es el único lugar donde se interpretan los resultados del repositorio (`Optional` vacío, menos registros de los solicitados). No conoce rutas, plantillas ni mensajes flash. Se anota con `@Service` y `@Transactional` a nivel de clase; `listar` y `obtenerPorId` usan `@Transactional(readOnly = true)`.

#### Dependencias y relaciones
| Clase/interfaz | Tipo de relación | Propósito |
| --- | --- | --- |
| `ProductoRepository` | Asociación dirigida (atributo, inyección por constructor) | Consultar, guardar y eliminar productos. |
| `Producto` | Dependencia de uso (parámetros, variables y retornos) | Entidad que se crea, modifica y devuelve. |
| `ProductoNoEncontradoException` | Dependencia (la lanza) | Señalar productos inexistentes. |
| `ProductoController` (Fabio) | Es usada por (asociación dirigida desde el controlador) | Consumidor de las seis operaciones públicas. |

#### Atributos
| Nombre | Tipo | Visibilidad | Valor inicial/asignación | Descripción |
| --- | --- | --- | --- | --- |
| `productoRepository` | `ProductoRepository` | private final | Asignado en el constructor (inyección de Spring). | Acceso a datos. |
| `NOMBRE_MAX_LONGITUD` | `int` | private static final | `100` | Longitud máxima del nombre tras recortarlo. |
| `PRECIO_MAXIMO` | `BigDecimal` | private static final | `new BigDecimal("99999999.99")` | Límite superior del precio. El inferior es `BigDecimal.ZERO`. |
| `PRECIO_ESCALA_MAXIMA` | `int` | private static final | `2` | Máximo de decimales significativos del precio. |

#### Operaciones

**Constructor**
- Firma y visibilidad: `public ProductoService(ProductoRepository productoRepository)`
- Descripción: recibe la dependencia del repositorio.
- Parámetros: `productoRepository` (`ProductoRepository`): repositorio que se usará en todas las operaciones.
- Retorno: no aplica (constructor).
- Servicio e interfaz del componente que implementa: no aplica.
- Precondiciones: Spring proporciona un repositorio no nulo.
- Resultado esperado: servicio listo para usarse.
- Excepciones previstas: ninguna.
- Origen: declarado.

**listar**
- Firma y visibilidad: `public List<Producto> listar()`
- Descripción: devuelve todos los productos ordenados por ID ascendente (`findAll(Sort.by(Sort.Direction.ASC, "id"))`).
- Parámetros: ninguno.
- Retorno: `List<Producto>`: productos registrados; lista vacía si no hay ninguno, nunca `null`.
- Servicio e interfaz del componente que implementa: consulta de productos de la interfaz de salida de Gestión de productos (RF-01).
- Precondiciones: ninguna.
- Resultado esperado: lista ordenada por ID ascendente.
- Excepciones previstas: solo errores técnicos de acceso a datos (`DataAccessException`).
- Origen: declarado.

**obtenerPorId**
- Firma y visibilidad: `public Producto obtenerPorId(Long id)`
- Descripción: recupera un producto existente; se usa para abrir el formulario de edición.
- Parámetros: `id` (`Long`): identificador del producto.
- Retorno: `Producto`: el producto con ese ID.
- Servicio e interfaz del componente que implementa: consulta por ID de la interfaz de salida de Gestión de productos (RF-03).
- Precondiciones: `id` no nulo y mayor que cero.
- Resultado esperado: producto existente con sus datos actuales.
- Excepciones previstas: `IllegalArgumentException` si el ID es nulo o no positivo; `ProductoNoEncontradoException` si no existe.
- Origen: declarado.

**crear**
- Firma y visibilidad: `public Producto crear(String nombre, BigDecimal precio)`
- Descripción: valida los datos, recorta el nombre, crea un `Producto` nuevo (ID nulo) y lo guarda con `save`.
- Parámetros: `nombre` (`String`): nombre capturado, con posibles espacios exteriores; `precio` (`BigDecimal`): precio en MXN.
- Retorno: `Producto`: la instancia devuelta por `save`, con el ID generado (no nulo).
- Servicio e interfaz del componente que implementa: alta de producto de la interfaz de salida de Gestión de productos (RF-02).
- Precondiciones: nombre no nulo, de 1 a 100 caracteres tras recortar; precio no nulo, entre 0 y 99999999.99 y con hasta dos decimales.
- Resultado esperado: fila nueva en `productos`; el nombre se guarda recortado y el precio con escala 2 (sin redondeo).
- Excepciones previstas: `IllegalArgumentException` con mensaje comprensible si algún dato es inválido (no se guarda nada); errores técnicos de acceso a datos.
- Origen: declarado.

**actualizar**
- Firma y visibilidad: `public Producto actualizar(Long id, String nombre, BigDecimal precio)`
- Descripción: comprueba que el producto exista, valida los datos y modifica la instancia gestionada conservando su ID. Nunca construye un `Producto` nuevo con ese ID, porque `save` podría interpretarlo como fusión de una entidad desconectada y acabar insertando un sustituto.
- Parámetros: `id` (`Long`): producto a modificar; `nombre` (`String`): nuevo nombre; `precio` (`BigDecimal`): nuevo precio.
- Retorno: `Producto`: producto actualizado, con el mismo ID.
- Servicio e interfaz del componente que implementa: modificación de producto de la interfaz de salida de Gestión de productos (RF-03).
- Precondiciones: `id` positivo y no nulo; producto existente; `nombre` y `precio` cumplen las reglas del modelo.
- Resultado esperado: la fila existente cambia de nombre y precio; no se crean ni eliminan filas.
- Excepciones previstas: `IllegalArgumentException` (ID, nombre o precio inválidos); `ProductoNoEncontradoException` si el ID no existe; si otro usuario lo elimina mientras se actualiza, Hibernate detecta que no se afectó ninguna fila y la transacción se revierte con una excepción técnica de Spring (`ObjectOptimisticLockingFailureException`), sin insertar nada.
- Origen: declarado.

**eliminar**
- Firma y visibilidad: `public void eliminar(Long id)`
- Descripción: comprueba que el producto exista y lo elimina con `delete(Producto)`.
- Parámetros: `id` (`Long`): producto a eliminar.
- Retorno: `void`.
- Servicio e interfaz del componente que implementa: baja de producto de la interfaz de salida de Gestión de productos (RF-04).
- Precondiciones: `id` positivo y no nulo; producto existente.
- Resultado esperado: la fila deja de existir.
- Excepciones previstas: `IllegalArgumentException` si el ID es inválido; `ProductoNoEncontradoException` si no existe; `ObjectOptimisticLockingFailureException` si desaparece de forma concurrente antes de confirmar.
- Origen: declarado.

**eliminarSeleccionados**
- Firma y visibilidad: `public void eliminarSeleccionados(List<Long> ids)`
- Descripción: elimina en una única transacción todos los productos indicados o ninguno. Ver «Garantía de la eliminación múltiple».
- Parámetros: `ids` (`List<Long>`): IDs seleccionados; los repetidos se procesan una sola vez.
- Retorno: `void`.
- Servicio e interfaz del componente que implementa: baja múltiple de la interfaz de salida de Gestión de productos (RF-05, RF-06 y RF-07; «eliminar todos» es seleccionar todo el listado).
- Precondiciones: `ids` no nulo ni vacío; cada elemento no nulo y positivo; todos los IDs existen.
- Resultado esperado: todos los productos solicitados eliminados; ante cualquier fallo, ningún cambio persistido.
- Excepciones previstas: `IllegalArgumentException("Selecciona al menos un producto")` si la lista es nula o vacía; `IllegalArgumentException` con mensaje comprensible si hay elementos nulos o no positivos; `ProductoNoEncontradoException` con uno de los IDs faltantes (el primero en el orden recibido); `ObjectOptimisticLockingFailureException` u otro error técnico si hay cambios concurrentes o falla el borrado. En todos los casos se revierte la transacción completa.
- Origen: declarado.

**Operaciones internas (no forman parte del contrato público)**

| Firma y visibilidad | Descripción | Excepciones |
| --- | --- | --- |
| `private void validarId(Long id)` | Rechaza ID nulo o menor o igual que cero. | `IllegalArgumentException` |
| `private Producto buscarExistente(Long id)` | `findById(id).orElseThrow(...)`: interpreta el `Optional` vacío. | `ProductoNoEncontradoException` |
| `private String validarNombre(String nombre)` | Rechaza nulo; aplica `strip()`; exige de 1 a 100 caracteres; devuelve el nombre recortado. | `IllegalArgumentException` |
| `private BigDecimal validarPrecio(BigDecimal precio)` | Rechaza nulo, fuera de 0–99999999.99 o con más de dos decimales significativos (`precio.stripTrailingZeros().scale() > 2`, de modo que `10.500` es válido y `10.505` no). Devuelve el valor con `setScale(2, RoundingMode.UNNECESSARY)`, que nunca redondea porque ya se comprobó. | `IllegalArgumentException` |
| `private Set<Long> validarSeleccion(List<Long> ids)` | Rechaza lista nula o vacía y elementos nulos o no positivos; devuelve un `LinkedHashSet` (sin repetidos, conservando el orden). | `IllegalArgumentException` |

#### Mensajes de validación propuestos
| Caso | Mensaje |
| --- | --- |
| ID nulo o no positivo | «El ID del producto debe ser un número positivo». |
| Nombre nulo, vacío o solo espacios | «El nombre es obligatorio». |
| Nombre de más de 100 caracteres | «El nombre no puede exceder 100 caracteres». |
| Precio nulo | «El precio es obligatorio». |
| Precio fuera de rango | «El precio debe estar entre 0 y 99999999.99». |
| Precio con más de dos decimales | «El precio admite como máximo dos decimales». |
| Selección nula o vacía | «Selecciona al menos un producto» (texto fijado por el contrato). |
| Selección con elementos inválidos | «La selección contiene IDs no válidos». |

#### Garantía de la eliminación múltiple
`eliminarSeleccionados` se ejecuta dentro de **una sola transacción** (`@Transactional` en la clase; los errores previstos son `RuntimeException`, por lo que Spring hace rollback automáticamente). Pasos:

1. Validar la selección y quitar repetidos (`validarSeleccion`). Si falla, no se toca la base de datos.
2. Cargar los productos con `findAllById(ids)`. Quedan como entidades gestionadas en la transacción.
3. Si se devolvieron menos registros que IDs distintos, localizar el primer ID faltante y lanzar `ProductoNoEncontradoException(id)`. No se ha borrado nada, y aunque se hubiera borrado, el rollback lo deshace.
4. Llamar a `deleteAll(productos)` con las entidades ya cargadas, en la misma transacción.

Por qué no hay borrados parciales:
- No se ignoran IDs faltantes; basta uno para rechazar toda la operación.
- Las eliminaciones se confirman todas juntas al terminar la transacción. Si algo falla, se descartan todas.
- **Cambios concurrentes:** otro usuario podría borrar un producto entre el paso 2 y la confirmación. Como las entidades están gestionadas en esa misma transacción, `delete` no se convierte en una operación vacía; Hibernate emite el `DELETE` y, al comprobar que no afectó ninguna fila, lanza una excepción que Spring traduce a `ObjectOptimisticLockingFailureException`. Esa excepción revierte la transacción completa. Por eso la carga y el borrado **deben quedar en la misma transacción** y no repartirse entre llamadas separadas al repositorio.
- No se agrega columna de versión ni consultas con bloqueo: el contrato exige que la tabla sea la acordada y que no haya métodos personalizados inicialmente. La implementación debe cubrir este escenario con una prueba.
- La excepción técnica puede aparecer al confirmar la transacción, es decir, al salir del método. Para el llamador el efecto es el mismo: la excepción llega y no queda nada borrado.

---

### ProductoRepository
- Tipo: interfaz.
- Paquete: `com.equipo.gestorproductos.repository`
- Componente: Persistencia de productos.
- Descripción y responsabilidad: acceso a la tabla `productos` mediante Spring Data JPA. Extiende `JpaRepository<Producto, Long>`; Spring genera la implementación en tiempo de ejecución. Inicialmente no declara métodos propios. Solo ofrece acceso a datos: no valida reglas de negocio ni decide qué hacer con resultados ausentes, tarea que corresponde al servicio.

```java
public interface ProductoRepository
        extends JpaRepository<Producto, Long> {
}
```

#### Dependencias y relaciones
| Clase/interfaz | Tipo de relación | Propósito |
| --- | --- | --- |
| `JpaRepository<Producto, Long>` | Generalización (extiende) | Aporta las operaciones CRUD, de orden y de consulta heredadas. |
| `Producto` | Dependencia (argumento de tipo `T`) | Entidad gestionada. |
| `Long` | Dependencia (argumento de tipo `ID`) | Tipo de la clave primaria. |
| `ProductoService` | Es usada por (asociación dirigida desde el servicio) | Único consumidor del repositorio. |

#### Atributos
No aplica: la interfaz no declara atributos.

#### Operaciones
Todas son heredadas; no hay operaciones declaradas en la interfaz. Se documentan las relevantes. Las firmas se expresan con `T = Producto` e `ID = Long`. Todas son públicas (miembros de interfaz) y participan en la transacción del servicio que las llama.

**findAll(Sort)**
- Firma y visibilidad: `public List<Producto> findAll(Sort sort)`
- Descripción: devuelve todos los productos con el orden indicado.
- Parámetros: `sort` (`Sort`): criterio de orden (el servicio usa `Sort.by(Sort.Direction.ASC, "id")`).
- Retorno: `List<Producto>`; vacía si no hay registros.
- Servicio e interfaz del componente que implementa: consulta de productos de la interfaz de salida de Persistencia de productos.
- Precondiciones: `sort` no nulo; la propiedad indicada existe en `Producto`.
- Resultado esperado: productos ordenados.
- Excepciones previstas: `DataAccessException` ante fallos técnicos.
- Origen: heredado (`ListPagingAndSortingRepository`, a través de `JpaRepository`).

**findById(Long)**
- Firma y visibilidad: `public Optional<Producto> findById(Long id)`
- Descripción: busca un producto por su clave primaria.
- Parámetros: `id` (`Long`): ID buscado; no nulo.
- Retorno: `Optional<Producto>`; **vacío si no existe**. El servicio lo interpreta y lanza `ProductoNoEncontradoException`.
- Servicio e interfaz del componente que implementa: consulta por ID de la interfaz de salida de Persistencia de productos.
- Precondiciones: `id` no nulo.
- Resultado esperado: producto o `Optional` vacío.
- Excepciones previstas: `IllegalArgumentException` si `id` es nulo (el servicio lo valida antes); `DataAccessException`.
- Origen: heredado (`CrudRepository`).

**findAllById(Iterable<Long>)**
- Firma y visibilidad: `public List<Producto> findAllById(Iterable<Long> ids)`
- Descripción: recupera los productos cuyos IDs se indican.
- Parámetros: `ids` (`Iterable<Long>`): IDs buscados, sin nulos ni repetidos.
- Retorno: `List<Producto>`; **puede tener menos elementos que IDs solicitados** (los inexistentes se omiten sin error) y no garantiza el orden de entrada. El servicio compara los resultados con la solicitud.
- Servicio e interfaz del componente que implementa: consulta múltiple de la interfaz de salida de Persistencia de productos.
- Precondiciones: `ids` no nulo.
- Resultado esperado: los productos existentes entre los solicitados.
- Excepciones previstas: `DataAccessException`.
- Origen: heredado (`ListCrudRepository`).

**save(Producto)**
- Firma y visibilidad: `public <S extends Producto> S save(S entity)`
- Descripción: inserta si el ID es nulo (la base genera el ID y se copia al objeto) o actualiza/fusiona si ya tiene ID.
- Parámetros: `entity` (`S extends Producto`): entidad a guardar, no nula.
- Retorno: la instancia guardada; el servicio debe usar **el valor devuelto**, no el argumento.
- Servicio e interfaz del componente que implementa: guardado de producto de la interfaz de salida de Persistencia de productos.
- Precondiciones: entidad no nula, con datos ya validados por el servicio.
- Resultado esperado: fila insertada o modificada.
- Excepciones previstas: `DataAccessException` (por ejemplo, violación de restricciones de la tabla).
- Origen: heredado (`CrudRepository`).

**delete(Producto)**
- Firma y visibilidad: `public void delete(Producto entity)`
- Descripción: elimina la entidad indicada.
- Parámetros: `entity` (`Producto`): entidad a eliminar, no nula.
- Retorno: `void`.
- Servicio e interfaz del componente que implementa: baja de producto de la interfaz de salida de Persistencia de productos.
- Precondiciones: entidad no nula, obtenida antes en la misma transacción. Si la entidad no está gestionada y no existe en la base, Spring Data trata la llamada como operación vacía sin avisar, por lo que la existencia debe comprobarla el servicio.
- Resultado esperado: fila eliminada.
- Excepciones previstas: `DataAccessException`; `ObjectOptimisticLockingFailureException` si la fila desapareció antes del borrado efectivo.
- Origen: heredado (`CrudRepository`).

**deleteAll(Iterable<Producto>)**
- Firma y visibilidad: `public void deleteAll(Iterable<? extends Producto> entities)`
- Descripción: elimina las entidades indicadas, una por una, dentro de la transacción en curso.
- Parámetros: `entities` (`Iterable<? extends Producto>`): entidades a eliminar, no nulas.
- Retorno: `void`.
- Servicio e interfaz del componente que implementa: baja múltiple de la interfaz de salida de Persistencia de productos.
- Precondiciones: se invoca dentro de la transacción de `eliminarSeleccionados`, con entidades cargadas en ella.
- Resultado esperado: todas las filas eliminadas al confirmarse la transacción; ninguna si esta se revierte.
- Excepciones previstas: `DataAccessException`; `ObjectOptimisticLockingFailureException` ante cambios concurrentes.
- Origen: heredado (`CrudRepository`).

---

### Producto
- Tipo: clase (entidad JPA).
- Paquete: `com.equipo.gestorproductos.model`
- Componente: modelo compartido por Gestión de productos y Persistencia de productos (también lo usan la presentación y la persistencia).
- Descripción y responsabilidad: representa un producto del catálogo y su correspondencia con la fila de la tabla `productos` de la base `gestor_productos`. Es un objeto de datos: no contiene lógica de negocio ni validaciones; estas viven en el servicio (y el formulario las repite). Se anota con `@Entity` y `@Table(name = "productos")` (Jakarta Persistence). El mapeo es explícito porque `ddl-auto=none`: la tabla se crea mediante script SQL y la entidad debe coincidir con ella. Montos en pesos mexicanos (MXN); se permiten nombres repetidos.

#### Dependencias y relaciones
| Clase/interfaz | Tipo de relación | Propósito |
| --- | --- | --- |
| Tabla `productos` (MySQL) | Correspondencia objeto-relacional (JPA) | Cada instancia equivale a una fila. |
| `ProductoService` | Es usada por (dependencia desde el servicio) | Crea, modifica y devuelve productos. |
| `ProductoRepository` | Es gestionada por (argumento de tipo) | Persiste y consulta instancias. |
| `ProductoController` y vistas (Fabio) | Es usada por | Transporta datos entre formulario, listado y servicio. |
| `BigDecimal`, `Long`, `String` | Asociación con tipos del JDK | Tipos de los atributos. |

#### Atributos
| Nombre | Tipo | Visibilidad | Valor inicial/asignación | Descripción |
| --- | --- | --- | --- | --- |
| `id` | `Long` | private | `null` hasta guardar; lo genera MySQL (`@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`). | Clave primaria `BIGINT` autoincremental. No editable por el usuario. |
| `nombre` | `String` | private | `null`; lo asigna el servicio ya recortado (`@Column(name = "nombre", nullable = false, length = 100)`). | `VARCHAR(100)`, obligatorio, de 1 a 100 caracteres tras recortar. Puede repetirse. |
| `precio` | `BigDecimal` | private | `null`; lo asigna el servicio con escala 2 (`@Column(name = "precio", nullable = false, precision = 10, scale = 2)`). | `DECIMAL(10,2)`, obligatorio, entre 0 y 99999999.99, en MXN. |

#### Operaciones

**Constructor sin argumentos**
- Firma y visibilidad: `public Producto()`
- Descripción: crea un producto vacío; exigido por JPA y usado por el servicio al crear. Es el único constructor previsto.
- Parámetros: ninguno.
- Retorno: no aplica (constructor).
- Servicio e interfaz del componente que implementa: no aplica.
- Precondiciones: ninguna.
- Resultado esperado: instancia con `id`, `nombre` y `precio` en `null`; el ID permanece nulo hasta guardarlo.
- Excepciones previstas: ninguna.
- Origen: declarado.

**getId / setId**
- Firma y visibilidad: `public Long getId()` y `public void setId(Long id)`
- Descripción: lectura y asignación del identificador. `setId` existe por el contrato de getters/setters y por JPA, pero el usuario no puede capturar ni modificar el ID: el servicio nunca lo recibe desde el formulario ni lo asigna en `crear` o `actualizar`.
- Parámetros: `id` (`Long`): identificador.
- Retorno: `getId` devuelve `Long` (`null` si aún no se guardó); `setId` devuelve `void`.
- Servicio e interfaz del componente que implementa: no aplica.
- Precondiciones: ninguna.
- Resultado esperado: se lee o asigna el valor sin transformarlo.
- Excepciones previstas: ninguna.
- Origen: declarado.

**getNombre / setNombre**
- Firma y visibilidad: `public String getNombre()` y `public void setNombre(String nombre)`
- Descripción: lectura y asignación del nombre. La clase no recorta ni valida; eso lo hace el servicio.
- Parámetros: `nombre` (`String`): nombre del producto.
- Retorno: `getNombre` devuelve `String`; `setNombre` devuelve `void`.
- Servicio e interfaz del componente que implementa: no aplica.
- Precondiciones: ninguna a nivel de la clase; las reglas del modelo se verifican antes en el servicio.
- Resultado esperado: se lee o asigna el valor sin transformarlo.
- Excepciones previstas: ninguna.
- Origen: declarado.

**getPrecio / setPrecio**
- Firma y visibilidad: `public BigDecimal getPrecio()` y `public void setPrecio(BigDecimal precio)`
- Descripción: lectura y asignación del precio. No redondea; cualquier valor inválido se rechaza antes en el servicio.
- Parámetros: `precio` (`BigDecimal`): precio en MXN.
- Retorno: `getPrecio` devuelve `BigDecimal`; `setPrecio` devuelve `void`.
- Servicio e interfaz del componente que implementa: no aplica.
- Precondiciones: ninguna a nivel de la clase.
- Resultado esperado: se lee o asigna el valor sin transformarlo.
- Excepciones previstas: ninguna.
- Origen: declarado.

> `equals`, `hashCode` y `toString` no forman parte del contrato; si se añaden durante el desarrollo, se documentarán en la ficha.

---

### ProductoNoEncontradoException
- Tipo: excepción (no verificada).
- Paquete: `com.equipo.gestorproductos.exception`
- Componente: Gestión de productos.
- Descripción y responsabilidad: indica que se intentó abrir para edición, modificar o eliminar un producto que no existe. La lanza únicamente `ProductoService`; el manejador web que la convierte en una respuesta comprensible (con opción de volver al listado) lo documenta e implementa Fabio. Al ser `RuntimeException`, provoca el rollback de la transacción donde se lanza.

#### Dependencias y relaciones
| Clase/interfaz | Tipo de relación | Propósito |
| --- | --- | --- |
| `RuntimeException` | Generalización (extiende) | Excepción no verificada; no obliga a declarar `throws`. |
| `ProductoService` | Es usada por (la lanza) | Señala productos inexistentes en `obtenerPorId`, `actualizar`, `eliminar` y `eliminarSeleccionados`. |
| `ProductoExceptionHandler` (Fabio) | Es capturada por | Muestra el error y permite regresar al listado. |

#### Atributos
No aplica: no declara atributos propios (el ID solo forma parte del mensaje). Solo se prevé `private static final long serialVersionUID = 1L`, por ser serializable.

#### Operaciones

**Constructor**
- Firma y visibilidad: `public ProductoNoEncontradoException(Long id)`
- Descripción: crea la excepción con el mensaje «No se encontró el producto con ID {id}», sustituyendo `{id}` por el valor recibido.
- Parámetros: `id` (`Long`): ID que no se localizó.
- Retorno: no aplica (constructor).
- Servicio e interfaz del componente que implementa: no aplica.
- Precondiciones: ninguna; el servicio pasa el ID que no se encontró.
- Resultado esperado: excepción con el mensaje indicado, accesible con `getMessage()`.
- Excepciones previstas: ninguna.
- Origen: declarado.

**getMessage**
- Firma y visibilidad: `public String getMessage()`
- Descripción: devuelve el mensaje de la excepción. No se sobrescribe: el constructor lo pasa a `super(...)`.
- Parámetros: ninguno.
- Retorno: `String` con el mensaje «No se encontró el producto con ID {id}».
- Servicio e interfaz del componente que implementa: no aplica.
- Precondiciones: ninguna.
- Resultado esperado: texto que el manejador web puede mostrar.
- Excepciones previstas: ninguna.
- Origen: heredado (`Throwable`).

---

## Insumos para el diagrama general de clases (6.2)

Resumen entregado a Said:

| Origen | Relación | Destino |
| --- | --- | --- |
| `ProductoService` | Asociación dirigida (`productoRepository`) | `ProductoRepository` |
| `ProductoService` | Dependencia («use») | `Producto` |
| `ProductoService` | Dependencia («throws») | `ProductoNoEncontradoException` |
| `ProductoRepository` | Generalización (extiende) | `JpaRepository<Producto, Long>` |
| `ProductoRepository` | Dependencia (argumento de tipo) | `Producto` |
| `ProductoNoEncontradoException` | Generalización | `RuntimeException` |
| `ProductoController` (Fabio) | Asociación dirigida | `ProductoService` |
| `Producto` | Mapeo JPA | Tabla `productos` |

## Llamadas servicio → repositorio para las secuencias (8)

Insumo para Valeria; incluye las alternativas relevantes de cada operación.

| Operación del servicio | Llamadas a `ProductoRepository`, en orden | Alternativas |
| --- | --- | --- |
| `listar()` | `findAll(Sort ID ascendente)` | Sin productos: lista vacía, no error. |
| `obtenerPorId(id)` | `findById(id)` | `Optional` vacío → `ProductoNoEncontradoException`. ID inválido → `IllegalArgumentException`, sin llamar al repositorio. |
| `crear(nombre, precio)` | `save(producto nuevo)` | Datos inválidos → `IllegalArgumentException`, sin llamar al repositorio. |
| `actualizar(id, nombre, precio)` | `findById(id)` y luego `save(producto cargado)` | Inexistente → `ProductoNoEncontradoException`. Datos inválidos → `IllegalArgumentException`, sin `save`. |
| `eliminar(id)` | `findById(id)` y luego `delete(producto)` | Inexistente → `ProductoNoEncontradoException`, sin `delete`. |
| `eliminarSeleccionados(ids)` | `findAllById(ids sin repetidos)` y luego `deleteAll(productos)` | Selección vacía → `IllegalArgumentException("Selecciona al menos un producto")`, sin llamar al repositorio. Faltan IDs → `ProductoNoEncontradoException`, sin `deleteAll`. Fallo o cambio concurrente → rollback completo y error técnico sin detalles internos en pantalla. |

## Puntos por confirmar

| Con quién | Tema |
| --- | --- |
| Said | Su sección 7 debe coincidir con la entidad: `id BIGINT` autoincremental (clave primaria), `nombre VARCHAR(100) NOT NULL`, `precio DECIMAL(10,2) NOT NULL`. El script SQL de la tabla se desarrollará después, en la etapa de persistencia, con `ddl-auto=none`; `sql/00-crear-base.sql` solo crea la base. |
| Said | Nombres exactos de las interfaces de salida de los componentes (4.2 y 4.3) para completar el campo «Servicio e interfaz del componente que implementa». |
| Fabio | Las firmas públicas del servicio no cambian. `ProductoExceptionHandler` y el controlador deben cubrir `IllegalArgumentException`, `ProductoNoEncontradoException` y errores técnicos de Spring (por ejemplo, `ObjectOptimisticLockingFailureException` en cambios concurrentes), que el servicio no convierte. Los mensajes de validación propuestos son un texto base que se puede ajustar. |
| Valeria | Las llamadas servicio → repositorio de la tabla anterior son las que deben aparecer en las secuencias. Orden de comprobaciones en `actualizar`: ID válido, existencia del producto y después nombre y precio, tal como lo enumera el contrato. En `crear` se validan los datos antes de guardar. |
