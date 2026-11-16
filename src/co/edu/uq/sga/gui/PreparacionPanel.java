package co.edu.uq.sga.gui;

import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.estructuras.Ruta;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import co.edu.uq.sga.modelo.TareaPreparacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/** Cola de preparación en orden de prioridad y ruta sugerida. */
public class PreparacionPanel extends JPanel implements Actualizable {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final Contexto ctx;
    private final DefaultTableModel modeloCola = Estilo.modelo("#", "Apartamento", "Prioridad", "Entrada hoy (hora llegada)", "En cola desde");
    private final DefaultTableModel modeloEnCurso = Estilo.modelo("Apartamento", "Inició");
    private final JTable tablaEnCurso = Estilo.tabla(modeloEnCurso);
    private final JTextArea txtRuta = new JTextArea(5, 40);
    private ListaEnlazada<TareaPreparacion> filasEnCurso = new ListaEnlazada<>();

    public PreparacionPanel(Contexto ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTable tablaCola = Estilo.tabla(modeloCola);
        JPanel izq = new JPanel(new BorderLayout(4, 4));
        izq.add(new JScrollPane(tablaCola), BorderLayout.CENTER);
        izq.add(Estilo.boton("Despachar siguiente (prioridad)", () -> ctx.ejecutar(this, this::despachar)), BorderLayout.SOUTH);

        JPanel der = new JPanel(new BorderLayout(4, 4));
        der.add(new JScrollPane(tablaEnCurso), BorderLayout.CENTER);
        der.add(Estilo.boton("Terminar preparación del seleccionado", () -> ctx.ejecutar(this, this::terminar)), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                Estilo.titulado("Cola de preparación (1.º entrada hoy por hora de llegada · 2.º el resto)", izq),
                Estilo.titulado("En preparación", der));
        split.setResizeWeight(0.62);
        add(split, BorderLayout.CENTER);

        txtRuta.setEditable(false);
        txtRuta.setLineWrap(true);
        txtRuta.setWrapStyleWord(true);
        JPanel sur = new JPanel(new BorderLayout(4, 4));
        sur.add(new JScrollPane(txtRuta), BorderLayout.CENTER);
        JPanel botones = new JPanel(new GridLayout(0, 1, 4, 4));
        botones.add(Estilo.boton("Calcular ruta más corta", () -> ctx.ejecutar(this, this::calcularRuta)));
        botones.add(Estilo.boton("Quitar ruta del mapa", () -> {
            ctx.setRuta(null);
            txtRuta.setText("");
            ctx.refrescar();
        }));
        sur.add(botones, BorderLayout.EAST);
        add(Estilo.titulado("Ruta sugerida desde recepción (Dijkstra) — se dibuja en la pestaña «Mapa»", sur), BorderLayout.SOUTH);
    }

    private void despachar() {
        try {
            TareaPreparacion t = ctx.aloj().despacharPreparacion();
            ctx.info(this, "Despachado: " + t);
        } catch (EstructuraVaciaException ex) {
            ctx.info(this, "No hay apartamentos pendientes de preparación.");
        }
    }

    private void terminar() {
        int f = tablaEnCurso.getSelectedRow();
        if (f < 0) throw new IllegalStateException("Seleccione un apartamento de la tabla «En preparación»");
        ctx.aloj().terminarPreparacion(filasEnCurso.obtener(f).getApartamento());
    }

    private void calcularRuta() {
        Ruta<String> r = ctx.aloj().rutaPreparacion();
        ctx.setRuta(r);
        if (r.camino().tamanio() <= 1) {
            txtRuta.setText("No hay apartamentos pendientes de preparación.");
            ctx.setRuta(null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : r.camino()) {
            if (sb.length() > 0) sb.append(" → ");
            sb.append(s);
        }
        txtRuta.setText(sb + "\nDistancia total: " + String.format("%.0f", r.distancia()) + " m");
    }

    @Override
    public void actualizar() {
        modeloCola.setRowCount(0);
        int pos = 1;
        for (TareaPreparacion t : ctx.aloj().getColaPreparacion()) { // en orden de prioridad
            modeloCola.addRow(new Object[]{pos++, t.getApartamento().getCodigo() + " · " + t.getApartamento().getNombre(),
                    t.isEntradaHoy() ? "1 (entrada hoy)" : "2",
                    t.isEntradaHoy() ? t.getHoraLlegada() : "—", t.getEncoladoEn().format(HORA)});
        }
        modeloEnCurso.setRowCount(0);
        filasEnCurso = new ListaEnlazada<>();
        for (TareaPreparacion t : ctx.aloj().getEnPreparacion()) {
            filasEnCurso.agregarFinal(t);
            modeloEnCurso.addRow(new Object[]{t.getApartamento().getCodigo(), t.getInicio().format(HORA)});
        }
    }
}
