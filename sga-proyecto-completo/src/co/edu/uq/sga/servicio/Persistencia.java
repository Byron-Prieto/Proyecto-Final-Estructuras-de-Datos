package co.edu.uq.sga.servicio;

import java.io.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.concurrent.Callable;

/**
 * Guarda y carga todo el estado del alojamiento con la serialización de Java.
 * Se escribe primero a un archivo temporal y luego se reemplaza el anterior, para no
 * dejar el archivo a medias si algo falla. La operación corre en un hilo con pila grande
 * porque las estructuras enlazadas se serializan de forma recursiva.
 */
public final class Persistencia {

    private static final long PILA = 256L * 1024 * 1024;

    private Persistencia() {
    }

    public static void guardar(Alojamiento aloj, Path archivo) throws IOException {
        enHiloGrande(() -> {
            Path padre = archivo.toAbsolutePath().getParent();
            if (padre != null) Files.createDirectories(padre);
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temporal)))) {
                out.writeObject(aloj);
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
            return null;
        });
    }

    public static Alojamiento cargar(Path archivo, Clock reloj) throws IOException {
        return enHiloGrande(() -> {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(archivo)))) {
                Alojamiento a = (Alojamiento) in.readObject();
                a.restaurarReloj(reloj);
                return a;
            } catch (ClassNotFoundException | InvalidClassException | ClassCastException ex) {
                throw new IOException("El archivo guardado es de una versión incompatible del programa", ex);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T enHiloGrande(Callable<T> tarea) throws IOException {
        Object[] resultado = new Object[1];
        Throwable[] error = new Throwable[1];
        Thread hilo = new Thread(null, () -> {
            try {
                resultado[0] = tarea.call();
            } catch (Throwable t) {
                error[0] = t;
            }
        }, "persistencia", PILA);
        hilo.start();
        try {
            hilo.join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Operación interrumpida", ex);
        }
        if (error[0] instanceof IOException io) throw io;
        if (error[0] != null) throw new IOException(String.valueOf(error[0].getMessage()), error[0]);
        return (T) resultado[0];
    }
}
