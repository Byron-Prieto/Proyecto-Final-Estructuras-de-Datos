package co.edu.uq.sga.modelo;

import java.io.Serializable;
import co.edu.uq.sga.estructuras.ArbolBinarioBusqueda;
import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.excepciones.ReservaSolapadaException;
import co.edu.uq.sga.excepciones.TransicionEstadoInvalidaException;

import java.util.Objects;

public class Apartamento implements Comparable<Apartamento>, Serializable {
    private static final long serialVersionUID = 1L;


    private final String codigo;
    private String nombre;
    private String bloque;
    private int piso;
    private int dormitorios;
    private int capacidad;
    private final ListaEnlazada<String> caracteristicas = new ListaEnlazada<>();
    private final double[] tarifas = new double[Temporada.values().length];
    private EstadoApartamento estado = EstadoApartamento.PREPARADO;
    private String motivoFueraDeServicio;

    /** Reservas ACTIVAS del apartamento, ordenadas por fecha de entrada. */
    private final ArbolBinarioBusqueda<Reserva> reservas = new ArbolBinarioBusqueda<>();
    private int totalReservas;

    public Apartamento(String codigo, String nombre, String bloque, int piso, int dormitorios, int capacidad) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("Código obligatorio");
        if (dormitorios < 1 || capacidad < 1) throw new IllegalArgumentException("Dormitorios y capacidad deben ser positivos");
        this.codigo = codigo;
        this.nombre = nombre;
        this.bloque = bloque;
        this.piso = piso;
        this.dormitorios = dormitorios;
        this.capacidad = capacidad;
    }

    // ------------------------------------------------------------- estado operativo

    public void cambiarEstado(EstadoApartamento nuevo) {
        if (nuevo == EstadoApartamento.FUERA_DE_SERVICIO) {
            throw new IllegalArgumentException("Use ponerFueraDeServicio(motivo) para registrar el motivo");
        }
        validar(nuevo);
        estado = nuevo;
        motivoFueraDeServicio = null;
    }

    public void ponerFueraDeServicio(String motivo) {
        if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        validar(EstadoApartamento.FUERA_DE_SERVICIO);
        estado = EstadoApartamento.FUERA_DE_SERVICIO;
        motivoFueraDeServicio = motivo;
    }

    private void validar(EstadoApartamento nuevo) {
        if (!estado.puedePasarA(nuevo)) {
            throw new TransicionEstadoInvalidaException("Apartamento " + codigo + ": no se puede pasar de "
                    + estado + " a " + nuevo);
        }
    }

    // ------------------------------------------------------------- reservas (ABB + recursión)

    /** Reserva activa que comparte alguna noche con la estancia, o null. Recursivo, O(altura). */
    public Reserva buscarConflicto(Estancia e) {
        return buscarConflicto(reservas.getRaiz(), e);
    }

    private Reserva buscarConflicto(ArbolBinarioBusqueda.Nodo<Reserva> n, Estancia e) {
        if (n == null) return null;
        Reserva r = n.getDato();
        if (r.getEstancia().solapa(e)) return r;
        // Las reservas activas no se cruzan entre sí: si no hay choque, la nueva queda toda a un lado.
        if (!e.getEntrada().isBefore(r.getEstancia().getSalida())) return buscarConflicto(n.getDer(), e);
        return buscarConflicto(n.getIzq(), e);
    }

    public boolean estaLibre(Estancia e) {
        return buscarConflicto(e) == null;
    }

    public void agregarReserva(Reserva r) {
        Reserva choque = buscarConflicto(r.getEstancia());
        if (choque != null) {
            throw new ReservaSolapadaException("El apartamento " + codigo + " ya tiene la reserva "
                    + choque.getId() + " (" + choque.getEstancia() + ")");
        }
        reservas.insertar(r);
        totalReservas++;
    }

    /** Libera las noches de una reserva (cancelada, finalizada, vencida...). */
    public boolean liberarReserva(Reserva r) {
        return reservas.eliminar(r);
    }

    public ArbolBinarioBusqueda<Reserva> getReservas() {
        return reservas;
    }

    // ------------------------------------------------------------- datos

    public double getTarifa(Temporada t) {
        return tarifas[t.ordinal()];
    }

    public void setTarifa(Temporada t, double valor) {
        if (valor < 0) throw new IllegalArgumentException("La tarifa no puede ser negativa");
        tarifas[t.ordinal()] = valor;
    }

    public void editar(String nombre, String bloque, int piso, int dormitorios, int capacidad) {
        if (dormitorios < 1 || capacidad < 1) throw new IllegalArgumentException("Dormitorios y capacidad deben ser positivos");
        this.nombre = nombre;
        this.bloque = bloque;
        this.piso = piso;
        this.dormitorios = dormitorios;
        this.capacidad = capacidad;
    }

    /** Reemplaza las características a partir de un texto separado por comas. */
    public void setCaracteristicas(String csv) {
        caracteristicas.limpiar();
        if (csv == null) return;
        for (String c : csv.split(",")) if (!c.isBlank()) caracteristicas.agregarFinal(c.trim());
    }

    public void agregarCaracteristica(String c) {
        caracteristicas.agregarFinal(c);
    }

    public ListaEnlazada<String> getCaracteristicas() {
        return caracteristicas;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getBloque() {
        return bloque;
    }

    public int getPiso() {
        return piso;
    }

    public int getDormitorios() {
        return dormitorios;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public EstadoApartamento getEstado() {
        return estado;
    }

    public String getMotivoFueraDeServicio() {
        return motivoFueraDeServicio;
    }

    public int getTotalReservas() {
        return totalReservas;
    }

    /** Orden natural: por código. */
    @Override
    public int compareTo(Apartamento o) {
        return codigo.compareTo(o.codigo);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Apartamento a && codigo.equals(a.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
