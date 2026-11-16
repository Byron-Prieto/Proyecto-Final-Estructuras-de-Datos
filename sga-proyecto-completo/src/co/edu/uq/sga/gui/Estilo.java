package co.edu.uq.sga.gui;

import co.edu.uq.sga.modelo.EstadoApartamento;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Locale;

public final class Estilo {
    public static final Color VERDE = new Color(46, 160, 67);
    public static final Color AZUL = new Color(33, 128, 226);
    public static final Color AMARILLO = new Color(245, 190, 30);
    public static final Color ROJO = new Color(220, 60, 55);
    public static final Color RUTA = new Color(255, 112, 0);

    private Estilo() {
    }

    public static Color colorEstado(EstadoApartamento e) {
        return switch (e) {
            case PREPARADO -> VERDE;
            case OCUPADO -> AZUL;
            case PENDIENTE_PREPARACION, EN_PREPARACION -> AMARILLO;
            case FUERA_DE_SERVICIO -> ROJO;
        };
    }

    /** Negro o blanco según el brillo del fondo. */
    public static Color textoSobre(Color fondo) {
        double l = 0.299 * fondo.getRed() + 0.587 * fondo.getGreen() + 0.114 * fondo.getBlue();
        return l > 150 ? Color.BLACK : Color.WHITE;
    }

    public static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    public static JTable tabla(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(24);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    public static JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> accion.run());
        return b;
    }

    public static String dinero(double v) {
        return String.format(Locale.of("es", "CO"), "$%,.0f", v);
    }

    public static JComponent titulado(String titulo, JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), titulo,
                TitledBorder.LEFT, TitledBorder.TOP));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public static JLabel leyenda(Color color, String texto) {
        JLabel l = new JLabel("  " + texto + "  ");
        l.setOpaque(true);
        l.setBackground(color);
        l.setForeground(textoSobre(color));
        return l;
    }
}
