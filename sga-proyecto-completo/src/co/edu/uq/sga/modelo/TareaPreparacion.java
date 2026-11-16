package co.edu.uq.sga.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Apartamento esperando preparación. horaLlegada solo aplica si hay entrada hoy. */
public class TareaPreparacion implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Apartamento apartamento;
    private final boolean entradaHoy;
    private final LocalTime horaLlegada;
    private final LocalDateTime encoladoEn;
    private LocalDateTime inicio;
    private LocalDateTime fin;

    public TareaPreparacion(Apartamento apartamento, boolean entradaHoy, LocalTime horaLlegada, LocalDateTime encoladoEn) {
        this.apartamento = apartamento;
        this.entradaHoy = entradaHoy;
        this.horaLlegada = entradaHoy ? horaLlegada : null;
        this.encoladoEn = encoladoEn;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public boolean isEntradaHoy() {
        return entradaHoy;
    }

    public LocalTime getHoraLlegada() {
        return horaLlegada;
    }

    public LocalDateTime getEncoladoEn() {
        return encoladoEn;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TareaPreparacion t && apartamento.equals(t.apartamento);
    }

    @Override
    public int hashCode() {
        return apartamento.hashCode();
    }

    @Override
    public String toString() {
        return apartamento.getCodigo() + (entradaHoy ? " (entrada hoy " + horaLlegada + ")" : "");
    }
}
