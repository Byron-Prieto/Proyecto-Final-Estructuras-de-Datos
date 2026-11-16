package co.edu.uq.sga.servicio;

import co.edu.uq.sga.modelo.Reserva;

/** reserva != null si se aceptó; si no, motivoRechazo explica por qué. */
public record ResultadoSolicitud(SolicitudExterna solicitud, Reserva reserva, String motivoRechazo) {
    public boolean aceptada() {
        return reserva != null;
    }
}
