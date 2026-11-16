package co.edu.uq.sga.gui;

import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.Alojamiento;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Inicio: tablero con todos los apartamentos y su estado operativo. */
public class TableroPanel extends JPanel implements Actualizable {

    private final Contexto ctx;
    private final JPanel rejilla = new JPanel(new GridLayout(0, 4, 10, 10));
    private final JLabel resumen = new JLabel();

    public TableroPanel(Contexto ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(resumen, BorderLayout.NORTH);
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        leyenda.add(Estilo.leyenda(Estilo.VERDE, "Preparado"));
        leyenda.add(Estilo.leyenda(Estilo.AZUL, "Ocupado"));
        leyenda.add(Estilo.leyenda(Estilo.AMARILLO, "Pendiente / En preparación"));
        leyenda.add(Estilo.leyenda(Estilo.ROJO, "Fuera de servicio"));
        leyenda.add(new JLabel("   (clic en un apartamento para ver el detalle)"));
        norte.add(leyenda, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.add(rejilla, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(contenedor);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
    }

    @Override
    public void actualizar() {
        Alojamiento aloj = ctx.aloj();
        rejilla.removeAll();
        int[] conteo = new int[EstadoApartamento.values().length];
        if (aloj.getCatalogo().estaVacio()) {
            rejilla.setLayout(new BorderLayout());
            rejilla.add(new JLabel("  No hay datos. Use «Cargar escenario» en la barra superior."), BorderLayout.CENTER);
        } else {
            rejilla.setLayout(new GridLayout(0, 4, 10, 10));
            for (Apartamento a : aloj.getCatalogo()) { // recorrido in-orden del árbol
                rejilla.add(tarjeta(a));
                conteo[a.getEstado().ordinal()]++;
            }
        }
        resumen.setText("<html><b>" + aloj.getCatalogo().tamanio() + " apartamentos</b> — Preparados: "
                + conteo[EstadoApartamento.PREPARADO.ordinal()]
                + " · Ocupados: " + conteo[EstadoApartamento.OCUPADO.ordinal()]
                + " · Por preparar: " + (conteo[EstadoApartamento.PENDIENTE_PREPARACION.ordinal()]
                + conteo[EstadoApartamento.EN_PREPARACION.ordinal()])
                + " · Fuera de servicio: " + conteo[EstadoApartamento.FUERA_DE_SERVICIO.ordinal()] + "</html>");
        rejilla.revalidate();
        rejilla.repaint();
    }

    private JPanel tarjeta(Apartamento a) {
        Color fondo = Estilo.colorEstado(a.getEstado());
        Color texto = Estilo.textoSobre(fondo);
        JPanel t = new JPanel();
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setBackground(fondo);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fondo.darker(), 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel codigo = new JLabel(a.getCodigo());
        codigo.setFont(codigo.getFont().deriveFont(Font.BOLD, 20f));
        t.add(etiqueta(codigo, texto));
        t.add(etiqueta(new JLabel(a.getNombre()), texto));
        t.add(etiqueta(new JLabel("Bloque " + a.getBloque() + " · Piso " + a.getPiso()), texto));
        t.add(etiqueta(new JLabel(a.getDormitorios() + " dorm. · cap. " + a.getCapacidad()), texto));
        JLabel estado = new JLabel(a.getEstado().getEtiqueta());
        estado.setFont(estado.getFont().deriveFont(Font.BOLD));
        t.add(etiqueta(estado, texto));
        if (a.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO) {
            t.add(etiqueta(new JLabel(a.getMotivoFueraDeServicio()), texto));
        } else if (a.getEstado() == EstadoApartamento.OCUPADO) {
            for (Reserva r : a.getReservas()) {
                if (r.getEstado() == EstadoReserva.EN_CURSO) {
                    t.add(etiqueta(new JLabel("Sale el " + r.getEstancia().getSalida()), texto));
                    break;
                }
            }
        }
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detalle(a);
            }
        });
        return t;
    }

    private JLabel etiqueta(JLabel l, Color c) {
        l.setForeground(c);
        return l;
    }

    private void detalle(Apartamento a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getCodigo()).append(" — ").append(a.getNombre()).append('\n');
        sb.append("Bloque ").append(a.getBloque()).append(", piso ").append(a.getPiso()).append('\n');
        sb.append(a.getDormitorios()).append(" dormitorios · capacidad ").append(a.getCapacidad()).append('\n');
        sb.append("Tarifa por persona/noche: Baja ").append(Estilo.dinero(a.getTarifa(Temporada.BAJA)))
                .append(" · Media ").append(Estilo.dinero(a.getTarifa(Temporada.MEDIA)))
                .append(" · Alta ").append(Estilo.dinero(a.getTarifa(Temporada.ALTA))).append('\n');
        sb.append("Características: ").append(a.getCaracteristicas()).append('\n');
        sb.append("Estado: ").append(a.getEstado().getEtiqueta());
        if (a.getMotivoFueraDeServicio() != null) sb.append(" (").append(a.getMotivoFueraDeServicio()).append(')');
        sb.append("\n\nReservas activas (por fecha de entrada):\n");
        boolean alguna = false;
        for (Reserva r : a.getReservas()) {
            sb.append("  ").append(r.getId()).append("  ").append(r.getEstancia()).append("  ")
                    .append(r.getEstado()).append("  ").append(r.getTitular().getNombre()).append('\n');
            alguna = true;
        }
        if (!alguna) sb.append("  (ninguna)\n");
        sb.append("\nReservas registradas en total: ").append(a.getTotalReservas());

        JTextArea area = new JTextArea(sb.toString(), 14, 52);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        boolean fuera = a.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO;
        String[] opciones = {fuera ? "Devolver a servicio" : "Poner fuera de servicio", "Cerrar"};
        int r = JOptionPane.showOptionDialog(this, new JScrollPane(area), "Apartamento " + a.getCodigo(),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[1]);
        if (r != 0) return;
        if (fuera) {
            ctx.ejecutar(this, () -> ctx.aloj().devolverAServicio(a));
        } else {
            String motivo = JOptionPane.showInputDialog(this, "Motivo para poner " + a.getCodigo() + " fuera de servicio:");
            if (motivo != null) ctx.ejecutar(this, () -> ctx.aloj().ponerFueraDeServicio(a, motivo));
        }
    }
}
