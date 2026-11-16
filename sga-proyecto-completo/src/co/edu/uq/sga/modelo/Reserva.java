package co.edu.uq.sga.modelo;

import java.io.Serializable;
import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.excepciones.CapacidadExcedidaException;
import co.edu.uq.sga.excepciones.TransicionEstadoInvalidaException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reserva implements Comparable<Reserva>, Serializable {
    private static final long serialVersionUID = 1L;


    public static final double PORCENTAJE_ANTICIPO = 0.30;
    public static final int HORAS_VENCIMIENTO = 24;

    private final String id;
    private final Apartamento apartamento;
    private final Huesped titular;
    private final ListaEnlazada<Ocupante> ocupantes;
    private final Estancia estancia;
    private final Canal canal;
    private final LocalDateTime creadaEn;
    private final Folio folio = new Folio();
    private EstadoReserva estado = EstadoReserva.PENDIENTE;
    private LocalTime horaLlegadaEstimada = LocalTime.of(15, 0);

    /** {@code ocupantes} son todas las personas que se alojan (incluido el titular si se hospeda). */
    public Reserva(String id, Apartamento apartamento, Huesped titular, ListaEnlazada<Ocupante> ocupantes,
                   Estancia estancia, Canal canal, LocalDateTime creadaEn) {
        if (ocupantes == null || ocupantes.estaVacia()) throw new IllegalArgumentException("Debe haber al menos un ocupante");
        if (ocupantes.tamanio() > apartamento.getCapacidad()) {
            throw new CapacidadExcedidaException("El apartamento " + apartamento.getCodigo() + " admite máximo "
                    + apartamento.getCapacidad() + " personas y se registraron " + ocupantes.tamanio());
        }
        this.id = id;
        this.apartamento = apartamento;
        this.titular = titular;
        this.ocupantes = ocupantes;
        this.estancia = estancia;
        this.canal = canal;
        this.creadaEn = creadaEn;
    }

    public void cambiarEstado(EstadoReserva nuevo) {
        if (!estado.puedePasarA(nuevo)) {
            throw new TransicionEstadoInvalidaException("Reserva " + id + ": no se puede pasar de " + estado + " a " + nuevo);
        }
        estado = nuevo;
    }

    /** Ocupantes que pagan (los menores de 6 años en la fecha de entrada no). */
    public int pagantes() {
        int n = 0;
        for (Ocupante o : ocupantes) if (o.pagaEn(estancia.getEntrada())) n++;
        return n;
    }

    public LocalDateTime getVencimiento() {
        return creadaEn.plusHours(HORAS_VENCIMIENTO);
    }

    public double anticipoRequerido(double total) {
        return total * PORCENTAJE_ANTICIPO;
    }

    /** Cuánto se devolvería si se cancelara hoy (para mostrarlo antes de confirmar). */
    public double calcularReembolso(LocalDate hoy) {
        return folio.totalPagado() * PoliticaCancelacion.porcentajeReembolso(hoy, estancia.getEntrada());
    }

    public String getId() {
        return id;
    }

    public Apartamento getApartamento() {
        return apartamento;
    }

    public Huesped getTitular() {
        return titular;
    }

    public ListaEnlazada<Ocupante> getOcupantes() {
        return ocupantes;
    }

    public Estancia getEstancia() {
        return estancia;
    }

    public Canal getCanal() {
        return canal;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public Folio getFolio() {
        return folio;
    }

    public LocalTime getHoraLlegadaEstimada() {
        return horaLlegadaEstimada;
    }

    public void setHoraLlegadaEstimada(LocalTime hora) {
        this.horaLlegadaEstimada = hora;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    /** Orden natural: por fecha de entrada (desempata por id para ser consistente con equals). */
    @Override
    public int compareTo(Reserva o) {
        int c = estancia.getEntrada().compareTo(o.estancia.getEntrada());
        return c != 0 ? c : id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Reserva r && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " [" + apartamento.getCodigo() + ", " + estancia + ", " + estado + "]";
    }
}
