package co.edu.uq.sga.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Movimiento inalterable de un folio. */
public final class Movimiento implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TipoMovimiento tipo;
    private final double monto;
    private final String concepto;
    private final LocalDateTime fecha;

    public Movimiento(TipoMovimiento tipo, double monto, String concepto, LocalDateTime fecha) {
        if (monto <= 0) throw new IllegalArgumentException("El monto debe ser positivo");
        this.tipo = tipo;
        this.monto = monto;
        this.concepto = concepto;
        this.fecha = fecha;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public double getMonto() {
        return monto;
    }

    public String getConcepto() {
        return concepto;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return tipo + " " + monto + " - " + concepto;
    }
}
