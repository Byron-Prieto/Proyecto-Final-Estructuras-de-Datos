package co.edu.uq.sga.modelo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;

public class Ocupante implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int EDAD_MINIMA_PAGO = 6;

    private final String nombre;
    private final LocalDate fechaNacimiento;

    public Ocupante(String nombre, LocalDate fechaNacimiento) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Nombre obligatorio");
        if (fechaNacimiento == null) throw new IllegalArgumentException("Fecha de nacimiento obligatoria");
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public int edadEn(LocalDate fecha) {
        return Period.between(fechaNacimiento, fecha).getYears();
    }

    /** Los menores de 6 años ocupan cupo pero no pagan. */
    public boolean pagaEn(LocalDate fecha) {
        return edadEn(fecha) >= EDAD_MINIMA_PAGO;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
