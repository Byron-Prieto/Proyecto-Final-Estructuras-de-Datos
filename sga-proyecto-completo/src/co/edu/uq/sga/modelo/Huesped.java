package co.edu.uq.sga.modelo;

import java.io.Serializable;
import co.edu.uq.sga.estructuras.Conjunto;
import co.edu.uq.sga.estructuras.ListaEnlazada;

import java.time.LocalDate;
import java.util.Objects;

public class Huesped implements Comparable<Huesped>, Serializable {
    private static final long serialVersionUID = 1L;


    private final String documento;
    private String nombre;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String correo;
    private String fotografia; // ruta opcional
    private final Conjunto<Apartamento> favoritos = new Conjunto<>();
    private final ListaEnlazada<Reserva> historial = new ListaEnlazada<>();

    public Huesped(String documento, String nombre, LocalDate fechaNacimiento, String telefono, String correo) {
        if (documento == null || documento.isBlank()) throw new IllegalArgumentException("Documento obligatorio");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Nombre obligatorio");
        this.documento = documento;
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.correo = correo;
    }

    /** Huésped "llave" solo con documento, para buscar en el ABB. */
    public static Huesped llave(String documento) {
        return new Huesped(documento, "-", null, null, null);
    }

    public boolean agregarFavorito(Apartamento a) {
        return favoritos.agregar(a);
    }

    public boolean quitarFavorito(Apartamento a) {
        return favoritos.eliminar(a);
    }

    public Conjunto<Apartamento> getFavoritos() {
        return favoritos;
    }

    public void agregarAlHistorial(Reserva r) {
        historial.agregarFinal(r);
    }

    public ListaEnlazada<Reserva> getHistorial() {
        return historial;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate f) {
        this.fechaNacimiento = f;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getFotografia() {
        return fotografia;
    }

    public void setFotografia(String ruta) {
        this.fotografia = ruta;
    }

    /** Orden natural: por documento. */
    @Override
    public int compareTo(Huesped o) {
        return documento.compareTo(o.documento);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Huesped h && documento.equals(h.documento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documento);
    }

    @Override
    public String toString() {
        return nombre + " (" + documento + ")";
    }
}
