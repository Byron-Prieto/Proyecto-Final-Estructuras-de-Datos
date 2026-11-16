package co.edu.uq.sga.modelo;

public enum TipoMovimiento {
    CARGO, PAGO, ANULACION_CARGO, ANULACION_PAGO;

    /** Movimiento que compensa a este. */
    public TipoMovimiento contrario() {
        return switch (this) {
            case CARGO -> ANULACION_CARGO;
            case PAGO -> ANULACION_PAGO;
            case ANULACION_CARGO -> CARGO;
            case ANULACION_PAGO -> PAGO;
        };
    }
}
