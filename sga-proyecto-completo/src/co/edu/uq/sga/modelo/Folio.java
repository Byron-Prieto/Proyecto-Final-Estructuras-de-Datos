package co.edu.uq.sga.modelo;

import java.io.Serializable;
import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.estructuras.Pila;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Cuenta de una reserva. Los movimientos nunca se borran: se corrigen con uno contrario.
 * Solo los cargos y pagos de recepción entran a la pila de "deshacer"; los movimientos
 * automáticos del sistema (cargo inicial de la estancia, devoluciones) no se pueden deshacer.
 */
public class Folio implements Serializable {
    private static final long serialVersionUID = 1L;


    private final ListaEnlazada<Movimiento> movimientos = new ListaEnlazada<>();
    private final Pila<Movimiento> operaciones = new Pila<>();
    private transient Clock reloj = Clock.systemDefaultZone(); // se vuelve a asignar al cargar

    public void setReloj(Clock reloj) {
        this.reloj = reloj;
    }

    private Movimiento nuevo(TipoMovimiento tipo, double monto, String concepto) {
        return new Movimiento(tipo, monto, concepto, LocalDateTime.now(reloj == null ? Clock.systemDefaultZone() : reloj));
    }

    // ---- operaciones de recepción (deshacibles)

    public Movimiento registrarCargo(double monto, String concepto) {
        return registrar(nuevo(TipoMovimiento.CARGO, monto, concepto));
    }

    public Movimiento registrarPago(double monto, String concepto) {
        return registrar(nuevo(TipoMovimiento.PAGO, monto, concepto));
    }

    private Movimiento registrar(Movimiento m) {
        movimientos.agregarFinal(m);
        operaciones.apilar(m);
        return m;
    }

    /**
     * Deshace la última operación registrando el movimiento contrario.
     * @throws co.edu.uq.sga.excepciones.EstructuraVaciaException si no hay operaciones.
     */
    public Movimiento deshacer() {
        Movimiento original = operaciones.desapilar();
        Movimiento contrario = nuevo(original.getTipo().contrario(), original.getMonto(),
                "Anulación: " + original.getConcepto());
        movimientos.agregarFinal(contrario);
        return contrario;
    }

    public boolean hayOperacionesParaDeshacer() {
        return !operaciones.estaVacia();
    }

    // ---- movimientos automáticos del sistema (no deshacibles)

    public Movimiento registrarCargoInicial(double monto, String concepto) {
        Movimiento m = nuevo(TipoMovimiento.CARGO, monto, concepto);
        movimientos.agregarFinal(m);
        return m;
    }

    public Movimiento registrarAnulacionCargo(double monto, String concepto) {
        Movimiento m = nuevo(TipoMovimiento.ANULACION_CARGO, monto, concepto);
        movimientos.agregarFinal(m);
        return m;
    }

    public Movimiento registrarDevolucion(double monto, String concepto) {
        Movimiento m = nuevo(TipoMovimiento.ANULACION_PAGO, monto, concepto);
        movimientos.agregarFinal(m);
        return m;
    }

    // ---- consultas

    /** Cargos netos − pagos netos. */
    public double saldo() {
        return totalCargos() - totalPagado();
    }

    public double totalCargos() {
        double t = 0;
        for (Movimiento m : movimientos) {
            if (m.getTipo() == TipoMovimiento.CARGO) t += m.getMonto();
            else if (m.getTipo() == TipoMovimiento.ANULACION_CARGO) t -= m.getMonto();
        }
        return t;
    }

    public double totalPagado() {
        double t = 0;
        for (Movimiento m : movimientos) {
            if (m.getTipo() == TipoMovimiento.PAGO) t += m.getMonto();
            else if (m.getTipo() == TipoMovimiento.ANULACION_PAGO) t -= m.getMonto();
        }
        return t;
    }

    public ListaEnlazada<Movimiento> getMovimientos() {
        return movimientos;
    }
}
