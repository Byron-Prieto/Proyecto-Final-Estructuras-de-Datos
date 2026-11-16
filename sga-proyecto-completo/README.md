# SGA — Sistema de Gestión de Alojamiento (Aparta-Suites UQ)

Requiere Java 21. Desde esta carpeta:

    # compilar
    mkdir -p out
    javac -encoding UTF-8 -d out $(find src -name '*.java')

    # ejecutar la aplicación (Swing)
    java -cp out co.edu.uq.sga.gui.Main

Ejecútelo desde esta carpeta para que el botón «Cargar escenario de ejemplo» encuentre `datos/escenario.txt`.

## Guardado de datos

La aplicación guarda sola, después de cada operación y al cerrar, en `datos/sga.dat`, y lo carga al abrir.
Se puede forzar con «Guardar ahora». «Cargar escenario» y «Vaciar datos» reemplazan lo guardado (piden confirmación).
Si el archivo no se puede leer (por ejemplo, de otra versión del programa), se conserva como `datos/sga.dat.ilegible`
y la aplicación inicia vacía. `datos/sga.dat` está en `.gitignore`: no se sube al repositorio.

## Pruebas rápidas por consola (sin interfaz)

    java -cp out Prueba            # estructuras de datos
    java -cp out PruebaDominio     # modelo y reglas
    java -cp out PruebaServicios   # servicios de punta a punta (usa datos/escenario.txt)
    java -cp out PruebaAdmin       # operaciones de administración
    java -cp out PruebaPersistencia  # guardar/cargar (fidelidad y volumen)
    java -cp out PruebaContexto    # simula cerrar y reabrir la aplicación

## Paquetes

- `estructuras`: estructuras propias genéricas (lista doble, cola, pila, cola de prioridad, ABB, grafo, conjunto, lista circular).
- `excepciones`: excepciones propias.
- `modelo`: Apartamento, Huesped, Reserva, Estancia, Folio, comparadores, etc.
- `servicio`: Alojamiento (fachada), Reportes, CargadorDatos, RelojAjustable.
- `gui`: interfaz Swing.

Formato del archivo de escenario: ver comentario de `CargadorDatos` y `datos/escenario.txt`.

## Diagramas

Ver la carpeta `diagramas/` (clases, estructuras propias, paquetes y excepciones). El PDF `diagramas/Diagramas_SGA.pdf` los reúne todos.
