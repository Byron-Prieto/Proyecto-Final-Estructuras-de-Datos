package co.edu.uq.sga.gui;

import co.edu.uq.sga.estructuras.Grafo;
import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.estructuras.Ruta;
import co.edu.uq.sga.modelo.Apartamento;
import co.edu.uq.sga.servicio.Alojamiento;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/** Mapa interactivo del edificio: grafo con estado de los apartamentos en tiempo real y rutas. */
public class MapaPanel extends JPanel implements Actualizable {

    private final Contexto ctx;
    private final Lienzo lienzo = new Lienzo();
    private final JComboBox<String> cmbOrigen = new JComboBox<>();
    private final JComboBox<String> cmbDestino = new JComboBox<>();
    private final JTextArea salida = new JTextArea(12, 24);

    public MapaPanel(Contexto ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(lienzo, BorderLayout.CENTER);
        add(panelControl(), BorderLayout.EAST);
        add(leyenda(), BorderLayout.SOUTH);
    }

    private JComponent leyenda() {
        JPanel l = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        l.add(Estilo.leyenda(Estilo.VERDE, "Preparado"));
        l.add(Estilo.leyenda(Estilo.AZUL, "Ocupado"));
        l.add(Estilo.leyenda(Estilo.AMARILLO, "Pendiente / En preparación"));
        l.add(Estilo.leyenda(Estilo.ROJO, "Fuera de servicio"));
        l.add(Estilo.leyenda(new Color(142, 68, 173), "Recepción"));
        l.add(Estilo.leyenda(new Color(230, 126, 34), "Escalera"));
        l.add(Estilo.leyenda(new Color(26, 188, 156), "Cuarto de aseo"));
        l.add(new JLabel("  Arrastre los nodos para reacomodar el mapa."));
        return l;
    }

    private JComponent panelControl() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(290, 400));

        JPanel combos = new JPanel(new GridLayout(0, 2, 4, 4));
        combos.add(new JLabel("Origen / pasillo A:"));
        combos.add(cmbOrigen);
        combos.add(new JLabel("Destino / pasillo B:"));
        combos.add(cmbDestino);
        combos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        p.add(combos);

        p.add(fila(Estilo.boton("Ruta más corta (Dijkstra)", () -> ctx.ejecutar(this, this::rutaEntre))));
        p.add(fila(Estilo.boton("Ruta de preparación", () -> ctx.ejecutar(this, this::rutaPreparacion))));
        p.add(fila(Estilo.boton("Quitar ruta", () -> { ctx.setRuta(null); lienzo.repaint(); })));
        p.add(fila(Estilo.boton("Si se cierra el pasillo A–B…", () -> ctx.ejecutar(this, this::cierrePasillo))));
        p.add(fila(Estilo.boton("Recorrido BFS desde origen", () -> ctx.ejecutar(this, () ->
                mostrar("BFS desde " + origen() + ":\n" + ctx.aloj().getMapa().bfs(origen()))))));
        p.add(fila(Estilo.boton("Recorrido DFS desde origen", () -> ctx.ejecutar(this, () ->
                mostrar("DFS desde " + origen() + ":\n" + ctx.aloj().getMapa().dfs(origen()))))));
        p.add(fila(Estilo.boton("Conectividad (componentes)", () -> ctx.ejecutar(this, () ->
                mostrar("Componentes conexas:\n" + ctx.aloj().getMapa().componentesConexas())))));
        p.add(fila(Estilo.boton("Apartamentos por bloque", () -> ctx.ejecutar(this, () ->
                mostrar("Por bloque:\n" + ctx.aloj().agruparPorBloque())))));
        p.add(fila(Estilo.boton("Reorganizar mapa", () -> { lienzo.reorganizar(); lienzo.repaint(); })));

        salida.setEditable(false);
        salida.setLineWrap(true);
        salida.setWrapStyleWord(true);
        p.add(Box.createVerticalStrut(6));
        p.add(new JScrollPane(salida));
        return p;
    }

    private JComponent fila(JButton b) {
        JPanel f = new JPanel(new BorderLayout());
        f.add(b, BorderLayout.CENTER);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        return f;
    }

    private String origen() {
        String s = (String) cmbOrigen.getSelectedItem();
        if (s == null) throw new IllegalStateException("No hay nodos en el mapa");
        return s;
    }

    private String destino() {
        String s = (String) cmbDestino.getSelectedItem();
        if (s == null) throw new IllegalStateException("No hay nodos en el mapa");
        return s;
    }

    private void mostrar(String texto) {
        salida.setText(texto);
        salida.setCaretPosition(0);
    }

    private void rutaEntre() {
        Ruta<String> r = ctx.aloj().getMapa().dijkstra(origen(), destino());
        if (r == null) {
            ctx.setRuta(null);
            mostrar("No existe un camino entre " + origen() + " y " + destino() + ".");
        } else {
            ctx.setRuta(r);
            mostrar("Ruta más corta:\n" + r.camino() + "\nDistancia: " + String.format("%.0f", r.distancia()) + " m");
        }
        lienzo.repaint();
    }

    private void rutaPreparacion() {
        Ruta<String> r = ctx.aloj().rutaPreparacion();
        if (r.camino().tamanio() <= 1) {
            ctx.setRuta(null);
            mostrar("No hay apartamentos pendientes de preparación.");
        } else {
            ctx.setRuta(r);
            mostrar("Ruta de preparación desde recepción:\n" + r.camino() + "\nDistancia: "
                    + String.format("%.0f", r.distancia()) + " m");
        }
        lienzo.repaint();
    }

    private void cierrePasillo() {
        ListaEnlazada<String> perdidos = ctx.aloj().inalcanzablesSiSeCierra(origen(), destino());
        mostrar("Si se cierra el pasillo " + origen() + "–" + destino() + ", desde recepción "
                + (perdidos.estaVacia() ? "todo sigue siendo alcanzable." : "quedan inalcanzables:\n" + perdidos));
    }

    @Override
    public void actualizar() {
        Alojamiento aloj = ctx.aloj();
        String o = (String) cmbOrigen.getSelectedItem();
        String d = (String) cmbDestino.getSelectedItem();
        DefaultComboBoxModel<String> mo = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<String> md = new DefaultComboBoxModel<>();
        for (String s : aloj.getMapa()) {
            mo.addElement(s);
            md.addElement(s);
        }
        cmbOrigen.setModel(mo);
        cmbDestino.setModel(md);
        if (o != null) cmbOrigen.setSelectedItem(o);
        if (d != null) cmbDestino.setSelectedItem(d);
        lienzo.reconstruir();
        lienzo.repaint();
    }

    // =====================================================================  lienzo

    private class Lienzo extends JPanel {
        private static final int MARGEN = 56;

        private String[] nombres = new String[0];
        private Apartamento[] aptos = new Apartamento[0];
        private double[] x = new double[0];
        private double[] y = new double[0];
        private int[] ea = new int[0];
        private int[] eb = new int[0];
        private double[] ep = new double[0];
        private String firma = "";
        private int arrastrado = -1;

        Lienzo() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            setPreferredSize(new Dimension(700, 520));
            ToolTipManager.sharedInstance().registerComponent(this);
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    arrastrado = nodoEn(e.getPoint());
                    if (arrastrado >= 0) {
                        cmbOrigen.setSelectedItem(nombres[arrastrado]);
                        if (e.isShiftDown() || SwingUtilities.isRightMouseButton(e)) cmbDestino.setSelectedItem(nombres[arrastrado]);
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (arrastrado < 0) return;
                    x[arrastrado] = Math.max(0, Math.min(1, (e.getX() - MARGEN) / (double) Math.max(1, getWidth() - 2 * MARGEN)));
                    y[arrastrado] = Math.max(0, Math.min(1, (e.getY() - MARGEN) / (double) Math.max(1, getHeight() - 2 * MARGEN)));
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    arrastrado = -1;
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int i = nodoEn(e.getPoint());
                    setToolTipText(i < 0 ? null : descripcion(i));
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private String descripcion(int i) {
            Apartamento a = aptos[i];
            if (a == null) return nombres[i];
            return "<html><b>" + a.getCodigo() + " · " + a.getNombre() + "</b><br>" + a.getEstado().getEtiqueta()
                    + "<br>Bloque " + a.getBloque() + ", piso " + a.getPiso() + " · cap. " + a.getCapacidad() + "</html>";
        }

        /** Relee el grafo; si cambió su estructura recalcula la disposición. */
        void reconstruir() {
            Alojamiento aloj = ctx.aloj();
            Grafo<String> g = aloj.getMapa();
            int n = g.cantidadVertices();
            String[] nuevos = new String[n];
            int i = 0;
            for (String s : g) nuevos[i++] = s;

            int m = 0;
            for (String s : nuevos) m += g.getVertice(s).getAdyacentes().tamanio();
            m /= 2;
            int[] na = new int[m], nb = new int[m];
            double[] np = new double[m];
            int k = 0;
            for (int u = 0; u < n; u++) {
                for (Grafo.Arista<String> ar : g.getVertice(nuevos[u]).getAdyacentes()) {
                    int v = indiceDe(nuevos, ar.getDestino().getDato());
                    if (u < v) {
                        na[k] = u;
                        nb[k] = v;
                        np[k++] = ar.getPeso();
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            for (String s : nuevos) sb.append(s).append('|');
            for (int q = 0; q < k; q++) sb.append(na[q]).append('-').append(nb[q]).append(',');
            String nuevaFirma = sb.toString();

            nombres = nuevos;
            ea = java.util.Arrays.copyOf(na, k);
            eb = java.util.Arrays.copyOf(nb, k);
            ep = java.util.Arrays.copyOf(np, k);
            aptos = new Apartamento[n];
            for (int u = 0; u < n; u++) aptos[u] = aloj.buscarApartamento(nombres[u]);
            if (!nuevaFirma.equals(firma)) {
                firma = nuevaFirma;
                reorganizar();
            }
        }

        private int indiceDe(String[] arr, String s) {
            for (int i = 0; i < arr.length; i++) if (arr[i].equals(s)) return i;
            return -1;
        }

        /** Disposición por fuerzas (Fruchterman–Reingold), determinista. */
        void reorganizar() {
            int n = nombres.length;
            x = new double[n];
            y = new double[n];
            if (n == 0) return;
            Random rnd = new Random(11);
            for (int i = 0; i < n; i++) {
                x[i] = rnd.nextDouble();
                y[i] = rnd.nextDouble();
            }
            double k = Math.sqrt(1.0 / n) * 0.95;
            double t = 0.12;
            for (int it = 0; it < 400; it++) {
                double[] dx = new double[n], dy = new double[n];
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        double ux = x[i] - x[j], uy = y[i] - y[j];
                        double d = Math.max(Math.hypot(ux, uy), 1e-4);
                        double f = k * k / d;
                        dx[i] += ux / d * f; dy[i] += uy / d * f;
                        dx[j] -= ux / d * f; dy[j] -= uy / d * f;
                    }
                }
                for (int e = 0; e < ea.length; e++) {
                    int i = ea[e], j = eb[e];
                    double ux = x[i] - x[j], uy = y[i] - y[j];
                    double d = Math.max(Math.hypot(ux, uy), 1e-4);
                    double f = d * d / k;
                    dx[i] -= ux / d * f; dy[i] -= uy / d * f;
                    dx[j] += ux / d * f; dy[j] += uy / d * f;
                }
                for (int i = 0; i < n; i++) {
                    double d = Math.max(Math.hypot(dx[i], dy[i]), 1e-6);
                    double paso = Math.min(d, t);
                    x[i] += dx[i] / d * paso;
                    y[i] += dy[i] / d * paso;
                }
                t *= 0.985;
            }
            // normalizar a [0,1]
            double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
            for (int i = 0; i < n; i++) {
                minX = Math.min(minX, x[i]); maxX = Math.max(maxX, x[i]);
                minY = Math.min(minY, y[i]); maxY = Math.max(maxY, y[i]);
            }
            for (int i = 0; i < n; i++) {
                x[i] = (x[i] - minX) / Math.max(maxX - minX, 1e-6);
                y[i] = (y[i] - minY) / Math.max(maxY - minY, 1e-6);
            }
        }

        private Point2DPx pos(int i) {
            return new Point2DPx(MARGEN + x[i] * (getWidth() - 2 * MARGEN), MARGEN + y[i] * (getHeight() - 2 * MARGEN));
        }

        private int anchoServicio(int i) {
            String et = nombres[i].equals(Alojamiento.RECEPCION) ? "REC" : nombres[i];
            return Math.max(38, getFontMetrics(getFont().deriveFont(Font.BOLD, 12f)).stringWidth(et) + 18);
        }

        private int nodoEn(Point p) {
            for (int i = nombres.length - 1; i >= 0; i--) {
                Point2DPx c = pos(i);
                if (aptos[i] != null) {
                    if (Math.abs(p.x - c.x) <= 32 && Math.abs(p.y - c.y) <= 16) return i;
                } else if (Math.abs(p.x - c.x) <= anchoServicio(i) / 2.0 && Math.abs(p.y - c.y) <= 19) {
                    return i;
                }
            }
            return -1;
        }

        private Color colorServicio(String nombre) {
            if (nombre.equals(Alojamiento.RECEPCION)) return new Color(142, 68, 173);
            if (nombre.toUpperCase().startsWith("ESC")) return new Color(230, 126, 34);
            if (nombre.toUpperCase().startsWith("ASEO")) return new Color(26, 188, 156);
            return Color.GRAY;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (nombres.length == 0) {
                g.setColor(Color.GRAY);
                g.drawString("El mapa está vacío. Cargue un escenario.", 20, 30);
                g.dispose();
                return;
            }
            Font base = getFont().deriveFont(12f);
            g.setFont(base);

            // pasillos
            g.setStroke(new BasicStroke(2f));
            for (int e = 0; e < ea.length; e++) {
                Point2DPx a = pos(ea[e]), b = pos(eb[e]);
                g.setColor(new Color(170, 170, 170));
                g.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
            }
            // ruta resaltada
            Ruta<String> ruta = ctx.getRuta();
            String[] camino = new String[0];
            if (ruta != null) {
                camino = new String[ruta.camino().tamanio()];
                int c = 0;
                for (String s : ruta.camino()) camino[c++] = s;
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Estilo.RUTA);
                for (int c2 = 0; c2 + 1 < camino.length; c2++) {
                    int i = indiceDe(nombres, camino[c2]), j = indiceDe(nombres, camino[c2 + 1]);
                    if (i < 0 || j < 0) continue;
                    Point2DPx a = pos(i), b = pos(j);
                    g.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
                }
            }
            // pesos
            g.setFont(base.deriveFont(11f));
            FontMetrics fm0 = g.getFontMetrics();
            for (int e = 0; e < ea.length; e++) {
                Point2DPx a = pos(ea[e]), b = pos(eb[e]);
                String txt = String.format("%.0f m", ep[e]);
                double mx = (a.x + b.x) / 2, my = (a.y + b.y) / 2;
                int w = fm0.stringWidth(txt) + 6;
                g.setColor(new Color(255, 255, 255, 220));
                g.fillRoundRect((int) mx - w / 2, (int) my - 8, w, 15, 6, 6);
                g.setColor(new Color(90, 90, 90));
                g.drawString(txt, (int) mx - w / 2 + 3, (int) my + 4);
            }
            // nodos
            g.setFont(base.deriveFont(Font.BOLD, 12f));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < nombres.length; i++) {
                Point2DPx c = pos(i);
                Color fondo;
                if (aptos[i] != null) {
                    fondo = Estilo.colorEstado(aptos[i].getEstado());
                    RoundRectangle2D r = new RoundRectangle2D.Double(c.x - 32, c.y - 15, 64, 30, 12, 12);
                    g.setColor(fondo);
                    g.fill(r);
                    g.setColor(fondo.darker());
                    g.setStroke(new BasicStroke(2f));
                    g.draw(r);
                } else {
                    fondo = colorServicio(nombres[i]);
                    int w = Math.max(38, fm.stringWidth(nombres[i].equals(Alojamiento.RECEPCION) ? "REC" : nombres[i]) + 18);
                    g.setColor(fondo);
                    g.fillOval((int) c.x - w / 2, (int) c.y - 19, w, 38);
                    g.setColor(fondo.darker());
                    g.setStroke(new BasicStroke(2f));
                    g.drawOval((int) c.x - w / 2, (int) c.y - 19, w, 38);
                }
                String et = nombres[i].equals(Alojamiento.RECEPCION) ? "REC" : nombres[i];
                g.setColor(Estilo.textoSobre(fondo));
                g.drawString(et, (int) (c.x - fm.stringWidth(et) / 2.0), (int) c.y + 4);
            }
            // orden de visita sobre la ruta
            int orden = 0;
            boolean[] visto = new boolean[nombres.length];
            for (String s : camino) {
                int i = indiceDe(nombres, s);
                if (i < 0 || visto[i]) continue;
                visto[i] = true;
                if (orden++ == 0) continue; // el origen no se numera
                Point2DPx c = pos(i);
                int bx = (int) c.x + (aptos[i] != null ? 22 : 12), by = (int) c.y - 28;
                g.setColor(Estilo.RUTA);
                g.fillOval(bx, by, 20, 20);
                g.setColor(Color.WHITE);
                String num = String.valueOf(orden - 1);
                g.drawString(num, bx + 10 - fm.stringWidth(num) / 2, by + 15);
            }
            g.dispose();
        }
    }

    private record Point2DPx(double x, double y) {
    }
}
