package co.edu.uq.sga.servicio;

import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.modelo.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Carga un escenario desde un archivo plano (campos separados por ';').
 * Líneas que empiezan con # son comentarios. Las fechas pueden ser ISO (2026-12-20)
 * o relativas a hoy (+3, -1). Tipos de línea:
 * <pre>
 * NODO;nombre
 * APARTAMENTO;codigo;nombre;bloque;piso;dormitorios;capacidad;tarifaBaja;tarifaMedia;tarifaAlta;car1|car2
 * PASILLO;nodoA;nodoB;peso
 * TEMPORADA;inicio;fin;BAJA|MEDIA|ALTA
 * HUESPED;documento;nombre;fechaNacimiento;telefono;correo
 * EMPLEADO;documento;nombre;RECEPCIONISTA|ADMINISTRADOR
 * FUERA;codigoApartamento;motivo
 * RESERVA;documentoTitular;codigoApartamento;entrada;salida;PORTAL|DIRECTA|EXTERNA;Nombre:fechaNac,Nombre:fechaNac
 * </pre>
 */
public final class CargadorDatos {

    private CargadorDatos() {
    }

    public static int cargar(Alojamiento aloj, Path archivo) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return cargar(aloj, br);
        }
    }

    public static int cargar(Alojamiento aloj, Reader lector) throws IOException {
        BufferedReader br = lector instanceof BufferedReader b ? b : new BufferedReader(lector);
        int numero = 0;
        int cargadas = 0;
        String linea;
        while ((linea = br.readLine()) != null) {
            numero++;
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) continue;
            try {
                procesar(aloj, linea.split(";", -1));
                cargadas++;
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Línea " + numero + ": " + ex.getMessage(), ex);
            }
        }
        return cargadas;
    }

    private static void procesar(Alojamiento aloj, String[] c) {
        switch (c[0].toUpperCase()) {
            case "NODO" -> aloj.agregarNodoMapa(c[1]);
            case "APARTAMENTO" -> {
                Apartamento a = new Apartamento(c[1], c[2], c[3], Integer.parseInt(c[4]),
                        Integer.parseInt(c[5]), Integer.parseInt(c[6]));
                a.setTarifa(Temporada.BAJA, Double.parseDouble(c[7]));
                a.setTarifa(Temporada.MEDIA, Double.parseDouble(c[8]));
                a.setTarifa(Temporada.ALTA, Double.parseDouble(c[9]));
                if (c.length > 10 && !c[10].isBlank()) {
                    for (String car : c[10].split("\\|")) a.agregarCaracteristica(car.trim());
                }
                aloj.registrarApartamento(a);
            }
            case "PASILLO" -> aloj.agregarPasillo(c[1], c[2], Double.parseDouble(c[3]));
            case "TEMPORADA" -> aloj.definirTemporada(fecha(aloj, c[1]), fecha(aloj, c[2]),
                    Temporada.valueOf(c[3].toUpperCase()));
            case "HUESPED" -> aloj.registrarHuesped(new Huesped(c[1], c[2], fecha(aloj, c[3]), c[4], c[5]));
            case "EMPLEADO" -> aloj.registrarEmpleado(new Empleado(c[1], c[2], Rol.valueOf(c[3].toUpperCase())));
            case "FUERA" -> aloj.ponerFueraDeServicio(existente(aloj, c[1]), c[2]);
            case "RESERVA" -> {
                Huesped h = aloj.buscarHuesped(c[1]);
                if (h == null) throw new IllegalArgumentException("Huésped inexistente: " + c[1]);
                Estancia e = new Estancia(fecha(aloj, c[3]), fecha(aloj, c[4]));
                ListaEnlazada<Ocupante> oc = new ListaEnlazada<>();
                for (String par : c[6].split(",")) {
                    String[] p = par.split(":");
                    oc.agregarFinal(new Ocupante(p[0].trim(), fecha(aloj, p[1].trim())));
                }
                aloj.crearReserva(h, existente(aloj, c[2]), e, oc, Canal.valueOf(c[5].toUpperCase()));
            }
            default -> throw new IllegalArgumentException("Tipo de línea desconocido: " + c[0]);
        }
    }

    private static Apartamento existente(Alojamiento aloj, String codigo) {
        Apartamento a = aloj.buscarApartamento(codigo);
        if (a == null) throw new IllegalArgumentException("Apartamento inexistente: " + codigo);
        return a;
    }

    private static LocalDate fecha(Alojamiento aloj, String texto) {
        texto = texto.trim();
        if (texto.startsWith("+") || (texto.startsWith("-") && texto.length() <= 4)) {
            return aloj.hoy().plusDays(Long.parseLong(texto.replace("+", "")));
        }
        return LocalDate.parse(texto);
    }
}
