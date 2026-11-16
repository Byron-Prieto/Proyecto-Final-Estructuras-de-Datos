package co.edu.uq.sga.servicio;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ConflictoCanal(SolicitudExterna solicitud, String motivo, LocalDateTime fecha) implements Serializable {
}
