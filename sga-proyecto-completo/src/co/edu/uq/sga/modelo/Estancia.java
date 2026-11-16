package co.edu.uq.sga.modelo;

import java.io.Serializable;
import co.edu.uq.sga.excepciones.FechasInvalidasException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Rango [entrada, salida): la noche de salida no se ocupa ni se cobra. Iterable por noche. */
public class Estancia implements Iterable<LocalDate>, Serializable {
    private static final long serialVersionUID = 1L;


    private final LocalDate entrada;
    private final LocalDate salida;

    public Estancia(LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null) throw new FechasInvalidasException("Las fechas son obligatorias");
        if (!salida.isAfter(entrada)) throw new FechasInvalidasException("La salida debe ser posterior a la entrada");
        this.entrada = entrada;
        this.salida = salida;
    }

    public LocalDate getEntrada() {
        return entrada;
    }

    public LocalDate getSalida() {
        return salida;
    }

    public int noches() {
        return (int) ChronoUnit.DAYS.between(entrada, salida);
    }

    /** Comparten al menos una noche. */
    public boolean solapa(Estancia otra) {
        return entrada.isBefore(otra.salida) && otra.entrada.isBefore(salida);
    }

    public boolean incluyeNoche(LocalDate noche) {
        return !noche.isBefore(entrada) && noche.isBefore(salida);
    }

    @Override
    public Iterator<LocalDate> iterator() {
        return new Iterator<>() {
            private LocalDate actual = entrada;

            @Override
            public boolean hasNext() {
                return actual.isBefore(salida);
            }

            @Override
            public LocalDate next() {
                if (!hasNext()) throw new NoSuchElementException();
                LocalDate n = actual;
                actual = actual.plusDays(1);
                return n;
            }
        };
    }

    @Override
    public String toString() {
        return entrada + " → " + salida + " (" + noches() + " noches)";
    }
}
