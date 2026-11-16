package co.edu.uq.sga.servicio;

import java.io.Serializable;
import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.modelo.Estancia;
import co.edu.uq.sga.modelo.Ocupante;

import java.time.LocalDateTime;

/** Solicitud simulada que envía la plataforma de terceros. */
public class SolicitudExterna implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String documento;
    private final String nombre;
    private final String codigoApartamento;
    private final Estancia estancia;
    private final ListaEnlazada<Ocupante> ocupantes;
    private final LocalDateTime recibidaEn;

    public SolicitudExterna(String documento, String nombre, String codigoApartamento, Estancia estancia,
                            ListaEnlazada<Ocupante> ocupantes, LocalDateTime recibidaEn) {
        this.documento = documento;
        this.nombre = nombre;
        this.codigoApartamento = codigoApartamento;
        this.estancia = estancia;
        this.ocupantes = ocupantes;
        this.recibidaEn = recibidaEn;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCodigoApartamento() {
        return codigoApartamento;
    }

    public Estancia getEstancia() {
        return estancia;
    }

    public ListaEnlazada<Ocupante> getOcupantes() {
        return ocupantes;
    }

    public LocalDateTime getRecibidaEn() {
        return recibidaEn;
    }

    @Override
    public String toString() {
        return nombre + " → " + codigoApartamento + " " + estancia;
    }
}
