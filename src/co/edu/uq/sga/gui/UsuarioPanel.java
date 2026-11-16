package co.edu.uq.sga.gui;

import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.Alojamiento;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.Iterator;

/** Panel del huésped: perfil, reservas con saldo, favoritos e historial. */
public class UsuarioPanel extends JPanel implements Actualizable {

    private final Contexto ctx;
    private final JComboBox<Huesped> cmbHuesped = new JComboBox<>();
    private final JLabel lblPerfil = new JLabel();
    private final JLabel lblFoto = new JLabel("Sin foto", SwingConstants.CENTER);

    private final DefaultTableModel modeloActivas = Estilo.modelo("Folio", "Apartamento", "Entrada", "Salida", "Estado",
            "Cargos", "Pagado", "Saldo");
    private final JTable tablaActivas = Estilo.tabla(modeloActivas);
    private ListaEnlazada<Reserva> filasActivas = new ListaEnlazada<>();

    private final DefaultTableModel modeloHistorial = Estilo.modelo("Folio", "Apartamento", "Entrada", "Salida", "Estado", "Total");
    private final DefaultListModel<Apartamento> modeloFavoritos = new DefaultListModel<>();
    private final JList<Apartamento> listaFavoritos = new JList<>(modeloFavoritos);
    private final JComboBox<Apartamento> cmbCatalogo = new JComboBox<>();
    private boolean actualizando;

    public UsuarioPanel(Contexto ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        norte.add(new JLabel("Huésped:"));
        cmbHuesped.setPreferredSize(new Dimension(260, 26));
        cmbHuesped.addActionListener(e -> {
            if (!actualizando) actualizarDatos();
        });
        norte.add(cmbHuesped);
        norte.add(Estilo.boton("Nuevo huésped…", () -> ctx.ejecutar(this, () -> {
            Huesped h = Dialogos.nuevoHuesped(this, ctx.aloj());
            if (h != null) cmbHuesped.setSelectedItem(h);
        })));
        norte.add(Estilo.boton("Foto…", this::elegirFoto));
        add(norte, BorderLayout.NORTH);

        JPanel perfil = new JPanel(new BorderLayout(8, 8));
        lblFoto.setPreferredSize(new Dimension(110, 110));
        lblFoto.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        perfil.add(lblFoto, BorderLayout.WEST);
        perfil.add(lblPerfil, BorderLayout.CENTER);
        perfil.setBorder(BorderFactory.createTitledBorder("Perfil"));

        JTabbedPane tabs = new JTabbedPane();
        JPanel activas = new JPanel(new BorderLayout(4, 4));
        activas.add(new JScrollPane(tablaActivas), BorderLayout.CENTER);
        JPanel acc = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        acc.add(Estilo.boton("Pagar anticipo 30 %", () -> ctx.ejecutar(this, this::pagarAnticipo)));
        acc.add(Estilo.boton("Cancelar (ver reembolso)", () -> ctx.ejecutar(this, this::cancelar)));
        acc.add(Estilo.boton("Ver folio", () -> ctx.ejecutar(this, () -> Dialogos.folio(this, seleccionada()))));
        acc.add(new JLabel("  Para reservar por el portal use la pestaña «Reservas» (canal PORTAL)."));
        activas.add(acc, BorderLayout.SOUTH);
        tabs.addTab("Mis reservas", activas);

        JPanel fav = new JPanel(new BorderLayout(4, 4));
        fav.add(new JScrollPane(listaFavoritos), BorderLayout.CENTER);
        JPanel accFav = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        accFav.add(cmbCatalogo);
        accFav.add(Estilo.boton("Agregar a favoritos", () -> ctx.ejecutar(this, this::agregarFavorito)));
        accFav.add(Estilo.boton("Quitar seleccionado", () -> ctx.ejecutar(this, this::quitarFavorito)));
        fav.add(accFav, BorderLayout.SOUTH);
        tabs.addTab("Favoritos", fav);

        tabs.addTab("Historial de estancias", new JScrollPane(Estilo.tabla(modeloHistorial)));

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(perfil, BorderLayout.NORTH);
        centro.add(tabs, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }

    private Huesped huesped() {
        Huesped h = (Huesped) cmbHuesped.getSelectedItem();
        if (h == null) throw new IllegalStateException("No hay un huésped seleccionado");
        return h;
    }

    private Reserva seleccionada() {
        int f = tablaActivas.getSelectedRow();
        if (f < 0) throw new IllegalStateException("Seleccione una reserva de la tabla");
        return filasActivas.obtener(f);
    }

    private void pagarAnticipo() {
        double m = ctx.aloj().pagarAnticipo(seleccionada());
        ctx.info(this, m > 0 ? "Anticipo pagado: " + Estilo.dinero(m) + ". Su reserva queda Confirmada."
                : "El anticipo ya estaba pagado.");
    }

    private void cancelar() {
        Reserva r = seleccionada();
        double reembolso = ctx.aloj().reembolsoSiCancela(r);
        if (JOptionPane.showConfirmDialog(this, "Si cancela la reserva " + r.getId() + " se le devolverían "
                + Estilo.dinero(reembolso) + ".\n¿Desea cancelarla?", "Cancelar reserva", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {
            ctx.info(this, "Reserva cancelada. Reembolso: " + Estilo.dinero(ctx.aloj().cancelarReserva(r)));
        }
    }

    private void agregarFavorito() {
        Apartamento a = (Apartamento) cmbCatalogo.getSelectedItem();
        if (a == null) return;
        if (!huesped().agregarFavorito(a)) ctx.info(this, a.getCodigo() + " ya estaba en favoritos (el conjunto no admite repetidos).");
    }

    private void quitarFavorito() {
        Apartamento a = listaFavoritos.getSelectedValue();
        if (a != null) huesped().quitarFavorito(a);
    }

    private void elegirFoto() {
        Huesped h = (Huesped) cmbHuesped.getSelectedItem();
        if (h == null) return;
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            h.setFotografia(fc.getSelectedFile().getAbsolutePath());
            ctx.guardar();
            actualizarDatos();
        }
    }

    @Override
    public void actualizar() {
        actualizando = true;
        Object sel = cmbHuesped.getSelectedItem();
        DefaultComboBoxModel<Huesped> m = new DefaultComboBoxModel<>();
        for (Huesped h : ctx.aloj().getHuespedes()) m.addElement(h);
        cmbHuesped.setModel(m);
        if (sel != null) cmbHuesped.setSelectedItem(sel);
        DefaultComboBoxModel<Apartamento> mc = new DefaultComboBoxModel<>();
        for (Apartamento a : ctx.aloj().getCatalogo()) mc.addElement(a);
        cmbCatalogo.setModel(mc);
        actualizando = false;
        actualizarDatos();
    }

    private void actualizarDatos() {
        Alojamiento aloj = ctx.aloj();
        modeloActivas.setRowCount(0);
        modeloHistorial.setRowCount(0);
        modeloFavoritos.clear();
        filasActivas = new ListaEnlazada<>();
        Huesped h = (Huesped) cmbHuesped.getSelectedItem();
        if (h == null) {
            lblPerfil.setText("<html>Sin huésped seleccionado.</html>");
            lblFoto.setIcon(null);
            lblFoto.setText("Sin foto");
            return;
        }
        lblPerfil.setText("<html><b style='font-size:14px'>" + h.getNombre() + "</b><br>Documento: " + h.getDocumento()
                + "<br>Nacimiento: " + (h.getFechaNacimiento() == null ? "—" : h.getFechaNacimiento())
                + "<br>Teléfono: " + (h.getTelefono() == null ? "—" : h.getTelefono())
                + "<br>Correo: " + (h.getCorreo() == null ? "—" : h.getCorreo()) + "</html>");
        mostrarFoto(h);

        for (Reserva r : h.getHistorial()) {
            if (r.getEstado().esActiva()) {
                filasActivas.agregarFinal(r);
                Folio f = r.getFolio();
                modeloActivas.addRow(new Object[]{r.getId(), r.getApartamento().getCodigo(), r.getEstancia().getEntrada(),
                        r.getEstancia().getSalida(), r.getEstado(), Estilo.dinero(f.totalCargos()),
                        Estilo.dinero(f.totalPagado()), Estilo.dinero(f.saldo())});
            }
        }
        // historial: de la más reciente a la más antigua (iterador inverso de la lista doble)
        Iterator<Reserva> it = h.getHistorial().iteradorInverso();
        while (it.hasNext()) {
            Reserva r = it.next();
            if (!r.getEstado().esActiva()) {
                modeloHistorial.addRow(new Object[]{r.getId(), r.getApartamento().getCodigo(), r.getEstancia().getEntrada(),
                        r.getEstancia().getSalida(), r.getEstado(), Estilo.dinero(r.getFolio().totalCargos())});
            }
        }
        for (Apartamento a : h.getFavoritos()) modeloFavoritos.addElement(a);
    }

    private void mostrarFoto(Huesped h) {
        lblFoto.setIcon(null);
        lblFoto.setText("Sin foto");
        if (h.getFotografia() == null) return;
        File f = new File(h.getFotografia());
        if (!f.isFile()) return;
        ImageIcon icono = new ImageIcon(f.getAbsolutePath());
        if (icono.getIconWidth() <= 0) return;
        lblFoto.setText("");
        lblFoto.setIcon(new ImageIcon(icono.getImage().getScaledInstance(104, 104, Image.SCALE_SMOOTH)));
    }
}
