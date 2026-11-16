package co.edu.uq.sga.gui;

import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.Alojamiento;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/**
 * Administración del alojamiento: apartamentos, huéspedes y empleados (árboles de búsqueda),
 * temporadas y tarifas, y edición del mapa.
 */
public class AdminPanel extends JPanel implements Actualizable {

    private final Contexto ctx;

    // apartamentos
    private final DefaultTableModel modeloApt = Estilo.modelo("Código", "Nombre", "Bloque", "Piso", "Dorm.", "Cap.",
            "Tarifa Baja", "Tarifa Media", "Tarifa Alta", "Estado", "Características");
    private final JTable tablaApt = Estilo.tabla(modeloApt);
    private ListaEnlazada<Apartamento> filasApt = new ListaEnlazada<>();
    private final JLabel lblArbolApt = new JLabel();

    // huéspedes
    private final DefaultTableModel modeloHue = Estilo.modelo("Documento", "Nombre", "Nacimiento", "Teléfono", "Correo", "Reservas");
    private final JTable tablaHue = Estilo.tabla(modeloHue);
    private ListaEnlazada<Huesped> filasHue = new ListaEnlazada<>();
    private final JLabel lblArbolHue = new JLabel();
    private final JTextField txtBuscarDoc = new JTextField(12);

    // empleados
    private final DefaultTableModel modeloEmp = Estilo.modelo("Documento", "Nombre", "Rol");
    private final JTable tablaEmp = Estilo.tabla(modeloEmp);
    private ListaEnlazada<Empleado> filasEmp = new ListaEnlazada<>();
    private final JLabel lblArbolEmp = new JLabel();

    // temporadas
    private final DefaultTableModel modeloTemp = Estilo.modelo("Inicio", "Fin", "Temporada");
    private final JTable tablaTemp = Estilo.tabla(modeloTemp);
    private ListaEnlazada<CalendarioTemporadas.Periodo> filasTemp = new ListaEnlazada<>();

    // mapa
    private final DefaultTableModel modeloPas = Estilo.modelo("Desde", "Hasta", "Metros");
    private final JTable tablaPas = Estilo.tabla(modeloPas);
    private ListaEnlazada<String[]> filasPas = new ListaEnlazada<>();

    public AdminPanel(Contexto ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Apartamentos y tarifas", panelApartamentos());
        tabs.addTab("Huéspedes", panelHuespedes());
        tabs.addTab("Empleados", panelEmpleados());
        tabs.addTab("Temporadas", panelTemporadas());
        tabs.addTab("Edición del mapa", panelMapa());
        add(tabs, BorderLayout.CENTER);
    }

    // =====================================================================  construcción de pestañas

    private JComponent pestana(JLabel info, JTable tabla, JComponent... extras) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        p.add(info, BorderLayout.NORTH);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        for (JComponent c : extras) botones.add(c);
        p.add(botones, BorderLayout.SOUTH);
        return p;
    }

    private JComponent panelApartamentos() {
        return pestana(lblArbolApt, tablaApt,
                Estilo.boton("Nuevo apartamento…", () -> ctx.ejecutar(this, () -> formApartamento(null))),
                Estilo.boton("Editar / cambiar tarifas…", () -> ctx.ejecutar(this, () -> formApartamento(seleccionado(tablaApt, filasApt)))),
                Estilo.boton("Eliminar", () -> ctx.ejecutar(this, this::eliminarApartamento)));
    }

    private JComponent panelHuespedes() {
        JButton buscar = Estilo.boton("Buscar por documento", () -> ctx.ejecutar(this, this::buscarHuesped));
        return pestana(lblArbolHue, tablaHue,
                new JLabel("Documento:"), txtBuscarDoc, buscar,
                Estilo.boton("Nuevo huésped…", () -> ctx.ejecutar(this, () -> Dialogos.nuevoHuesped(this, ctx.aloj()))),
                Estilo.boton("Editar…", () -> ctx.ejecutar(this, this::editarHuesped)),
                Estilo.boton("Eliminar", () -> ctx.ejecutar(this, this::eliminarHuesped)));
    }

    private JComponent panelEmpleados() {
        return pestana(lblArbolEmp, tablaEmp,
                Estilo.boton("Nuevo empleado…", () -> ctx.ejecutar(this, this::nuevoEmpleado)),
                Estilo.boton("Eliminar", () -> ctx.ejecutar(this, () ->
                        ctx.aloj().eliminarEmpleado(seleccionado(tablaEmp, filasEmp)))));
    }

    private JComponent panelTemporadas() {
        JLabel info = new JLabel("<html>Las fechas que no estén en ningún periodo se cobran como temporada <b>Baja</b> (base). "
                + "Los periodos no pueden cruzarse. Un cambio no modifica los cargos de reservas ya creadas.</html>");
        return pestana(info, tablaTemp,
                Estilo.boton("Definir periodo…", () -> ctx.ejecutar(this, this::nuevaTemporada)),
                Estilo.boton("Eliminar periodo", () -> ctx.ejecutar(this, () ->
                        ctx.aloj().eliminarTemporada(seleccionado(tablaTemp, filasTemp)))));
    }

    private JComponent panelMapa() {
        JLabel info = new JLabel("<html>Pasillos del edificio. Para analizar conectividad y rutas use la pestaña «Mapa». "
                + "Los apartamentos se agregan al mapa desde «Apartamentos y tarifas».</html>");
        return pestana(info, tablaPas,
                Estilo.boton("Nuevo punto de servicio…", () -> ctx.ejecutar(this, this::nuevoNodo)),
                Estilo.boton("Nuevo pasillo…", () -> ctx.ejecutar(this, this::nuevoPasillo)),
                Estilo.boton("Eliminar pasillo (cierre definitivo)", () -> ctx.ejecutar(this, () -> {
                    String[] f = seleccionado(tablaPas, filasPas);
                    ctx.aloj().eliminarPasillo(f[0], f[1]);
                })),
                Estilo.boton("Eliminar punto de servicio…", () -> ctx.ejecutar(this, this::eliminarNodo)));
    }

    private <T> T seleccionado(JTable tabla, ListaEnlazada<T> filas) {
        int f = tabla.getSelectedRow();
        if (f < 0) throw new IllegalStateException("Seleccione una fila de la tabla");
        return filas.obtener(f);
    }

    // =====================================================================  apartamentos

    private double numero(JTextField campo, String nombre) {
        try {
            double v = Double.parseDouble(campo.getText().trim().replace(",", "."));
            if (v < 0) throw new IllegalArgumentException(nombre + " no puede ser negativa");
            return v;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(nombre + " debe ser un número");
        }
    }

    /** Alta (existente == null) o edición de un apartamento. */
    private void formApartamento(Apartamento ex) {
        Alojamiento aloj = ctx.aloj();
        JTextField codigo = new JTextField(ex == null ? "" : ex.getCodigo(), 14);
        codigo.setEnabled(ex == null);
        JTextField nombre = new JTextField(ex == null ? "" : ex.getNombre(), 14);
        JTextField bloque = new JTextField(ex == null ? "" : ex.getBloque(), 14);
        JSpinner piso = new JSpinner(new SpinnerNumberModel(ex == null ? 1 : ex.getPiso(), 0, 30, 1));
        JSpinner dorm = new JSpinner(new SpinnerNumberModel(ex == null ? 1 : ex.getDormitorios(), 1, 12, 1));
        JSpinner cap = new JSpinner(new SpinnerNumberModel(ex == null ? 2 : ex.getCapacidad(), 1, 40, 1));
        JTextField baja = new JTextField(ex == null ? "0" : String.valueOf((long) ex.getTarifa(Temporada.BAJA)), 14);
        JTextField media = new JTextField(ex == null ? "0" : String.valueOf((long) ex.getTarifa(Temporada.MEDIA)), 14);
        JTextField alta = new JTextField(ex == null ? "0" : String.valueOf((long) ex.getTarifa(Temporada.ALTA)), 14);
        StringBuilder cs = new StringBuilder();
        if (ex != null) for (String c : ex.getCaracteristicas()) cs.append(cs.length() > 0 ? ", " : "").append(c);
        JTextField caract = new JTextField(cs.toString(), 14);

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Código (único):")); p.add(codigo);
        p.add(new JLabel("Nombre:")); p.add(nombre);
        p.add(new JLabel("Bloque:")); p.add(bloque);
        p.add(new JLabel("Piso:")); p.add(piso);
        p.add(new JLabel("Dormitorios:")); p.add(dorm);
        p.add(new JLabel("Capacidad máxima:")); p.add(cap);
        p.add(new JLabel("Tarifa p/p/noche Baja:")); p.add(baja);
        p.add(new JLabel("Tarifa p/p/noche Media:")); p.add(media);
        p.add(new JLabel("Tarifa p/p/noche Alta:")); p.add(alta);
        p.add(new JLabel("Características (comas):")); p.add(caract);
        JComboBox<String> cmbNodo = new JComboBox<>();
        JTextField metros = new JTextField("10", 14);
        if (ex == null) {
            cmbNodo.addItem("(no conectar)");
            for (String s : aloj.getMapa()) cmbNodo.addItem(s);
            p.add(new JLabel("Conectar en el mapa a:")); p.add(cmbNodo);
            p.add(new JLabel("Distancia del pasillo (m):")); p.add(metros);
        }
        if (JOptionPane.showConfirmDialog(this, p, ex == null ? "Nuevo apartamento" : "Editar " + ex.getCodigo(),
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        double tb = numero(baja, "La tarifa Baja"), tm = numero(media, "La tarifa Media"), ta = numero(alta, "La tarifa Alta");
        if (ex == null) {
            Apartamento a = new Apartamento(codigo.getText().trim(), nombre.getText().trim(), bloque.getText().trim(),
                    (Integer) piso.getValue(), (Integer) dorm.getValue(), (Integer) cap.getValue());
            a.setTarifa(Temporada.BAJA, tb);
            a.setTarifa(Temporada.MEDIA, tm);
            a.setTarifa(Temporada.ALTA, ta);
            a.setCaracteristicas(caract.getText());
            aloj.registrarApartamento(a);
            if (cmbNodo.getSelectedIndex() > 0) {
                aloj.agregarPasillo(a.getCodigo(), (String) cmbNodo.getSelectedItem(), numero(metros, "La distancia"));
            }
        } else {
            aloj.actualizarApartamento(ex, nombre.getText().trim(), bloque.getText().trim(), (Integer) piso.getValue(),
                    (Integer) dorm.getValue(), (Integer) cap.getValue(), tb, tm, ta, caract.getText());
        }
    }

    private void eliminarApartamento() {
        Apartamento a = seleccionado(tablaApt, filasApt);
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar el apartamento " + a + " y sus pasillos del mapa?",
                "Eliminar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ctx.aloj().eliminarApartamento(a);
        }
    }

    // =====================================================================  huéspedes

    private void buscarHuesped() {
        String doc = txtBuscarDoc.getText().trim();
        if (doc.isEmpty()) throw new IllegalArgumentException("Escriba un documento");
        Huesped h = ctx.aloj().buscarHuesped(doc); // búsqueda en el árbol binario
        if (h == null) {
            ctx.info(this, "No existe un huésped con documento " + doc + ".");
            return;
        }
        int i = filasHue.indiceDe(h);
        if (i >= 0) tablaHue.setRowSelectionInterval(i, i);
        ctx.info(this, "Encontrado: " + h);
    }

    private void editarHuesped() {
        Huesped h = seleccionado(tablaHue, filasHue);
        JTextField nombre = new JTextField(h.getNombre(), 15);
        JTextField nac = new JTextField(h.getFechaNacimiento() == null ? "" : h.getFechaNacimiento().toString(), 15);
        JTextField tel = new JTextField(h.getTelefono() == null ? "" : h.getTelefono(), 15);
        JTextField correo = new JTextField(h.getCorreo() == null ? "" : h.getCorreo(), 15);
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Documento:")); p.add(new JLabel(h.getDocumento()));
        p.add(new JLabel("Nombre:")); p.add(nombre);
        p.add(new JLabel("Nacimiento (AAAA-MM-DD):")); p.add(nac);
        p.add(new JLabel("Teléfono:")); p.add(tel);
        p.add(new JLabel("Correo:")); p.add(correo);
        if (JOptionPane.showConfirmDialog(this, p, "Editar huésped", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (nombre.getText().isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        LocalDate f = nac.getText().isBlank() ? null : Dialogos.fecha(nac.getText());
        h.setNombre(nombre.getText().trim());
        h.setFechaNacimiento(f);
        h.setTelefono(tel.getText().trim());
        h.setCorreo(correo.getText().trim());
    }

    private void eliminarHuesped() {
        Huesped h = seleccionado(tablaHue, filasHue);
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar al huésped " + h + "?", "Eliminar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ctx.aloj().eliminarHuesped(h);
        }
    }

    // =====================================================================  empleados, temporadas, mapa

    private void nuevoEmpleado() {
        JTextField doc = new JTextField(14), nombre = new JTextField(14);
        JComboBox<Rol> rol = new JComboBox<>(Rol.values());
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Documento:")); p.add(doc);
        p.add(new JLabel("Nombre:")); p.add(nombre);
        p.add(new JLabel("Rol:")); p.add(rol);
        if (JOptionPane.showConfirmDialog(this, p, "Nuevo empleado", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (nombre.getText().isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        ctx.aloj().registrarEmpleado(new Empleado(doc.getText().trim(), nombre.getText().trim(), (Rol) rol.getSelectedItem()));
    }

    private void nuevaTemporada() {
        JTextField ini = new JTextField(ctx.aloj().hoy().toString(), 12), fin = new JTextField(ctx.aloj().hoy().plusDays(15).toString(), 12);
        JComboBox<Temporada> temp = new JComboBox<>(new Temporada[]{Temporada.MEDIA, Temporada.ALTA, Temporada.BAJA});
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Inicio (AAAA-MM-DD):")); p.add(ini);
        p.add(new JLabel("Fin (inclusive):")); p.add(fin);
        p.add(new JLabel("Temporada:")); p.add(temp);
        if (JOptionPane.showConfirmDialog(this, p, "Definir periodo", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        ctx.aloj().definirTemporada(Dialogos.fecha(ini.getText()), Dialogos.fecha(fin.getText()), (Temporada) temp.getSelectedItem());
    }

    private void nuevoNodo() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre del punto de servicio (ej. ESC-C, ASEO-3):");
        if (nombre == null || nombre.isBlank()) return;
        ctx.aloj().agregarNodoMapa(nombre.trim());
    }

    private void nuevoPasillo() {
        Alojamiento aloj = ctx.aloj();
        JComboBox<String> a = new JComboBox<>(), b = new JComboBox<>();
        for (String s : aloj.getMapa()) {
            a.addItem(s);
            b.addItem(s);
        }
        JTextField metros = new JTextField("10", 8);
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Desde:")); p.add(a);
        p.add(new JLabel("Hasta:")); p.add(b);
        p.add(new JLabel("Distancia (m):")); p.add(metros);
        if (JOptionPane.showConfirmDialog(this, p, "Nuevo pasillo", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (a.getSelectedItem().equals(b.getSelectedItem())) throw new IllegalArgumentException("Elija dos nodos distintos");
        aloj.agregarPasillo((String) a.getSelectedItem(), (String) b.getSelectedItem(), numero(metros, "La distancia"));
    }

    private void eliminarNodo() {
        Alojamiento aloj = ctx.aloj();
        ListaEnlazada<String> servicios = new ListaEnlazada<>();
        for (String s : aloj.getMapa()) if (!s.equals(Alojamiento.RECEPCION) && aloj.buscarApartamento(s) == null) servicios.agregarFinal(s);
        if (servicios.estaVacia()) throw new IllegalStateException("No hay puntos de servicio para eliminar");
        String[] op = new String[servicios.tamanio()];
        int i = 0;
        for (String s : servicios) op[i++] = s;
        Object sel = JOptionPane.showInputDialog(this, "Punto de servicio a eliminar:", "Eliminar",
                JOptionPane.PLAIN_MESSAGE, null, op, op[0]);
        if (sel != null) aloj.eliminarNodoMapa((String) sel);
    }

    // =====================================================================  actualización

    @Override
    public void actualizar() {
        Alojamiento aloj = ctx.aloj();

        modeloApt.setRowCount(0);
        filasApt = new ListaEnlazada<>();
        for (Apartamento a : aloj.getCatalogo()) {
            filasApt.agregarFinal(a);
            StringBuilder cs = new StringBuilder();
            for (String c : a.getCaracteristicas()) cs.append(cs.length() > 0 ? ", " : "").append(c);
            modeloApt.addRow(new Object[]{a.getCodigo(), a.getNombre(), a.getBloque(), a.getPiso(), a.getDormitorios(),
                    a.getCapacidad(), Estilo.dinero(a.getTarifa(Temporada.BAJA)), Estilo.dinero(a.getTarifa(Temporada.MEDIA)),
                    Estilo.dinero(a.getTarifa(Temporada.ALTA)), a.getEstado(), cs.toString()});
        }
        lblArbolApt.setText("Catálogo (árbol binario de búsqueda por código): " + aloj.getCatalogo().tamanio()
                + " apartamentos · altura del árbol: " + aloj.getCatalogo().altura());

        modeloHue.setRowCount(0);
        filasHue = new ListaEnlazada<>();
        for (Huesped h : aloj.getHuespedes()) {
            filasHue.agregarFinal(h);
            modeloHue.addRow(new Object[]{h.getDocumento(), h.getNombre(), h.getFechaNacimiento() == null ? "—" : h.getFechaNacimiento(),
                    h.getTelefono() == null ? "—" : h.getTelefono(), h.getCorreo() == null ? "—" : h.getCorreo(),
                    h.getHistorial().tamanio()});
        }
        lblArbolHue.setText("Huéspedes (árbol binario de búsqueda por documento): " + aloj.getHuespedes().tamanio()
                + " · altura del árbol: " + aloj.getHuespedes().altura());

        modeloEmp.setRowCount(0);
        filasEmp = new ListaEnlazada<>();
        for (Empleado e : aloj.getEmpleados()) {
            filasEmp.agregarFinal(e);
            modeloEmp.addRow(new Object[]{e.getDocumento(), e.getNombre(), e.getRol()});
        }
        StringBuilder turnos = new StringBuilder();
        for (Empleado e : aloj.getTurnos()) turnos.append(turnos.length() > 0 ? " → " : "").append(e.getNombre());
        lblArbolEmp.setText("<html>Empleados (árbol por documento): " + aloj.getEmpleados().tamanio()
                + " · altura: " + aloj.getEmpleados().altura() + "<br>Rotación de turnos (desde el turno actual): "
                + (turnos.length() == 0 ? "—" : turnos) + "</html>");

        modeloTemp.setRowCount(0);
        filasTemp = new ListaEnlazada<>();
        for (CalendarioTemporadas.Periodo p : aloj.getCalendario().getPeriodos()) {
            filasTemp.agregarFinal(p);
            modeloTemp.addRow(new Object[]{p.inicio(), p.fin(), p.temporada()});
        }

        modeloPas.setRowCount(0);
        filasPas = new ListaEnlazada<>();
        for (String u : aloj.getMapa()) {
            for (co.edu.uq.sga.estructuras.Grafo.Arista<String> ar : aloj.getMapa().getVertice(u).getAdyacentes()) {
                String v = ar.getDestino().getDato();
                if (u.compareTo(v) < 0) {
                    filasPas.agregarFinal(new String[]{u, v});
                    modeloPas.addRow(new Object[]{u, v, String.format("%.0f", ar.getPeso())});
                }
            }
        }
    }
}
