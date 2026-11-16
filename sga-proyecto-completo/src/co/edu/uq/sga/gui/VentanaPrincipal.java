package co.edu.uq.sga.gui;

import co.edu.uq.sga.estructuras.Pila;
import co.edu.uq.sga.modelo.Empleado;
import co.edu.uq.sga.servicio.Alojamiento;
import co.edu.uq.sga.servicio.CargadorDatos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class VentanaPrincipal extends JFrame {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Contexto ctx;
    private final JTabbedPane pestanas = new JTabbedPane();
    private final Actualizable[] paneles;
    private final JLabel lblReloj = new JLabel();
    private final JLabel lblTurno = new JLabel();
    private final JLabel lblGuardado = new JLabel();

    /** Navegación "atrás" entre pantallas: pila con los índices de pestañas visitadas. */
    private final Pila<Integer> historial = new Pila<>();
    private int pestanaActual = 0;
    private boolean navegandoAtras;

    public VentanaPrincipal(Contexto ctx) {
        super("SGA — Aparta-Suites UQ");
        this.ctx = ctx;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ctx.guardar(); // último guardado al cerrar
            }
        });

        TableroPanel tablero = new TableroPanel(ctx);
        ReservasPanel reservas = new ReservasPanel(ctx);
        CalendarioPanel calendario = new CalendarioPanel(ctx);
        UsuarioPanel usuario = new UsuarioPanel(ctx);
        PreparacionPanel preparacion = new PreparacionPanel(ctx);
        MapaPanel mapa = new MapaPanel(ctx);
        EstadisticasPanel estadisticas = new EstadisticasPanel(ctx);
        AdminPanel admin = new AdminPanel(ctx);
        paneles = new Actualizable[]{tablero, reservas, calendario, usuario, preparacion, mapa, estadisticas, admin};

        pestanas.addTab("Inicio", tablero);
        pestanas.addTab("Reservas", reservas);
        pestanas.addTab("Calendario", calendario);
        pestanas.addTab("Huésped", usuario);
        pestanas.addTab("Preparación", preparacion);
        pestanas.addTab("Mapa", mapa);
        pestanas.addTab("Estadísticas", estadisticas);
        pestanas.addTab("Administración", admin);
        pestanas.addChangeListener(e -> {
            int nueva = pestanas.getSelectedIndex();
            if (!navegandoAtras && nueva != pestanaActual) historial.apilar(pestanaActual);
            pestanaActual = nueva;
            refrescar();
        });

        setLayout(new BorderLayout());
        add(barra(), BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);

        ctx.setRefrescar(this::refrescar);
        refrescar();

        // vencimiento automático de reservas Pendientes (cada 15 s)
        new Timer(15_000, e -> {
            if (!ctx.aloj().procesarVencimientos().estaVacia()) {
                ctx.guardar();
                refrescar();
            }
        }).start();
        new Timer(30_000, e -> actualizarReloj()).start();
    }

    private JComponent barra() {
        JToolBar b = new JToolBar();
        b.setFloatable(false);
        b.add(Estilo.boton("◀ Atrás", this::atras));
        b.addSeparator();
        b.add(Estilo.boton("Cargar escenario de ejemplo", this::cargarEjemplo));
        b.add(Estilo.boton("Cargar archivo…", this::cargarArchivo));
        b.addSeparator();
        b.add(Estilo.boton("Guardar ahora", this::guardarAhora));
        b.add(Estilo.boton("Vaciar datos", this::vaciar));
        b.addSeparator();
        b.add(Estilo.boton("Avanzar reloj 25 h (demo)", () -> {
            ctx.reloj().avanzar(Duration.ofHours(25));
            ctx.ejecutar(this, () -> {
                int n = ctx.aloj().procesarVencimientos().tamanio();
                if (n > 0) ctx.info(this, n + " reserva(s) pendiente(s) vencieron y se cancelaron.");
            });
        }));
        b.add(Box.createHorizontalGlue());
        b.add(lblGuardado);
        b.addSeparator();
        b.add(lblTurno);
        b.addSeparator();
        b.add(Estilo.boton("Rotar turno", () -> ctx.ejecutar(this, () -> ctx.aloj().rotarTurno())));
        b.addSeparator();
        b.add(lblReloj);
        return b;
    }

    private void atras() {
        if (historial.estaVacia()) return;
        navegandoAtras = true;
        pestanas.setSelectedIndex(historial.desapilar());
        navegandoAtras = false;
    }

    private void cargarEjemplo() {
        File f = new File("datos/escenario.txt");
        if (!f.isFile()) {
            cargarArchivo();
            return;
        }
        cargar(f);
    }

    private void cargarArchivo() {
        JFileChooser fc = new JFileChooser(new File("datos").isDirectory() ? new File("datos") : new File("."));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) cargar(fc.getSelectedFile());
    }

    private boolean hayDatos() {
        Alojamiento a = ctx.aloj();
        return !a.getCatalogo().estaVacio() || !a.getHuespedes().estaVacio() || !a.getReservas().estaVacia();
    }

    private void guardarAhora() {
        ctx.guardar();
        refrescar();
        if (ctx.getErrorGuardado() != null) ctx.info(this, "No se pudo guardar: " + ctx.getErrorGuardado());
    }

    private void vaciar() {
        if (JOptionPane.showConfirmDialog(this, "Se borrarán todos los datos (también los guardados).\n¿Continuar?",
                "Vaciar datos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        ctx.reemplazar(new Alojamiento(ctx.reloj()));
        ctx.guardar();
        refrescar();
    }

    private void cargar(File archivo) {
        if (hayDatos() && JOptionPane.showConfirmDialog(this,
                "Cargar un escenario reemplaza los datos actuales (también los guardados).\n¿Continuar?",
                "Cargar escenario", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            Alojamiento nuevo = new Alojamiento(ctx.reloj());
            int lineas = CargadorDatos.cargar(nuevo, Path.of(archivo.getAbsolutePath()));
            ctx.reemplazar(nuevo);
            ctx.guardar();
            refrescar();
            ctx.info(this, "Escenario cargado: " + lineas + " registros.\nApartamentos: " + nuevo.getCatalogo().tamanio()
                    + " · Huéspedes: " + nuevo.getHuespedes().tamanio() + " · Reservas: " + nuevo.getReservas().tamanio());
        } catch (Exception ex) {
            ctx.error(this, ex);
        }
    }

    private void actualizarReloj() {
        lblReloj.setText("  Fecha/hora del sistema: " + ctx.aloj().ahora().format(FORMATO) + "  ");
        lblGuardado.setText(ctx.estadoGuardado() + "  ");
        try {
            Empleado e = ctx.aloj().turnoActual();
            lblTurno.setText("Turno de recepción: " + e.getNombre() + "  ");
        } catch (RuntimeException ex) {
            lblTurno.setText("Turno de recepción: —  ");
        }
    }

    public void refrescar() {
        actualizarReloj();
        // solo se redibuja la pestaña visible; las demás se actualizan al mostrarse
        int i = pestanas.getSelectedIndex();
        if (i >= 0) paneles[i].actualizar();
    }
}
