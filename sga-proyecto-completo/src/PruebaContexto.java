import co.edu.uq.sga.gui.Contexto;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.*;

import java.nio.file.*;
import java.time.LocalDate;

/** Simula cerrar y volver a abrir la aplicación (sin ventanas). */
public class PruebaContexto {
    static void ok(boolean c, String m) { System.out.println((c ? "  OK   " : "  FALLO ") + m); if (!c) System.exit(1); }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Files.deleteIfExists(Path.of("datos/sga.dat"));
        Files.deleteIfExists(Path.of("datos/sga.dat.ilegible"));

        System.out.println("Sesión 1: arranque sin archivo, cargar escenario y registrar un huésped");
        Contexto c1 = new Contexto(RelojAjustable.enMarcha());
        ok(c1.iniciarDesdeArchivo() == null && c1.aloj().getHuespedes().estaVacio(), "inicia vacío");
        Alojamiento nuevo = new Alojamiento(c1.reloj());
        CargadorDatos.cargar(nuevo, Path.of("datos/escenario.txt"));
        c1.reemplazar(nuevo);
        c1.ejecutar(null, () -> c1.aloj().registrarHuesped(new Huesped("7777", "Nuevo Huésped", LocalDate.of(2000, 1, 1), "300", "n@x.co")));
        ok(Files.exists(c1.archivo()), "se creó " + c1.archivo() + " automáticamente (" + c1.estadoGuardado() + ")");

        System.out.println("Sesión 2: se vuelve a abrir la aplicación");
        Contexto c2 = new Contexto(RelojAjustable.enMarcha());
        ok(c2.iniciarDesdeArchivo() == null, "carga sin avisos");
        ok(c2.aloj().buscarHuesped("7777") != null, "el huésped registrado sigue ahí");
        ok(c2.aloj().getReservas().tamanio() == 3, "reservas: " + c2.aloj().getReservas().tamanio());

        System.out.println("Sesión 3: archivo dañado");
        Files.writeString(c2.archivo(), "esto no es un archivo válido");
        Contexto c3 = new Contexto(RelojAjustable.enMarcha());
        String aviso = c3.iniciarDesdeArchivo();
        ok(aviso != null && c3.aloj().getHuespedes().estaVacio(), "avisa y arranca vacío: " + aviso.replace("\n", " "));
        ok(Files.exists(Path.of("datos/sga.dat.ilegible")), "el archivo dañado se conservó aparte");

        Files.deleteIfExists(Path.of("datos/sga.dat"));
        Files.deleteIfExists(Path.of("datos/sga.dat.ilegible"));
    }
}
