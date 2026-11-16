package co.edu.uq.sga.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reloj que se puede adelantar a mano (para probar vencimientos y para la demostración).
 * {@link #enMarcha()} sigue la hora real; el constructor con fecha crea un reloj congelado.
 */
public class RelojAjustable extends Clock {
    private final boolean enMarcha;
    private Instant congelado;
    private Duration desfase = Duration.ZERO;
    private final ZoneId zona = ZoneId.systemDefault();

    /** Reloj congelado en {@code inicio} (útil en pruebas). */
    public RelojAjustable(LocalDateTime inicio) {
        this.enMarcha = false;
        this.congelado = inicio.atZone(zona).toInstant();
    }

    private RelojAjustable() {
        this.enMarcha = true;
    }

    /** Reloj que avanza con la hora real del sistema. */
    public static RelojAjustable enMarcha() {
        return new RelojAjustable();
    }

    public void avanzar(Duration d) {
        if (enMarcha) desfase = desfase.plus(d);
        else congelado = congelado.plus(d);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId z) {
        return this;
    }

    @Override
    public Instant instant() {
        return enMarcha ? Instant.now().plus(desfase) : congelado;
    }
}
