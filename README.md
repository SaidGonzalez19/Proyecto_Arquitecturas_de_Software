# Proyecto_Arquitecturas_de_Software
Repositorio oficial del equipo para las entregas del proyecto de la asignatura Arquitecturas de Software.

## Miembros del equipo

<table border="1">
  <tr>
    <td align="center"><img src="imagenes/foto_valeria.jpeg" alt="Valeria" width="150"/></td>
    <td align="center"><img src="imagenes/foto_fabio.jpeg" alt="Fabio" width="150"/></td>
    <td align="center"><img src="imagenes/foto_said.jpeg" alt="Said" width="150"/></td>
    <td align="center"><img src="imagenes/foto_ramon.jpeg" alt="Ramón" width="150"/></td>
  </tr>
  <tr>
    <td align="center"><b>Couoh Concha Valeria Fernanda</b></td>
    <td align="center"><b>Gonzalez Alcocer Fabio Leonardo</b></td>
    <td align="center"><b>Gonzalez Chablé Said Alfredo</b></td>
    <td align="center"><b>Pacheco Pacheco Ramón Alonso</b></td>
  </tr>
</table>

## Documentación

- [Etapa 1. Definición del proyecto y acuerdos de trabajo](docs/01-definicion-proyecto.md): alcance del Gestor de Productos, requisitos, arquitectura prevista, responsabilidades y convenciones del equipo.
- [Entorno de desarrollo](docs/02-entorno-desarrollo.md): versiones, conexión local a MySQL y ejecución con Maven e IntelliJ IDEA.

## Ejecución

Requiere JDK 17 y MySQL 8.4.13. Ejecutar `sql/00-crear-base.sql` y copiar `application-local.properties.example` como `application-local.properties`, completando los datos de conexión.

Desde la raíz del repositorio, con MySQL iniciado:

```bash
# macOS
./mvnw clean verify
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

La guía de entorno explica la configuración y las comprobaciones de esta base inicial.
