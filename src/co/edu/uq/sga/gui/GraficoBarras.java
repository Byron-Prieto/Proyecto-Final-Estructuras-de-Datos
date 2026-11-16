package co.edu.uq.sga.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/** Gráfico de barras horizontales sencillo. */
public class GraficoBarras extends JPanel {
    private String titulo;
    private String[] etiquetas = new String[0];
    private double[] valores = new double[0];
    private String sufijo = "";
    private Color color = Estilo.AZUL;

    public GraficoBarras(String titulo, String sufijo, Color color) {
        this.titulo = titulo;
        this.sufijo = sufijo;
        this.color = color;
        setPreferredSize(new Dimension(380, 220));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
    }

    public void setDatos(String[] etiquetas, double[] valores) {
        this.etiquetas = etiquetas;
        this.valores = valores;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(380, Math.max(160, 54 + 22 * etiquetas.length));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(getFont().deriveFont(Font.BOLD, 13f));
        g.setColor(Color.DARK_GRAY);
        g.drawString(titulo, 10, 20);
        g.setFont(getFont().deriveFont(12f));
        FontMetrics fm = g.getFontMetrics();
        int izq = 10;
        for (String e : etiquetas) izq = Math.max(izq, fm.stringWidth(e) + 18);
        double max = 0;
        for (double v : valores) max = Math.max(max, v);
        if (max <= 0) max = 1;
        int ancho = getWidth() - izq - 80;
        for (int i = 0; i < etiquetas.length; i++) {
            int y = 34 + i * 22;
            g.setColor(Color.DARK_GRAY);
            g.drawString(etiquetas[i], 10, y + 13);
            int w = (int) Math.round(ancho * valores[i] / max);
            g.setColor(color);
            g.fillRoundRect(izq, y, Math.max(w, 1), 16, 6, 6);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.format(Locale.of("es", "CO"), sufijo.isEmpty() ? "%,.0f%s" : "%,.1f%s", valores[i], sufijo), izq + w + 6, y + 13);
        }
        if (etiquetas.length == 0) {
            g.setColor(Color.GRAY);
            g.drawString("Sin datos", 10, 44);
        }
        g.dispose();
    }
}
