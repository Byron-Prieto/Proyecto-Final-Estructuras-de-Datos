package co.edu.uq.sga.excepciones;

/** Se lanza al consultar o extraer de una estructura sin elementos. */
public class EstructuraVaciaException extends RuntimeException {
    public EstructuraVaciaException(String mensaje) {
        super(mensaje);
    }
}
