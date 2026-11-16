package co.edu.uq.sga.gui;

import co.edu.uq.sga.estructuras.ListaEnlazada;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import co.edu.uq.sga.modelo.*;
import co.edu.uq.sga.servicio.Alojamiento;
import co.edu.uq.sga.servicio.ResultadoSolicitud;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/** Panel de reservas: disponibilidad, creación, folio, llegadas/salidas y canal externo. */
public class ReservasPanel extends JPanel implements Actualizable {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Contexto ctx;

    private final JTextField txtEntrada = new JTextField(9);
    private final JTextField txtSalida = new JTextField(9);
    private final JSpinner spPersonas = new JSpinner(new SpinnerNumberModel(2, 1, 60, 1));
    private final JComboBox<String> cmbOrden = new JComboBox<>(new String[]{"Precio", "Capacidad", "Número de dormitorios"});
    private final JComboBox<Huesped> cmbHuesped = new JComboBox<>();
    private final JComboBox<Canal> cmbCanal = new JComboBox<>(new Canal[]{Canal.PORTAL, Canal.DIRECTA});
    private final JTextArea txtOcupantes = new JTextArea(3, 26);

    private final DefaultTableModel modeloResultados = Estilo.modelo("Código", "Nombre", "Bloque", "Piso", "Dorm.",
            "Capacidad", "Tarifa p/p/noche", "Total estimado");
    private final JTable tablaResultados = Estilo.tabla(modeloResultados);
    private ListaEnlazada<Apartamento> resultados = new ListaEnlazada<>();

    private final DefaultTableModel modeloReservas = Estilo.modelo("Folio", "Apartamento", "Titular", "Entrada", "Salida",
            "Canal", "Estado", "Cargos", "Pagado", "Saldo");
    private final JTable tablaReservas = Estilo.tabla(modeloReservas);
    private ListaEnlazada<Reserva> filasReservas = new ListaEnlazada<>();

    private final JLabel lblExterna = new JLabel();
    private boolean iniciado;

    public ReservasPanel(Contexto ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panelBusqueda(), BorderLayout.NORTH);

        JScrollPane spRes = new JScrollPane(tablaResultados);
        spRes.setPreferredSize(new Dimension(400, 130));
        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.add(new JScrollPane(tablaReservas), BorderLayout.CENTER);
        abajo.add(panelAcciones(), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                Estilo.titulado("Apartamentos disponibles", spRes),
                Estilo.titulado("Reservas", abajo));
        split.setResizeWeight(0.35);
        add(split, BorderLayout.CENTER);
        add(panelExterno(), BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------ construcción

    private JComponent panelBusqueda() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridy = 0;
        c.gridx = 0; p.add(new JLabel("Entrada (AAAA-MM-DD):"), c);
        c.gridx = 1; p.add(txtEntrada, c);
        c.gridx = 2; p.add(new JLabel("Salida:"), c);
        c.gridx = 3; p.add(txtSalida, c);
        c.gridx = 4; p.add(new JLabel("Personas:"), c);
        c.gridx = 5; p.add(spPersonas, c);
        c.gridx = 6; p.add(new JLabel("Ordenar por:"), c);
        c.gridx = 7; p.add(cmbOrden, c);
        c.gridx = 8; p.add(Estilo.boton("Buscar", () -> ctx.ejecutar(this, this::buscar)), c);

        c.gridy = 1;
        c.gridx = 0; p.add(new JLabel("Huésped titular:"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL; p.add(cmbHuesped, c);
        c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        c.gridx = 4; p.add(Estilo.boton("Nuevo huésped…", () -> ctx.ejecutar(this, () -> {
            Huesped h = Dialogos.nuevoHuesped(this, ctx.aloj());
            if (h != null) {
                actualizarHuespedes();
                cmbHuesped.setSelectedItem(h);
            }
        })), c);
        c.gridx = 5; p.add(new JLabel("Canal:"), c);
        c.gridx = 6; p.add(cmbCanal, c);

        c.gridy = 2;
        c.gridx = 0; p.add(new JLabel("<html>Ocupantes<br>(Nombre, AAAA-MM-DD)</html>"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL;
        txtOcupantes.setToolTipText("Una persona por línea. Si lo deja vacío se usan adultos genéricos según «Personas».");
        p.add(new JScrollPane(txtOcupantes), c);
        c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        c.gridx = 4; p.add(Estilo.boton("Cotizar", () -> ctx.ejecutar(this, this::cotizar)), c);
        c.gridx = 5; p.add(Estilo.boton("Reservar", () -> ctx.ejecutar(this, this::reservar)), c);
        c.gridx = 6; c.gridwidth = 2;
        p.add(Estilo.boton("Grupo grande (varios apartamentos)", () -> ctx.ejecutar(this, this::grupoGrande)), c);
        return p;
    }

    private JComponent panelAcciones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        p.add(Estilo.boton("Pagar anticipo 30 %", () -> ctx.ejecutar(this, this::pagarAnticipo)));
        p.add(Estilo.boton("Cancelar", () -> ctx.ejecutar(this, this::cancelar)));
        p.add(Estilo.boton("Check-in", () -> ctx.ejecutar(this, () -> ctx.aloj().checkIn(reservaSeleccionada()))));
        p.add(Estilo.boton("Check-out", () -> ctx.ejecutar(this, () -> ctx.aloj().checkOut(reservaSeleccionada()))));
        p.add(Estilo.boton("No-show", () -> ctx.ejecutar(this, () -> ctx.aloj().marcarNoShow(reservaSeleccionada()))));
        p.add(new JSeparator(SwingConstants.VERTICAL));
        p.add(Estilo.boton("Registrar cargo", () -> ctx.ejecutar(this, () -> movimiento(true))));
        p.add(Estilo.boton("Registrar pago", () -> ctx.ejecutar(this, () -> movimiento(false))));
        p.add(Estilo.boton("Deshacer última operación", () -> ctx.ejecutar(this, this::deshacer)));
        p.add(Estilo.boton("Ver folio", () -> ctx.ejecutar(this, () -> Dialogos.folio(this, reservaSeleccionada()))));
        return p;
    }

    private JComponent panelExterno() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setBorder(BorderFactory.createTitledBorder("Canal externo (plataforma de terceros) y vencimientos"));
        p.add(lblExterna);
        p.add(Estilo.boton("Simular solicitud…", () -> ctx.ejecutar(this, this::simularSolicitud)));
        p.add(Estilo.boton("Procesar siguiente", () -> ctx.ejecutar(this, this::procesarSiguiente)));
        p.add(Estilo.boton("Procesar todas", () -> ctx.ejecutar(this, this::procesarTodas)));
        p.add(Estilo.boton("Procesar vencimientos", () -> ctx.ejecutar(this, () -> {
            ListaEnlazada<Reserva> v = ctx.aloj().procesarVencimientos();
            ctx.info(this, v.estaVacia() ? "No hay reservas pendientes vencidas." : "Reservas canceladas por vencimiento: " + v);
        })));
        return p;
    }

    // ------------------------------------------------------------------ lectura de campos

    private Estancia estancia() {
        return new Estancia(Dialogos.fecha(txtEntrada.getText()), Dialogos.fecha(txtSalida.getText()));
    }

    private int personas() {
        return (Integer) spPersonas.getValue();
    }

    private ListaEnlazada<Ocupante> ocupantes() {
        return txtOcupantes.getText().isBlank() ? Dialogos.adultos(personas()) : Dialogos.ocupantes(txtOcupantes.getText());
    }

    private Huesped titular() {
        Huesped h = (Huesped) cmbHuesped.getSelectedItem();
        if (h == null) throw new IllegalStateException("Seleccione o registre un huésped titular");
        return h;
    }

    private Apartamento apartamentoSeleccionado() {
        int f = tablaResultados.getSelectedRow();
        if (f < 0) throw new IllegalStateException("Seleccione un apartamento de los resultados");
        return resultados.obtener(f);
    }

    private Reserva reservaSeleccionada() {
        int f = tablaReservas.getSelectedRow();
        if (f < 0) throw new IllegalStateException("Seleccione una reserva de la tabla");
        return filasReservas.obtener(f);
    }

    // ------------------------------------------------------------------ acciones

    private void buscar() {
        Alojamiento aloj = ctx.aloj();
        Estancia e = estancia();
        Comparator<Apartamento> orden = switch (cmbOrden.getSelectedIndex()) {
            case 1 -> Comparadores.apartamentosPorCapacidad();
            case 2 -> Comparadores.apartamentosPorDormitorios();
            default -> Comparadores.apartamentosPorPrecio(aloj.getCalendario().temporadaDe(e.getEntrada()));
        };
        resultados = aloj.buscarDisponibles(e, personas(), orden);
        modeloResultados.setRowCount(0);
        Temporada t = aloj.getCalendario().temporadaDe(e.getEntrada());
        for (Apartamento a : resultados) {
            double total = Liquidador.total(Liquidador.liquidar(a, e, personas(), aloj.getCalendario()));
            modeloResultados.addRow(new Object[]{a.getCodigo(), a.getNombre(), a.getBloque(), a.getPiso(),
                    a.getDormitorios(), a.getCapacidad(), Estilo.dinero(a.getTarifa(t)), Estilo.dinero(total)});
        }
        if (resultados.estaVacia()) {
            ctx.info(this, "No hay apartamentos libres para " + personas() + " personas en esas fechas.\n"
                    + "Pruebe «Grupo grande» para combinar varios apartamentos.");
        }
    }

    private void cotizar() {
        Dialogos.cotizacion(this, ctx.aloj(), apartamentoSeleccionado(), estancia(), ocupantes());
    }

    private void reservar() {
        Apartamento a = apartamentoSeleccionado();
        Canal canal = (Canal) cmbCanal.getSelectedItem();
        Reserva r = ctx.aloj().crearReserva(titular(), a, estancia(), ocupantes(), canal);
        String msg = "Reserva " + r.getId() + " creada (" + r.getEstado() + ").\nTotal: "
                + Estilo.dinero(r.getFolio().totalCargos());
        if (r.getEstado() == EstadoReserva.PENDIENTE) {
            msg += "\nAnticipo requerido (30 %): " + Estilo.dinero(r.anticipoRequerido(r.getFolio().totalCargos()))
                    + "\nVence el " + r.getVencimiento().format(HORA) + " si no se paga.";
        }
        ctx.info(this, msg);
        buscar();
    }

    private void grupoGrande() {
        Alojamiento aloj = ctx.aloj();
        Estancia e = estancia();
        ListaEnlazada<Ocupante> oc = ocupantes();
        ListaEnlazada<Apartamento> combinacion = aloj.asignarGrupo(e, oc.tamanio());
        StringBuilder sb = new StringBuilder("Combinación con el menor número de apartamentos para "
                + oc.tamanio() + " personas:\n\n");
        int cap = 0;
        for (Apartamento a : combinacion) {
            sb.append("  • ").append(a).append("  (capacidad ").append(a.getCapacidad()).append(")\n");
            cap += a.getCapacidad();
        }
        sb.append("\nApartamentos: ").append(combinacion.tamanio()).append(" · capacidad total: ").append(cap)
                .append("\n\n¿Reservarlos para ").append(titular().getNombre()).append("?");
        if (JOptionPane.showConfirmDialog(this, sb.toString(), "Asignación de grupo grande",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        ListaEnlazada<Reserva> creadas = aloj.reservarGrupo(titular(), e, oc, (Canal) cmbCanal.getSelectedItem());
        ctx.info(this, "Reservas creadas: " + creadas);
    }

    private void pagarAnticipo() {
        double m = ctx.aloj().pagarAnticipo(reservaSeleccionada());
        ctx.info(this, m > 0 ? "Anticipo pagado: " + Estilo.dinero(m) : "El anticipo ya estaba pagado.");
    }

    private void cancelar() {
        Reserva r = reservaSeleccionada();
        double reembolso = ctx.aloj().reembolsoSiCancela(r);
        int ok = JOptionPane.showConfirmDialog(this, "Si cancela la reserva " + r.getId() + " se devolverían "
                + Estilo.dinero(reembolso) + ".\n¿Desea cancelarla?", "Cancelar reserva", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            double devuelto = ctx.aloj().cancelarReserva(r);
            ctx.info(this, "Reserva cancelada. Reembolso: " + Estilo.dinero(devuelto));
        }
    }

    private void movimiento(boolean cargo) {
        Reserva r = reservaSeleccionada();
        Object[] datos = Dialogos.concepto(this, cargo ? "Registrar cargo" : "Registrar pago");
        if (datos == null) return;
        if (cargo) r.getFolio().registrarCargo((Double) datos[1], (String) datos[0]);
        else r.getFolio().registrarPago((Double) datos[1], (String) datos[0]);
    }

    private void deshacer() {
        Reserva r = reservaSeleccionada();
        try {
            Movimiento m = r.getFolio().deshacer();
            ctx.info(this, "Se registró el movimiento contrario: " + m);
        } catch (EstructuraVaciaException ex) {
            ctx.info(this, "Este folio no tiene operaciones para deshacer.");
        }
    }

    private void simularSolicitud() {
        JTextField doc = new JTextField("9001"), nombre = new JTextField("Huésped plataforma"),
                apto = new JTextField(), entrada = new JTextField(txtEntrada.getText()),
                salida = new JTextField(txtSalida.getText());
        JSpinner pers = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Documento:")); p.add(doc);
        p.add(new JLabel("Nombre:")); p.add(nombre);
        p.add(new JLabel("Código de apartamento:")); p.add(apto);
        p.add(new JLabel("Entrada:")); p.add(entrada);
        p.add(new JLabel("Salida:")); p.add(salida);
        p.add(new JLabel("Personas:")); p.add(pers);
        if (JOptionPane.showConfirmDialog(this, p, "Solicitud de la plataforma externa",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        ctx.aloj().recibirSolicitudExterna(doc.getText().trim(), nombre.getText().trim(), apto.getText().trim(),
                new Estancia(Dialogos.fecha(entrada.getText()), Dialogos.fecha(salida.getText())),
                Dialogos.adultos((Integer) pers.getValue()));
    }

    private void procesarSiguiente() {
        if (ctx.aloj().solicitudesPendientes() == 0) {
            ctx.info(this, "No hay solicitudes en la cola.");
            return;
        }
        ctx.info(this, describir(ctx.aloj().procesarSiguienteSolicitud()));
    }

    private void procesarTodas() {
        if (ctx.aloj().solicitudesPendientes() == 0) {
            ctx.info(this, "No hay solicitudes en la cola.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ResultadoSolicitud r : ctx.aloj().procesarTodasLasSolicitudes()) sb.append(describir(r)).append("\n\n");
        ctx.info(this, sb.toString().trim());
    }

    private String describir(ResultadoSolicitud r) {
        return r.aceptada()
                ? "ACEPTADA: " + r.solicitud() + "\n→ reserva " + r.reserva().getId()
                : "RECHAZADA: " + r.solicitud() + "\nMotivo: " + r.motivoRechazo();
    }

    // ------------------------------------------------------------------ actualización

    private void actualizarHuespedes() {
        Object sel = cmbHuesped.getSelectedItem();
        DefaultComboBoxModel<Huesped> m = new DefaultComboBoxModel<>();
        for (Huesped h : ctx.aloj().getHuespedes()) m.addElement(h);
        cmbHuesped.setModel(m);
        if (sel != null) cmbHuesped.setSelectedItem(sel);
    }

    @Override
    public void actualizar() {
        Alojamiento aloj = ctx.aloj();
        if (!iniciado) {
            txtEntrada.setText(aloj.hoy().plusDays(1).toString());
            txtSalida.setText(aloj.hoy().plusDays(3).toString());
            iniciado = true;
        }
        actualizarHuespedes();

        filasReservas = new ListaEnlazada<>();
        int seleccion = tablaReservas.getSelectedRow();
        modeloReservas.setRowCount(0);
        for (Reserva r : aloj.getReservas()) {
            filasReservas.agregarFinal(r);
            Folio f = r.getFolio();
            modeloReservas.addRow(new Object[]{r.getId(), r.getApartamento().getCodigo(), r.getTitular().getNombre(),
                    r.getEstancia().getEntrada(), r.getEstancia().getSalida(), r.getCanal(), r.getEstado(),
                    Estilo.dinero(f.totalCargos()), Estilo.dinero(f.totalPagado()), Estilo.dinero(f.saldo())});
        }
        if (seleccion >= 0 && seleccion < modeloReservas.getRowCount()) {
            tablaReservas.setRowSelectionInterval(seleccion, seleccion);
        }
        lblExterna.setText("Solicitudes en cola: " + aloj.solicitudesPendientes()
                + " · Conflictos registrados: " + aloj.getConflictos().tamanio()
                + " · Pendientes por vencer: " + aloj.getPendientes().tamanio() + "   ");
    }
}
