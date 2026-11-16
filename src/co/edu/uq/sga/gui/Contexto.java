package co.edu.uq.sga.gui;

import co.edu.uq.sga.estructuras.Ruta;
import co.edu.uq.sga.servicio.Alojamiento;
import co.edu.uq.sga.servicio.Persistencia;
import co.edu.uq.sga.servicio.RelojAjustable;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Estado compartido entre los paneles de la interfaz. */
public class Contexto {
    private Alojamiento aloj;
    private final RelojAjustable reloj;
    private Runnable refrescar = () -> { };
    private Ruta<String> ruta;

    /** Archivo donde se guardan automáticamente los datos. */
    private final Path archivo = Path.of("datos", "sga.dat");
    private LocalDateTime ultimoGuardado;
    private String errorGuardado;

    public Contexto(RelojAjustable reloj) {
        this.reloj = reloj;
        this.aloj = new Alojamiento(reloj);
    }

    public Alojamiento aloj() {
        return aloj;
    }

    public RelojAjustable reloj() {
        return reloj;
    }

    public void reemplazar(Alojamiento nuevo) {
        this.aloj = nuevo;
        this.ruta = null;
    }

    public Path archivo() {
        return archivo;
    }

    /** Guarda todo el estado. Nunca lanza: si falla, deja el error para mostrarlo en la barra. */
    public void guardar() {
        try {
            Persistencia.guardar(aloj, archivo);
            ultimoGuardado = LocalDateTime.now();
            errorGuardado = null;
        } catch (IOException | RuntimeException ex) {
            errorGuardado = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        }
    }

    public String getErrorGuardado() {
        return errorGuardado;
    }

    public String estadoGuardado() {
        if (errorGuardado != null) return "⚠ No se pudo guardar";
        if (ultimoGuardado == null) return "Sin cambios por guardar";
        return "Guardado " + ultimoGuardado.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Al iniciar: carga los datos guardados si existen. Devuelve un mensaje si el archivo no se pudo leer
     * (se conserva aparte como sga.dat.ilegible para no sobrescribirlo), o null si todo salió bien.
     */
    public String iniciarDesdeArchivo() {
        if (!Files.exists(archivo)) return null;
        try {
            reemplazar(Persistencia.cargar(archivo, reloj));
            return null;
        } catch (IOException | RuntimeException ex) {
            try {
                Files.move(archivo, archivo.resolveSibling("sga.dat.ilegible"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignorada) {
                // si ni siquiera se puede mover, se informa igual
            }
            return "No se pudieron cargar los datos guardados (" + ex.getMessage()
                    + ").\nSe inicia sin datos; el archivo anterior quedó como datos/sga.dat.ilegible.";
        }
    }

    public void setRefrescar(Runnable r) {
        this.refrescar = r;
    }

    public void refrescar() {
        refrescar.run();
    }

    /** Ruta resaltada en el mapa (puede ser null). */
    public Ruta<String> getRuta() {
        return ruta;
    }

    public void setRuta(Ruta<String> ruta) {
        this.ruta = ruta;
    }

    public void error(Component padre, Exception ex) {
        String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        JOptionPane.showMessageDialog(padre, msg, "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE);
    }

    public void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "SGA", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Ejecuta una acción mostrando cualquier error de negocio como mensaje y refrescando después. */
    public void ejecutar(Component padre, Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException ex) {
            error(padre, ex);
        }
        guardar(); // guardado automático después de cada operación
        refrescar();
    }
}
