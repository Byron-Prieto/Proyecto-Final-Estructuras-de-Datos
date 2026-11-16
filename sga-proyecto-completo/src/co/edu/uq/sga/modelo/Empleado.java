package co.edu.uq.sga.modelo;

import java.io.Serializable;
public class Empleado implements Comparable<Empleado>, Serializable {
    private static final long serialVersionUID = 1L;

    private final String documento;
    private String nombre;
    private final Rol rol;

    public Empleado(String documento, String nombre, Rol rol) {
        if (documento == null || documento.isBlank()) throw new IllegalArgumentException("Documento obligatorio");
        this.documento = documento;
        this.nombre = nombre;
        this.rol = rol;
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

    public Rol getRol() {
        return rol;
    }

    @Override
    public int compareTo(Empleado o) {
        return documento.compareTo(o.documento);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Empleado e && documento.equals(e.documento);
    }

    @Override
    public int hashCode() {
        return documento.hashCode();
    }

    @Override
    public String toString() {
        return nombre + " (" + rol + ")";
    }
}
