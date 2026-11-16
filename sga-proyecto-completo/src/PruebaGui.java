import co.edu.uq.sga.estructuras.*;
import co.edu.uq.sga.gui.*;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Renderiza los paneles sin pantalla (modo headless) para revisarlos como imagen. */
public class PruebaGui {
    static void layout(Component c) {
        if (c instanceof Container k) {
            k.doLayout();
            for (Component h : k.getComponents()) layout(h);
        }
    }

    static void foto(JComponent p, String nombre, int w, int h) throws Exception {
        p.setSize(w, h);
        layout(p); layout(p);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);
        p.paint(g);
        g.dispose();
        ImageIO.write(img, "png", new File("capturas/" + nombre + ".png"));
        System.out.println("ok " + nombre);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Contexto ctx = new Contexto(RelojAjustable.enMarcha());
        Alojamiento a = ctx.aloj();
        CargadorDatos.cargar(a, Path.of("datos/escenario.txt"));
        Huesped h2 = a.buscarHuesped("1002");
        ListaEnlazada<Ocupante> oc = new ListaEnlazada<>(); oc.agregarFinal(new Ocupante("X", LocalDate.of(1990, 1, 1)));
        Reserva r = a.crearReserva(h2, a.buscarApartamento("A202"), new Estancia(a.hoy(), a.hoy().plusDays(2)), oc, Canal.DIRECTA);
        a.checkIn(r);
        Reserva r2 = a.crearReserva(h2, a.buscarApartamento("B201"), new Estancia(a.hoy(), a.hoy().plusDays(1)), oc, Canal.DIRECTA);
        a.checkIn(r2); a.checkOut(r2);
        a.crearReserva(h2, a.buscarApartamento("B201"), new Estancia(a.hoy(), a.hoy().plusDays(2)), oc, Canal.DIRECTA);
        Reserva r3 = a.crearReserva(h2, a.buscarApartamento("B101"), new Estancia(a.hoy(), a.hoy().plusDays(1)), oc, Canal.DIRECTA);
        a.checkIn(r3); a.checkOut(r3);
        a.buscarReserva("R-0001").getFolio(); // portal pendiente
        a.recibirSolicitudExterna("9001", "Plataforma", "A102", new Estancia(a.hoy().plusDays(2), a.hoy().plusDays(4)), oc);
        a.procesarTodasLasSolicitudes();
        a.buscarHuesped("1001").agregarFavorito(a.buscarApartamento("A201"));
        ctx.setRuta(a.rutaPreparacion());
        System.out.println("ruta: " + ctx.getRuta());

        record P(String n, JComponent c) {}
        P[] ps = {new P("1_tablero", new TableroPanel(ctx)), new P("2_reservas", new ReservasPanel(ctx)),
                new P("3_calendario", new CalendarioPanel(ctx)), new P("4_huesped", new UsuarioPanel(ctx)),
                new P("5_preparacion", new PreparacionPanel(ctx)), new P("6_mapa", new MapaPanel(ctx)),
                new P("7_estadisticas", new EstadisticasPanel(ctx)), new P("8_admin", new AdminPanel(ctx))};
        for (P p : ps) {
            ((Actualizable) p.c()).actualizar();
            foto(p.c(), p.n(), 1100, 680);
        }
    }
}
