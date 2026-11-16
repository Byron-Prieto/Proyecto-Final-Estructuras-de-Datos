package co.edu.uq.sga.modelo;

public enum EstadoReserva {
    PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA, NO_SHOW;

    public boolean puedePasarA(EstadoReserva nuevo) {
        return switch (this) {
            case PENDIENTE -> nuevo == CONFIRMADA || nuevo == CANCELADA;
            case CONFIRMADA -> nuevo == EN_CURSO || nuevo == CANCELADA || nuevo == NO_SHOW;
            case EN_CURSO -> nuevo == FINALIZADA;
            case FINALIZADA, CANCELADA, NO_SHOW -> false;
        };
    }

    /** Una reserva activa bloquea las noches de su apartamento. */
    public boolean esActiva() {
        return this == PENDIENTE || this == CONFIRMADA || this == EN_CURSO;
    }
}
