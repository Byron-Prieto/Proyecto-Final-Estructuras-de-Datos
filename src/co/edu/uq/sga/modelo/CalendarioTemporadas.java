package co.edu.uq.sga.modelo;

import java.io.Serializable;
import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.excepciones.FechasInvalidasException;

import java.time.LocalDate;

/** Fechas de cada temporada. Lo no definido es Baja (temporada base). */
public class CalendarioTemporadas implements Serializable {
    private static final long serialVersionUID = 1L;


    public record Periodo(LocalDate inicio, LocalDate fin, Temporada temporada) implements Serializable {
        boolean contiene(LocalDate f) {
            return !f.isBefore(inicio) && !f.isAfter(fin);
        }
    }

    private final ListaEnlazada<Periodo> periodos = new ListaEnlazada<>();

    /** Define un periodo (ambos extremos inclusive). No puede cruzarse con otro. */
    public void definir(LocalDate inicio, LocalDate fin, Temporada temporada) {
        if (inicio == null || fin == null || temporada == null) throw new FechasInvalidasException("Datos obligatorios");
        if (fin.isBefore(inicio)) throw new FechasInvalidasException("El fin no puede ser anterior al inicio");
        for (Periodo p : periodos) {
            if (!inicio.isAfter(p.fin()) && !p.inicio().isAfter(fin)) {
                throw new FechasInvalidasException("El periodo se cruza con otro ya definido");
            }
        }
        periodos.agregarFinal(new Periodo(inicio, fin, temporada));
    }

    public boolean eliminar(Periodo p) {
        return periodos.eliminar(p);
    }

    public Temporada temporadaDe(LocalDate fecha) {
        for (Periodo p : periodos) if (p.contiene(fecha)) return p.temporada();
        return Temporada.BAJA;
    }

    public ListaEnlazada<Periodo> getPeriodos() {
        return periodos;
    }
}
