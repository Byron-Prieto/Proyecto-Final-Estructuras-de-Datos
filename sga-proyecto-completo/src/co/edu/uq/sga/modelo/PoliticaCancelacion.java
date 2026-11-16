package co.edu.uq.sga.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SUPUESTO: el enunciado no define la política de reembolso. Se usa esta escala,
 * fácil de cambiar aquí: 7+ días antes → 100 %, 3 a 6 días → 50 %, menos → 0 %
 * (sobre lo pagado).
 */
public final class PoliticaCancelacion {
    private PoliticaCancelacion() {
    }

    public static double porcentajeReembolso(LocalDate hoy, LocalDate entrada) {
        long dias = ChronoUnit.DAYS.between(hoy, entrada);
        if (dias >= 7) return 1.0;
        if (dias >= 3) return 0.5;
        return 0.0;
    }
}
