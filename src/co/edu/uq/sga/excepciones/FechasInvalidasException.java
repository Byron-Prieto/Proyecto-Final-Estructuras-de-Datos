package co.edu.uq.sga.excepciones;

public class FechasInvalidasException extends RuntimeException {
    public FechasInvalidasException(String mensaje) {
        super(mensaje);
    }
}
