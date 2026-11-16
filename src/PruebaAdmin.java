import co.edu.uq.sga.estructuras.*;
import co.edu.uq.sga.excepciones.*;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.*;

import java.nio.file.Path;

public class PruebaAdmin {
    static void intenta(String que, Runnable r) {
        try { r.run(); System.out.println("  FALLO (no lanzó excepción): " + que); }
        catch (RuntimeException e) { System.out.println("  OK " + que + " → " + e.getClass().getSimpleName() + ": " + e.getMessage()); }
    }

    public static void main(String[] a) throws Exception {
        Alojamiento aloj = new Alojamiento(RelojAjustable.enMarcha());
        CargadorDatos.cargar(aloj, Path.of("datos/escenario.txt"));

        System.out.println("Eliminar con restricciones:");
        intenta("apartamento con reservas activas", () -> aloj.eliminarApartamento(aloj.buscarApartamento("A101")));
        intenta("huésped con reserva activa", () -> aloj.eliminarHuesped(aloj.buscarHuesped("1001")));
        intenta("eliminar recepción", () -> aloj.eliminarNodoMapa("RECEPCION"));
        intenta("eliminar apartamento como nodo", () -> aloj.eliminarNodoMapa("B202"));
        intenta("reducir capacidad bajo ocupantes", () -> aloj.actualizarApartamento(aloj.buscarApartamento("A101"), "x", "A", 1, 2, 2, 1, 1, 1, ""));
        intenta("temporadas que se cruzan", () -> aloj.definirTemporada(java.time.LocalDate.of(2026, 12, 25), java.time.LocalDate.of(2026, 12, 26), Temporada.MEDIA));

        System.out.println("Eliminar apartamento libre (B202) y reindexar el grafo:");
        aloj.eliminarApartamento(aloj.buscarApartamento("B202"));
        System.out.println("  catálogo=" + aloj.getCatalogo().tamanio() + " nodos=" + aloj.getMapa().cantidadVertices()
                + " buscar B202=" + aloj.buscarApartamento("B202"));
        System.out.println("  Dijkstra REC→B201: " + aloj.getMapa().dijkstra("RECEPCION", "B201"));
        System.out.println("  Dijkstra REC→ASEO-2: " + aloj.getMapa().dijkstra("RECEPCION", "ASEO-2"));
        aloj.eliminarNodoMapa("ASEO-2");
        System.out.println("  BFS: " + aloj.getMapa().bfs("RECEPCION"));

        System.out.println("Nuevo apartamento conectado al mapa:");
        Apartamento n = new Apartamento("C101", "Nuevo", "C", 1, 2, 4);
        n.setTarifa(Temporada.BAJA, 50000); aloj.registrarApartamento(n); aloj.agregarPasillo("C101", "ESC-B", 7);
        System.out.println("  Dijkstra REC→C101: " + aloj.getMapa().dijkstra("RECEPCION", "C101"));
        aloj.actualizarApartamento(n, "Renombrado", "C", 2, 3, 6, 51000, 61000, 81000, "WiFi, Jacuzzi");
        System.out.println("  " + n + " cap=" + n.getCapacidad() + " tarifaAlta=" + n.getTarifa(Temporada.ALTA) + " " + n.getCaracteristicas());

        System.out.println("Empleados / turnos:");
        aloj.eliminarEmpleado(aloj.getEmpleados().buscar(new Empleado("E02", "-", Rol.RECEPCIONISTA)));
        for (Empleado e : aloj.getTurnos()) System.out.println("  turno: " + e);
        System.out.println("  altura árboles: apts=" + aloj.getCatalogo().altura() + " huéspedes=" + aloj.getHuespedes().altura());
    }
}
