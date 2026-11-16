package co.edu.uq.sga.gui;

import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.Alojamiento;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Diálogos y utilidades de captura compartidos por los paneles. */
public final class Dialogos {
    private Dialogos() {
    }

    public static LocalDate fecha(String texto) {
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Fecha inválida «" + texto.trim() + "». Use el formato AAAA-MM-DD");
        }
    }

    /** Una persona por línea: «Nombre, AAAA-MM-DD». */
    public static ListaEnlazada<Ocupante> ocupantes(String texto) {
        ListaEnlazada<Ocupante> r = new ListaEnlazada<>();
        for (String linea : texto.split("\\R")) {
            if (linea.isBlank()) continue;
            String[] p = linea.split("[,;]");
            if (p.length < 2) throw new IllegalArgumentException("Ocupante mal escrito: «" + linea.trim()
                    + "». Use «Nombre, AAAA-MM-DD»");
            r.agregarFinal(new Ocupante(p[0].trim(), fecha(p[1])));
        }
        return r;
    }

    /** n adultos genéricos (para pruebas rápidas). */
    public static ListaEnlazada<Ocupante> adultos(int n) {
        ListaEnlazada<Ocupante> r = new ListaEnlazada<>();
        for (int i = 1; i <= n; i++) r.agregarFinal(new Ocupante("Ocupante " + i, LocalDate.of(1990, 1, 1)));
        return r;
    }

    /** Pide los datos del perfil y registra al huésped. Devuelve null si se cancela. */
    public static Huesped nuevoHuesped(Component padre, Alojamiento aloj) {
        JTextField doc = new JTextField(15), nombre = new JTextField(15), nac = new JTextField("1990-01-01", 15),
                tel = new JTextField(15), correo = new JTextField(15), foto = new JTextField(15);
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Documento:")); p.add(doc);
        p.add(new JLabel("Nombre:")); p.add(nombre);
        p.add(new JLabel("Nacimiento (AAAA-MM-DD):")); p.add(nac);
        p.add(new JLabel("Teléfono:")); p.add(tel);
        p.add(new JLabel("Correo:")); p.add(correo);
        p.add(new JLabel("Fotografía (ruta, opcional):")); p.add(foto);
        if (JOptionPane.showConfirmDialog(padre, p, "Nuevo huésped", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return null;
        }
        Huesped h = new Huesped(doc.getText().trim(), nombre.getText().trim(), fecha(nac.getText()),
                tel.getText().trim(), correo.getText().trim());
        if (!foto.getText().isBlank()) h.setFotografia(foto.getText().trim());
        aloj.registrarHuesped(h);
        return h;
    }

    public static void cotizacion(Component padre, Alojamiento aloj, Apartamento a, Estancia e,
                                  ListaEnlazada<Ocupante> ocupantes) {
        ListaEnlazada<LineaLiquidacion> lineas = aloj.cotizar(a, e, ocupantes);
        DefaultTableModel m = Estilo.modelo("Noche", "Temporada", "Tarifa p/p", "Pagantes", "Valor");
        for (LineaLiquidacion l : lineas) {
            m.addRow(new Object[]{l.noche(), l.temporada(), Estilo.dinero(l.tarifa()), l.pagantes(), Estilo.dinero(l.valor())});
        }
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.add(new JLabel("<html><b>" + a + "</b> · " + e + " · " + ocupantes.tamanio() + " personas (los menores de 6 años no pagan)</html>"),
                BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(Estilo.tabla(m));
        sp.setPreferredSize(new Dimension(520, Math.min(320, 60 + 24 * Math.max(1, lineas.tamanio()))));
        p.add(sp, BorderLayout.CENTER);
        p.add(new JLabel("Total: " + Estilo.dinero(co.edu.uq.sga.modelo.Liquidador.total(lineas))), BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(padre, p, "Cotización noche por noche", JOptionPane.PLAIN_MESSAGE);
    }

    public static void folio(Component padre, Reserva r) {
        DefaultTableModel m = Estilo.modelo("Fecha", "Tipo", "Monto", "Concepto");
        for (Movimiento mv : r.getFolio().getMovimientos()) {
            m.addRow(new Object[]{mv.getFecha().toLocalDate() + " " + mv.getFecha().toLocalTime().withNano(0),
                    mv.getTipo(), Estilo.dinero(mv.getMonto()), mv.getConcepto()});
        }
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.add(new JLabel("<html>Folio de <b>" + r.getId() + "</b> · " + r.getApartamento().getCodigo() + " · " + r.getTitular().getNombre() + "</html>"),
                BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(Estilo.tabla(m));
        sp.setPreferredSize(new Dimension(620, 240));
        p.add(sp, BorderLayout.CENTER);
        Folio f = r.getFolio();
        p.add(new JLabel("Cargos: " + Estilo.dinero(f.totalCargos()) + "   Pagado: " + Estilo.dinero(f.totalPagado())
                + "   Saldo: " + Estilo.dinero(f.saldo())), BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(padre, p, "Folio", JOptionPane.PLAIN_MESSAGE);
    }

    /** Pide concepto y monto. Devuelve {concepto, monto} o null. */
    public static Object[] concepto(Component padre, String titulo) {
        JTextField concepto = new JTextField(18), monto = new JTextField(10);
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Concepto:")); p.add(concepto);
        p.add(new JLabel("Monto:")); p.add(monto);
        if (JOptionPane.showConfirmDialog(padre, p, titulo, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;
        try {
            double v = Double.parseDouble(monto.getText().trim().replace(",", "."));
            return new Object[]{concepto.getText().trim().isEmpty() ? titulo : concepto.getText().trim(), v};
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El monto debe ser un número");
        }
    }
}
