package co.edu.uq.sga.gui;

import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.Alojamiento;
import co.edu.uq.sga.servicio.Reportes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/** Estadísticas: ocupación, ingresos, canales, conflictos y preparación. */
public class EstadisticasPanel extends JPanel implements Actualizable {

    private final Contexto ctx;
    private final JTextField txtDesde = new JTextField(10);
    private final JTextField txtHasta = new JTextField(10);
    private final GraficoBarras graficoOcupacion = new GraficoBarras("Ocupación por apartamento (% de noches)", " %", Estilo.AZUL);
    private final GraficoBarras graficoTemporada = new GraficoBarras("Noches vendidas por temporada", "", Estilo.VERDE);
    private final GraficoBarras graficoCanal = new GraficoBarras("Reservas por canal", "", Estilo.AMARILLO);
    private final DefaultTableModel modeloCanal = Estilo.modelo("Canal", "Total", "Pendiente", "Confirmada", "En curso",
            "Finalizada", "Cancelada", "No-show");
    private final JLabel lblResumen = new JLabel();
    private boolean fechasIniciadas;

    public EstadisticasPanel(Contexto ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtro.add(new JLabel("Periodo — desde:"));
        filtro.add(txtDesde);
        filtro.add(new JLabel("hasta:"));
        filtro.add(txtHasta);
        filtro.add(Estilo.boton("Calcular", () -> ctx.ejecutar(this, this::calcular)));
        add(filtro, BorderLayout.NORTH);

        JPanel graficos = new JPanel(new GridLayout(0, 1, 8, 8));
        graficos.add(graficoOcupacion);
        graficos.add(graficoTemporada);
        graficos.add(graficoCanal);
        JPanel izq = new JPanel(new BorderLayout());
        izq.add(graficos, BorderLayout.NORTH);
        JScrollPane spIzq = new JScrollPane(izq);
        spIzq.getVerticalScrollBar().setUnitIncrement(16);

        JPanel der = new JPanel(new BorderLayout(8, 8));
        JScrollPane spTabla = new JScrollPane(Estilo.tabla(modeloCanal));
        spTabla.setPreferredSize(new Dimension(520, 130));
        der.add(Estilo.titulado("Reservas por canal y estado (incluye canceladas y no-show)", spTabla), BorderLayout.NORTH);
        lblResumen.setVerticalAlignment(SwingConstants.TOP);
        lblResumen.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        der.add(Estilo.titulado("Resumen del periodo", lblResumen), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spIzq, der);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void actualizar() {
        if (!fechasIniciadas) {
            txtDesde.setText(ctx.aloj().hoy().toString());
            txtHasta.setText(ctx.aloj().hoy().plusDays(30).toString());
            fechasIniciadas = true;
        }
        try {
            calcular();
        } catch (RuntimeException ex) {
            lblResumen.setText("<html>" + ex.getMessage() + "</html>");
        }
    }

    private void calcular() {
        Alojamiento aloj = ctx.aloj();
        Reportes rep = new Reportes(aloj);
        LocalDate desde = Dialogos.fecha(txtDesde.getText());
        LocalDate hasta = Dialogos.fecha(txtHasta.getText());
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("«Hasta» no puede ser anterior a «desde»");

        int n = aloj.getCatalogo().tamanio();
        String[] et = new String[n];
        double[] val = new double[n];
        int i = 0;
        for (Apartamento a : aloj.getCatalogo()) {
            et[i] = a.getCodigo();
            val[i++] = rep.porcentajeOcupacion(a, desde, hasta.plusDays(1));
        }
        graficoOcupacion.setDatos(et, val);

        int[] nt = rep.nochesPorTemporada();
        graficoTemporada.setDatos(new String[]{"Baja", "Media", "Alta"}, new double[]{nt[0], nt[1], nt[2]});

        Canal[] canales = Canal.values();
        String[] ec = new String[canales.length];
        double[] vc = new double[canales.length];
        modeloCanal.setRowCount(0);
        for (int k = 0; k < canales.length; k++) {
            ec[k] = canales[k].name();
            vc[k] = rep.reservasPorCanal(canales[k]);
            Object[] fila = new Object[8];
            fila[0] = canales[k];
            fila[1] = rep.reservasPorCanal(canales[k]);
            EstadoReserva[] estados = EstadoReserva.values();
            for (int s = 0; s < estados.length; s++) fila[2 + s] = rep.reservasPorCanalYEstado(canales[k], estados[s]);
            modeloCanal.addRow(fila);
        }
        graficoCanal.setDatos(ec, vc);

        lblResumen.setText("<html><table cellpadding='3'>"
                + "<tr><td>Ingresos (pagos netos) del periodo:</td><td><b>" + Estilo.dinero(rep.ingresos(desde, hasta)) + "</b></td></tr>"
                + "<tr><td>Conflictos de canal registrados:</td><td><b>" + rep.conflictosDeCanal() + "</b></td></tr>"
                + "<tr><td>Tiempo promedio de preparación:</td><td><b>" + String.format("%.1f min", rep.tiempoPromedioPreparacionMinutos()) + "</b></td></tr>"
                + "<tr><td>Preparaciones completadas:</td><td><b>" + aloj.getPreparadas().tamanio() + "</b></td></tr>"
                + "<tr><td>Reservas registradas:</td><td><b>" + aloj.getReservas().tamanio() + "</b></td></tr>"
                + "</table></html>");
    }
}
