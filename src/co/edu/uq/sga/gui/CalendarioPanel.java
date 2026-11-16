package co.edu.uq.sga.gui;

import co.edu.uq.sga.modelo.Apartamento;
import co.edu.uq.sga.servicio.EstadoNoche;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/** Calendario de ocupación: apartamentos × noches del mes. */
public class CalendarioPanel extends JPanel implements Actualizable {

    private static final Color COLOR_LIBRE = new Color(200, 235, 205);
    private static final Color COLOR_OCUPADA = new Color(33, 128, 226);
    private static final Color COLOR_BLOQUEADA = new Color(150, 150, 150);

    private final Contexto ctx;
    private YearMonth mes = YearMonth.now();
    private boolean mesInicializado;
    private final JLabel titulo = new JLabel("", SwingConstants.CENTER);
    private final JTable tabla = new JTable();
    private Apartamento[] filas = new Apartamento[0];

    public CalendarioPanel(Contexto ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel norte = new JPanel(new BorderLayout());
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        nav.add(Estilo.boton("◀ Mes anterior", () -> { mes = mes.minusMonths(1); actualizar(); }));
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setPreferredSize(new Dimension(200, 24));
        nav.add(titulo);
        nav.add(Estilo.boton("Mes siguiente ▶", () -> { mes = mes.plusMonths(1); actualizar(); }));
        nav.add(Estilo.boton("Hoy", () -> { mes = YearMonth.from(ctx.aloj().hoy()); actualizar(); }));
        norte.add(nav, BorderLayout.NORTH);
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        leyenda.add(Estilo.leyenda(COLOR_LIBRE, "Libre"));
        leyenda.add(Estilo.leyenda(COLOR_OCUPADA, "Ocupada"));
        leyenda.add(Estilo.leyenda(COLOR_BLOQUEADA, "Bloqueada (fuera de servicio)"));
        norte.add(leyenda, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        tabla.setRowHeight(26);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setShowGrid(true);
        tabla.setGridColor(Color.WHITE);
        tabla.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    @Override
    public void actualizar() {
        if (!mesInicializado) {
            mes = YearMonth.from(ctx.aloj().hoy());
            mesInicializado = true;
        }
        int n = ctx.aloj().getCatalogo().tamanio();
        filas = new Apartamento[n];
        int i = 0;
        for (Apartamento a : ctx.aloj().getCatalogo()) filas[i++] = a;
        titulo.setText(mes.getMonth().getDisplayName(TextStyle.FULL, Locale.of("es")) + " " + mes.getYear());

        int dias = mes.lengthOfMonth();
        tabla.setModel(new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return filas.length;
            }

            @Override
            public int getColumnCount() {
                return dias + 1;
            }

            @Override
            public String getColumnName(int c) {
                return c == 0 ? "Apartamento" : String.valueOf(c);
            }

            @Override
            public Object getValueAt(int r, int c) {
                if (c == 0) return filas[r].getCodigo() + " · " + filas[r].getNombre();
                return ctx.aloj().estadoNoche(filas[r], mes.atDay(c));
            }
        });
        tabla.getColumnModel().getColumn(0).setPreferredWidth(170);
        LocalDate hoy = ctx.aloj().hoy();
        DefaultTableCellRenderer render = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, "", false, false, r, c);
                setHorizontalAlignment(CENTER);
                EstadoNoche e = (EstadoNoche) v;
                setBackground(switch (e) {
                    case LIBRE -> COLOR_LIBRE;
                    case OCUPADA -> COLOR_OCUPADA;
                    case BLOQUEADA -> COLOR_BLOQUEADA;
                });
                setBorder(mes.atDay(c).equals(hoy) ? BorderFactory.createMatteBorder(0, 2, 0, 0, Estilo.RUTA) : null);
                return this;
            }
        };
        for (int c = 1; c <= dias; c++) {
            tabla.getColumnModel().getColumn(c).setPreferredWidth(30);
            tabla.getColumnModel().getColumn(c).setCellRenderer(render);
        }
    }
}
