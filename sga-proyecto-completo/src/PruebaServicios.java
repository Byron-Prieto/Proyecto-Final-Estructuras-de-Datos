import co.edu.uq.sga.estructuras.*;
import co.edu.uq.sga.excepciones.*;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.*;

import java.nio.file.Path;
import java.time.*;

public class PruebaServicios {
    static void t(String s) { System.out.println("\n== " + s); }

    public static void main(String[] args) throws Exception {
        RelojAjustable reloj = new RelojAjustable(LocalDateTime.of(2026, 11, 11, 9, 0));
        Alojamiento aloj = new Alojamiento(reloj);
        int n = CargadorDatos.cargar(aloj, Path.of("datos/escenario.txt"));
        System.out.println("líneas cargadas: " + n + ", reservas: " + aloj.getReservas().tamanio());

        t("Disponibilidad (4 personas, +1..+3) por precio");
        Estancia e = new Estancia(aloj.hoy().plusDays(1), aloj.hoy().plusDays(3));
        for (Apartamento a : aloj.buscarDisponibles(e, 4, Comparadores.apartamentosPorPrecio(Temporada.BAJA))) System.out.println("  " + a + " $" + a.getTarifa(Temporada.BAJA));

        t("Portal: pendiente, anticipo y vencimiento");
        Reserva r1 = aloj.buscarReserva("R-0001");
        System.out.println(r1 + " cargos=" + r1.getFolio().totalCargos() + " pagantes=" + r1.pagantes());
        System.out.println("vencimiento=" + r1.getVencimiento());
        reloj.avanzar(Duration.ofHours(25));
        System.out.println("vencidas: " + aloj.procesarVencimientos() + " → " + r1.getEstado() + ", saldo=" + r1.getFolio().saldo());
        System.out.println("A101 libre de nuevo? " + aloj.buscarApartamento("A101").estaLibre(r1.getEstancia()));

        t("Portal nueva + anticipo + cancelación con reembolso");
        Huesped h = aloj.buscarHuesped("1001");
        ListaEnlazada<Ocupante> oc = new ListaEnlazada<>(); oc.agregarFinal(new Ocupante("Laura", LocalDate.of(1995, 5, 5))); oc.agregarFinal(new Ocupante("Ana", LocalDate.of(2000, 1, 1)));
        Reserva r5 = aloj.crearReserva(h, aloj.buscarApartamento("B101"), new Estancia(aloj.hoy().plusDays(10), aloj.hoy().plusDays(12)), oc, Canal.PORTAL);
        System.out.println(r5.getEstado() + " anticipo=" + aloj.pagarAnticipo(r5) + " → " + r5.getEstado());
        System.out.println("reembolso si cancela hoy: " + aloj.reembolsoSiCancela(r5) + " → devuelto " + aloj.cancelarReserva(r5) + " saldo=" + r5.getFolio().saldo() + " estado=" + r5.getEstado());

        t("Check-in / check-out / cola de preparación");
        Huesped h2 = aloj.buscarHuesped("1002");
        Reserva hoyR = aloj.crearReserva(h2, aloj.buscarApartamento("A202"), new Estancia(aloj.hoy(), aloj.hoy().plusDays(2)), oc, Canal.DIRECTA);
        aloj.checkIn(hoyR); System.out.println("A202: " + aloj.buscarApartamento("A202").getEstado());
        try { aloj.checkIn(aloj.buscarReserva("R-0002")); } catch (RuntimeException ex) { System.out.println("OK: " + ex.getClass().getSimpleName() + " - " + ex.getMessage()); }
        Reserva hoyR2 = aloj.crearReserva(h2, aloj.buscarApartamento("B201"), new Estancia(aloj.hoy(), aloj.hoy().plusDays(1)), oc, Canal.DIRECTA);
        aloj.buscarApartamento("B201").cambiarEstado(EstadoApartamento.OCUPADO); // simulamos que está ocupado (no preparado)
        try { aloj.checkIn(hoyR2); } catch (ApartamentoNoPreparadoException ex) { System.out.println("OK no preparado: " + ex.getMessage()); }
        aloj.buscarApartamento("B201").cambiarEstado(EstadoApartamento.PENDIENTE_PREPARACION); // B201 pendiente (con entrada hoy)
        aloj.reordenarColaPreparacion();
        // A202 sale; B201 se encola manualmente por devolverAServicio no aplica → usamos checkout
        aloj.checkOut(hoyR);
        // B201 entra a la cola vía fuera-de-servicio → devolver
        aloj.ponerFueraDeServicio(aloj.buscarApartamento("B201"), "pintura"); aloj.devolverAServicio(aloj.buscarApartamento("B201"));
        for (TareaPreparacion tp : aloj.getColaPreparacion()) System.out.println("  cola: " + tp);
        System.out.println("ruta: " + aloj.rutaPreparacion());
        reloj.avanzar(Duration.ofMinutes(5));
        TareaPreparacion d = aloj.despacharPreparacion(); System.out.println("despachado: " + d + " → " + d.getApartamento().getEstado());
        reloj.avanzar(Duration.ofMinutes(40));
        aloj.terminarPreparacion(d.getApartamento()); System.out.println(d.getApartamento().getCodigo() + ": " + d.getApartamento().getEstado());
        System.out.println("promedio prep (min): " + new Reportes(aloj).tiempoPromedioPreparacionMinutos());

        t("Canal externo (FIFO + conflicto)");
        ListaEnlazada<Ocupante> o1 = new ListaEnlazada<>(); o1.agregarFinal(new Ocupante("Ext1", LocalDate.of(1990, 1, 1)));
        Estancia ee = new Estancia(aloj.hoy().plusDays(2), aloj.hoy().plusDays(4));
        aloj.recibirSolicitudExterna("9001", "Booking Guest 1", "A102", ee, o1);  // choca con R-0002 (+2..+5)
        aloj.recibirSolicitudExterna("9002", "Booking Guest 2", "B202", ee, o1);  // libre
        aloj.recibirSolicitudExterna("9003", "Booking Guest 3", "Z999", ee, o1);  // no existe
        for (ResultadoSolicitud rs : aloj.procesarTodasLasSolicitudes()) System.out.println("  " + (rs.aceptada() ? "OK " + rs.reserva() : "RECHAZADA: " + rs.motivoRechazo()));
        System.out.println("conflictos=" + aloj.getConflictos().tamanio());

        t("Grupo grande (11 personas, +20..+22)");
        ListaEnlazada<Ocupante> grupo = new ListaEnlazada<>();
        for (int i = 0; i < 11; i++) grupo.agregarFinal(new Ocupante("G" + i, LocalDate.of(1985, 1, 1)));
        Estancia eg = new Estancia(aloj.hoy().plusDays(20), aloj.hoy().plusDays(22));
        System.out.println("combinación: " + aloj.asignarGrupo(eg, 11));
        System.out.println("combinación 5 pers: " + aloj.asignarGrupo(eg, 5));
        for (Reserva r : aloj.reservarGrupo(aloj.buscarHuesped("1003"), eg, grupo, Canal.DIRECTA)) System.out.println("  " + r + " ocupantes=" + r.getOcupantes().tamanio());
        try { aloj.asignarGrupo(eg, 100); } catch (CapacidadExcedidaException ex) { System.out.println("OK: " + ex.getMessage()); }

        t("Turnos, grafo y reportes");
        System.out.println(aloj.turnoActual() + " → " + aloj.rotarTurno() + " → " + aloj.rotarTurno() + " → " + aloj.rotarTurno());
        System.out.println("inalcanzables si se cierra ESC-A/A101: " + aloj.inalcanzablesSiSeCierra("ESC-A", "A101"));
        System.out.println("inalcanzables si se cierra RECEPCION/ESC-B (hay ESC-A↔ESC-B): " + aloj.inalcanzablesSiSeCierra("RECEPCION", "ESC-B"));
        System.out.println("bloques: " + aloj.agruparPorBloque());
        Reportes rep = new Reportes(aloj);
        for (Canal c : Canal.values()) System.out.println(c + ": total=" + rep.reservasPorCanal(c) + " canceladas=" + rep.reservasPorCanalYEstado(c, EstadoReserva.CANCELADA));
        int[] nt = rep.nochesPorTemporada(); System.out.println("noches BAJA/MEDIA/ALTA: " + nt[0] + "/" + nt[1] + "/" + nt[2]);
        System.out.println("ocupación A102 próximos 10 días: " + rep.porcentajeOcupacion(aloj.buscarApartamento("A102"), aloj.hoy(), aloj.hoy().plusDays(10)) + "%");
        System.out.println("ingresos hoy: " + rep.ingresos(aloj.hoy(), aloj.hoy()));
        System.out.println("calendario A102: " + aloj.estadoNoche(aloj.buscarApartamento("A102"), aloj.hoy().plusDays(3)) + ", B102: " + aloj.estadoNoche(aloj.buscarApartamento("B102"), aloj.hoy()));
    }
}
