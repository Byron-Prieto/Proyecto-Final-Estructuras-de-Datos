import co.edu.uq.sga.estructuras.*;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.*;

import java.nio.file.*;
import java.time.*;

public class PruebaPersistencia {
    static void ok(boolean c, String m) { System.out.println((c ? "  OK   " : "  FALLO ") + m); if (!c) System.exit(1); }

    public static void main(String[] args) throws Exception {
        Path f = Path.of("/tmp/sga_prueba/sga.dat");
        RelojAjustable reloj = RelojAjustable.enMarcha();
        Alojamiento a = new Alojamiento(reloj);
        CargadorDatos.cargar(a, Path.of("datos/escenario.txt"));
        Huesped h = a.buscarHuesped("1002");
        ListaEnlazada<Ocupante> oc = new ListaEnlazada<>(); oc.agregarFinal(new Ocupante("X", LocalDate.of(1990, 1, 1)));
        Reserva r = a.crearReserva(h, a.buscarApartamento("A202"), new Estancia(a.hoy(), a.hoy().plusDays(2)), oc, Canal.DIRECTA);
        a.checkIn(r);
        Reserva r2 = a.crearReserva(h, a.buscarApartamento("B201"), new Estancia(a.hoy(), a.hoy().plusDays(1)), oc, Canal.DIRECTA);
        a.checkIn(r2); a.checkOut(r2);
        a.crearReserva(h, a.buscarApartamento("B201"), new Estancia(a.hoy(), a.hoy().plusDays(2)), oc, Canal.DIRECTA);
        a.recibirSolicitudExterna("9001", "Plataforma", "A102", new Estancia(a.hoy().plusDays(2), a.hoy().plusDays(4)), oc);
        a.procesarTodasLasSolicitudes();   // genera un conflicto
        a.recibirSolicitudExterna("9002", "Plataforma 2", "B202", new Estancia(a.hoy().plusDays(5), a.hoy().plusDays(6)), oc); // queda en cola
        Reserva portal = a.buscarReserva("R-0001");
        portal.getFolio().registrarPago(1000, "abono"); portal.getFolio().registrarCargo(500, "minibar");
        a.buscarHuesped("1001").agregarFavorito(a.buscarApartamento("A201"));
        a.rotarTurno();

        long t0 = System.nanoTime();
        Persistencia.guardar(a, f);
        System.out.printf("guardado: %d bytes en %d ms%n", Files.size(f), (System.nanoTime() - t0) / 1_000_000);
        Alojamiento b = Persistencia.cargar(f, reloj);

        System.out.println("Comparación del estado cargado:");
        ok(b.getCatalogo().tamanio() == a.getCatalogo().tamanio(), "apartamentos: " + b.getCatalogo().tamanio());
        ok(b.getHuespedes().tamanio() == a.getHuespedes().tamanio(), "huéspedes: " + b.getHuespedes().tamanio());
        ok(b.getReservas().tamanio() == a.getReservas().tamanio(), "reservas: " + b.getReservas().tamanio());
        ok(b.buscarApartamento("A202").getEstado() == EstadoApartamento.OCUPADO, "A202 sigue Ocupado");
        ok(b.buscarApartamento("B201").getEstado() == EstadoApartamento.PENDIENTE_PREPARACION, "B201 sigue Pendiente de preparación");
        ok(b.buscarApartamento("B102").getMotivoFueraDeServicio().equals("Fuga de agua en el baño"), "motivo de fuera de servicio");
        ok(b.buscarReserva("R-0001").getFolio().saldo() == portal.getFolio().saldo(), "saldo del folio R-0001: " + b.buscarReserva("R-0001").getFolio().saldo());
        ok(b.getConflictos().tamanio() == 1, "conflictos de canal: 1");
        ok(b.solicitudesPendientes() == 1, "solicitud externa aún en cola");
        ok(b.getColaPreparacion().tamanio() == a.getColaPreparacion().tamanio(), "cola de preparación: " + b.getColaPreparacion().tamanio());
        ok(b.getPendientes().tamanio() == a.getPendientes().tamanio(), "reservas pendientes por vencer: " + b.getPendientes().tamanio());
        ok(b.turnoActual().getNombre().equals(a.turnoActual().getNombre()), "turno actual: " + b.turnoActual().getNombre());
        ok(b.buscarHuesped("1001").getFavoritos().contiene(b.buscarApartamento("A201")), "favoritos del huésped");
        ok(b.getMapa().dijkstra("RECEPCION", "A101").distancia() == a.getMapa().dijkstra("RECEPCION", "A101").distancia(), "grafo: misma ruta Dijkstra");
        ok(b.getCalendario().getPeriodos().tamanio() == 2, "temporadas");
        // identidad de objetos preservada: la reserva del apartamento es la misma del huésped
        Reserva enApto = b.buscarApartamento("A202").getReservas().getRaiz().getDato();
        ok(enApto == b.buscarReserva(enApto.getId()), "la misma reserva (no copias) en árbol, lista e historial");
        ok(b.buscarHuesped("1002").getHistorial().contiene(enApto), "historial del huésped apunta a la misma reserva");

        System.out.println("El sistema cargado sigue funcionando:");
        Reserva nueva = b.crearReserva(b.buscarHuesped("1003"), b.buscarApartamento("B202"), new Estancia(b.hoy().plusDays(8), b.hoy().plusDays(9)), oc, Canal.PORTAL);
        ok(nueva.getId().equals("R-0007"), "consecutivo continúa sin repetir ids: " + nueva.getId());
        b.getReservas(); b.checkOut(b.buscarApartamento("A202").getReservas().getRaiz().getDato());
        ok(b.getColaPreparacion().tamanio() == a.getColaPreparacion().tamanio() + 1, "check-out encola preparación");
        b.despacharPreparacion(); // comparador serializado funciona
        ok(b.getEnPreparacion().tamanio() == 1, "despachar con el comparador restaurado");
        double saldo = b.buscarReserva("R-0001").getFolio().saldo();
        b.buscarReserva("R-0001").getFolio().deshacer();
        ok(b.buscarReserva("R-0001").getFolio().saldo() != saldo, "deshacer en folio cargado (reloj restaurado)");
        ok(b.procesarSiguienteSolicitud().aceptada(), "procesa la solicitud que quedó en cola");

        System.out.println("Prueba de volumen:");
        Alojamiento g = new Alojamiento(reloj);
        for (int i = 0; i < 300; i++) { Apartamento ap = new Apartamento(String.format("Z%04d", i), "n", "A", 1, 2, 4); ap.setTarifa(Temporada.BAJA, 40000); g.registrarApartamento(ap); }
        for (int i = 0; i < 3000; i++) g.registrarHuesped(new Huesped(String.format("D%06d", i), "Huésped " + i, LocalDate.of(1990, 1, 1), "300", "x@y.co"));
        int n = 0;
        for (int i = 0; i < 3000; i++) {
            Apartamento ap = g.buscarApartamento(String.format("Z%04d", i % 300));
            int semana = i / 300;
            g.crearReserva(g.buscarHuesped(String.format("D%06d", i)), ap, new Estancia(g.hoy().plusDays(semana * 3L + 1), g.hoy().plusDays(semana * 3L + 3)), oc, i % 2 == 0 ? Canal.PORTAL : Canal.DIRECTA);
            n++;
        }
        t0 = System.nanoTime();
        Persistencia.guardar(g, f);
        long ms1 = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        Alojamiento g2 = Persistencia.cargar(f, reloj);
        long ms2 = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("  %d reservas, %d huéspedes, %d apartamentos → %d KB, guardar %d ms, cargar %d ms%n",
                g2.getReservas().tamanio(), g2.getHuespedes().tamanio(), g2.getCatalogo().tamanio(), Files.size(f) / 1024, ms1, ms2);
        ok(g2.getReservas().tamanio() == 3000, "volumen restaurado sin errores de pila");
    }
}
