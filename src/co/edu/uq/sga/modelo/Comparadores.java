package co.edu.uq.sga.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.function.Function;

/** Órdenes alternativos al natural. */
public final class Comparadores {
    private Comparadores() {
    }

    /** Extractor de clave serializable: los comparadores de las colas se guardan junto con ellas. */
    private interface Clave<T, R> extends Function<T, R>, Serializable {
    }

    public static Comparator<Apartamento> apartamentosPorPrecio(Temporada t) {
        return Comparator.comparingDouble((Apartamento a) -> a.getTarifa(t)).thenComparing(Comparator.naturalOrder());
    }

    public static Comparator<Apartamento> apartamentosPorCapacidad() {
        return Comparator.comparingInt(Apartamento::getCapacidad).thenComparing(Comparator.naturalOrder());
    }

    public static Comparator<Apartamento> apartamentosPorDormitorios() {
        return Comparator.comparingInt(Apartamento::getDormitorios).thenComparing(Comparator.naturalOrder());
    }

    public static Comparator<Apartamento> apartamentosPorNumeroReservas() {
        return Comparator.comparingInt(Apartamento::getTotalReservas).thenComparing(Comparator.naturalOrder());
    }

    /** Entrada hoy primero → hora de llegada → orden de encolado. */
    public static Comparator<TareaPreparacion> prioridadPreparacion() {
        return Comparator.comparing((Clave<TareaPreparacion, Boolean>) TareaPreparacion::isEntradaHoy, Comparator.reverseOrder())
                .thenComparing((Clave<TareaPreparacion, LocalTime>) TareaPreparacion::getHoraLlegada,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((Clave<TareaPreparacion, LocalDateTime>) TareaPreparacion::getEncoladoEn);
    }

    /** Reservas pendientes: la que vence antes sale primero. */
    public static Comparator<Reserva> reservasPorVencimiento() {
        return Comparator.comparing((Clave<Reserva, LocalDateTime>) Reserva::getVencimiento)
                .thenComparing(Comparator.naturalOrder());
    }
}
