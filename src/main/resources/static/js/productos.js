// Las casillas pertenecen al formulario múltiple aunque estén en las filas de la tabla.
const formularioSeleccion = document.querySelector("#eliminar-seleccion");
const casillas = Array.from(document.querySelectorAll('input[name="ids"]'));
const todos = document.querySelector("#seleccionar-todos");
const contador = document.querySelector("#cantidad-seleccionada");
const aviso = document.querySelector("#aviso-seleccion");
const dialogo = document.querySelector("#confirmacion");
const titulo = document.querySelector("#titulo-confirmacion");
const detalle = document.querySelector("#detalle-confirmacion");
const lista = document.querySelector("#productos-afectados");
const confirmar = document.querySelector("#confirmar");
let formularioPendiente = null;

function actualizarSeleccion() {
    const cantidad = casillas.filter(casilla => casilla.checked).length;
    todos.checked = cantidad > 0 && cantidad === casillas.length;
    todos.indeterminate = cantidad > 0 && cantidad < casillas.length;
    contador.textContent = cantidad === 0 ? "" : cantidad + (cantidad === 1 ? " seleccionado" : " seleccionados");
    if (cantidad > 0) {
        aviso.hidden = true;
    }
}

function abrirConfirmacion(formulario) {
    const individual = formulario !== formularioSeleccion;
    const afectadas = individual
        ? [formulario.closest("tr").querySelector('input[name="ids"]')]
        : casillas.filter(casilla => casilla.checked);

    if (afectadas.length === 0) {
        aviso.hidden = false;
        aviso.scrollIntoView({ block: "nearest" });
        return;
    }

    aviso.hidden = true;
    titulo.textContent = individual ? "Eliminar producto" : "Eliminar productos seleccionados";
    confirmar.textContent = individual ? "Eliminar producto" : "Eliminar seleccionados";
    const cantidad = afectadas.length;
    const seleccionTotal = !individual && cantidad === casillas.length;

    if (individual) {
        detalle.textContent = "¿Deseas eliminar este producto?";
    } else if (seleccionTotal) {
        detalle.textContent = cantidad === 1
            ? "Se eliminará el único producto del listado."
            : "Se eliminarán los " + cantidad + " productos del listado.";
    } else {
        detalle.textContent = cantidad === 1 ? "Se eliminará 1 producto:" : "Se eliminarán " + cantidad + " productos:";
    }

    lista.replaceChildren();
    lista.hidden = seleccionTotal;
    if (!seleccionTotal) {
        for (const casilla of afectadas) {
            const elemento = document.createElement("li");
            elemento.textContent = casilla.dataset.nombre + " (ID " + casilla.value + ")";
            lista.append(elemento);
        }
    }
    formularioPendiente = formulario;
    dialogo.showModal();
}

for (const casilla of casillas) {
    casilla.disabled = false;
    casilla.addEventListener("change", actualizarSeleccion);
}
todos.disabled = casillas.length === 0;
todos.addEventListener("change", () => {
    for (const casilla of casillas) {
        casilla.checked = todos.checked;
    }
    actualizarSeleccion();
});

for (const formulario of document.querySelectorAll("form[data-eliminar]")) {
    formulario.addEventListener("submit", evento => {
        evento.preventDefault();
        abrirConfirmacion(formulario);
    });
    formulario.querySelector('button[type="submit"]').disabled = false;
}

document.querySelector("#cancelar").addEventListener("click", () => dialogo.close());
dialogo.addEventListener("close", () => {
    if (!dialogo.open) {
        formularioPendiente = null;
    }
});
confirmar.addEventListener("click", () => {
    if (formularioPendiente === null) {
        return;
    }
    const formulario = formularioPendiente;
    formularioPendiente = null;
    dialogo.close();
    // El diálogo ya obtuvo la confirmación. submit() envía una sola vez y no vuelve a abrirlo.
    formulario.submit();
});

actualizarSeleccion();
window.addEventListener("pageshow", actualizarSeleccion);
