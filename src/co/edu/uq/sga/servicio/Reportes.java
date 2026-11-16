package co.edu.uq.sga.servicio;

import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.modelo.*;

import java.time.Duration;
import java.time.LocalDate;

/** Estadísticas obtenidas recorriendo las estructuras del sistema. */
public class Reportes {

    private final Alojamiento alojamiento;

    public Reportes(Alojamiento alojamiento) {
        this.alojamiento = alojamiento;
    }

    /** Reservas que generan ocupación real (no Pendientes, Canceladas ni No-show). */
    private boolean efectiva(Reserva r) {
        EstadoReserva e = r.getEstado();
        return e == EstadoReserva.CONFIRMADA || e == EstadoReserva.EN_CURSO || e == EstadoReserva.FINALIZADA;
    }

    /** Noches ocupadas de un apartamento en [desde, hasta). */
    public int nochesOcupadas(Apartamento a, LocalDate desde, LocalDate hasta) {
        int n = 0;
        for (Reserva r : alojamiento.getReservas()) {
            if (!efectiva(r) || !r.getApartamento().equals(a)) continue;
            for (LocalDate noche : r.getEstancia()) {
                if (!noche.isBefore(desde) && noche.isBefore(hasta)) n++;
            }
        }
        return n;
    }

    public double porcentajeOcupacion(Apartamento a, LocalDate desde, LocalDate hasta) {
        long dias = hasta.toEpochDay() - desde.toEpochDay();
        return dias <= 0 ? 0 : 100.0 * nochesOcupadas(a, desde, hasta) / dias;
    }

    /** Noches ocupadas por temporada (índice = ordinal de Temporada). */
    public int[] nochesPorTemporada() {
        int[] r = new int[Temporada.values().length];
        for (Reserva res : alojamiento.getReservas()) {
            if (!efectiva(res)) continue;
            for (LocalDate noche : res.getEstancia()) r[alojamiento.getCalendario().temporadaDe(noche).ordinal()]++;
        }
        return r;
    }

    /** Pagos netos recibidos entre dos fechas (ambas inclusive). */
    public double ingresos(LocalDate desde, LocalDate hasta) {
        double total = 0;
        for (Reserva r : alojamiento.getReservas()) {
            for (Movimiento m : r.getFolio().getMovimientos()) {
                LocalDate f = m.getFecha().toLocalDate();
                if (f.isBefore(desde) || f.isAfter(hasta)) continue;
                if (m.getTipo() == TipoMovimiento.PAGO) total += m.getMonto();
                else if (m.getTipo() == TipoMovimiento.ANULACION_PAGO) total -= m.getMonto();
            }
        }
        return total;
    }

    public int reservasPorCanal(Canal c) {
        int n = 0;
        for (Reserva r : alojamiento.getReservas()) if (r.getCanal() == c) n++;
        return n;
    }

    public int reservasPorCanalYEstado(Canal c, EstadoReserva e) {
        int n = 0;
        for (Reserva r : alojamiento.getReservas()) if (r.getCanal() == c && r.getEstado() == e) n++;
        return n;
    }

    public int conflictosDeCanal() {
        return alojamiento.getConflictos().tamanio();
    }

    /** Promedio (minutos) entre que se inicia y termina la preparación. */
    public double tiempoPromedioPreparacionMinutos() {
        ListaEnlazada<TareaPreparacion> hechas = alojamiento.getPreparadas();
        if (hechas.estaVacia()) return 0;
        double seg = 0;
        for (TareaPreparacion t : hechas) seg += Duration.between(t.getInicio(), t.getFin()).getSeconds();
        return seg / 60.0 / hechas.tamanio();
    }
}
