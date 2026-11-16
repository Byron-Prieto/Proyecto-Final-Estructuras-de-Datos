package co.edu.uq.sga.servicio;

import java.io.Serializable;
import co.edu.uq.sga.estructuras.*;
import co.edu.uq.sga.excepciones.*;
import co.edu.uq.sga.modelo.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Fachada del SGA: reúne las estructuras del sistema y las reglas de negocio.
 * Administra un único alojamiento.
 */
public class Alojamiento implements Serializable {
    private static final long serialVersionUID = 1L;


    public static final String RECEPCION = "RECEPCION";

    private transient Clock reloj; // no se guarda: se reasigna con restaurarReloj()

    // --- catálogos (árboles binarios de búsqueda)
    private final ArbolBinarioBusqueda<Apartamento> catalogo = new ArbolBinarioBusqueda<>();
    private final ArbolBinarioBusqueda<Huesped> huespedes = new ArbolBinarioBusqueda<>();
    private final ArbolBinarioBusqueda<Empleado> empleados = new ArbolBinarioBusqueda<>();

    // --- mapa del edificio
    private final Grafo<String> mapa = new Grafo<>();

    // --- reglas de negocio
    private final CalendarioTemporadas calendario = new CalendarioTemporadas();
    private final ListaEnlazada<Reserva> reservas = new ListaEnlazada<>();
    private int consecutivoReserva;

    // --- colas
    private ColaPrioridad<TareaPreparacion> colaPreparacion = new ColaPrioridad<>(Comparadores.prioridadPreparacion());
    private final ColaPrioridad<Reserva> pendientes = new ColaPrioridad<>(Comparadores.reservasPorVencimiento());
    private final Cola<SolicitudExterna> solicitudes = new Cola<>();

    // --- seguimiento
    private final ListaEnlazada<TareaPreparacion> enPreparacion = new ListaEnlazada<>();
    private final ListaEnlazada<TareaPreparacion> preparadas = new ListaEnlazada<>();
    private final ListaEnlazada<ConflictoCanal> conflictos = new ListaEnlazada<>();
    private final ListaCircular<Empleado> turnos = new ListaCircular<>();

    public Alojamiento() {
        this(Clock.systemDefaultZone());
    }

    public Alojamiento(Clock reloj) {
        this.reloj = reloj;
        mapa.agregarVertice(RECEPCION);
    }

    /** Tras cargar datos guardados hay que devolverle el reloj (y a cada folio). */
    public void restaurarReloj(Clock nuevo) {
        this.reloj = nuevo;
        for (Reserva r : reservas) r.getFolio().setReloj(nuevo);
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    // =====================================================================  registro / búsqueda

    public void registrarApartamento(Apartamento a) {
        if (!catalogo.insertar(a)) throw new IllegalArgumentException("Ya existe el apartamento " + a.getCodigo());
        mapa.agregarVertice(a.getCodigo());
    }

    public Apartamento buscarApartamento(String codigo) {
        return catalogo.buscar(new Apartamento(codigo, "-", "-", 0, 1, 1));
    }

    public void registrarHuesped(Huesped h) {
        if (!huespedes.insertar(h)) throw new IllegalArgumentException("Ya existe el huésped " + h.getDocumento());
    }

    public Huesped buscarHuesped(String documento) {
        return huespedes.buscar(Huesped.llave(documento));
    }

    public void registrarEmpleado(Empleado e) {
        if (!empleados.insertar(e)) throw new IllegalArgumentException("Ya existe el empleado " + e.getDocumento());
        if (e.getRol() == Rol.RECEPCIONISTA) turnos.agregar(e);
    }

    public void eliminarEmpleado(Empleado e) {
        empleados.eliminar(e);
        turnos.eliminar(e);
    }

    public Empleado turnoActual() {
        return turnos.actual();
    }

    public Empleado rotarTurno() {
        return turnos.rotar();
    }

    public void definirTemporada(LocalDate inicio, LocalDate fin, Temporada t) {
        calendario.definir(inicio, fin, t);
    }

    // =====================================================================  mapa

    public void agregarNodoMapa(String nombre) {
        mapa.agregarVertice(nombre);
    }

    public void agregarPasillo(String a, String b, double peso) {
        mapa.agregarArista(a, b, peso);
    }

    public ListaEnlazada<String> inalcanzablesSiSeCierra(String a, String b) {
        return mapa.inalcanzablesSiSeCierra(RECEPCION, a, b);
    }

    /** Apartamentos agrupados por bloque. */
    public ListaEnlazada<ListaEnlazada<Apartamento>> agruparPorBloque() {
        ListaEnlazada<ListaEnlazada<Apartamento>> grupos = new ListaEnlazada<>();
        for (Apartamento a : catalogo) {
            ListaEnlazada<Apartamento> destino = null;
            for (ListaEnlazada<Apartamento> g : grupos) {
                if (g.primero().getBloque().equals(a.getBloque())) {
                    destino = g;
                    break;
                }
            }
            if (destino == null) {
                destino = new ListaEnlazada<>();
                grupos.agregarFinal(destino);
            }
            destino.agregarFinal(a);
        }
        return grupos;
    }

    // =====================================================================  disponibilidad y cotización

    private boolean esReservable(Apartamento a, Estancia e, int personas) {
        return a.getEstado() != EstadoApartamento.FUERA_DE_SERVICIO
                && a.getCapacidad() >= personas
                && a.estaLibre(e);
    }

    /** Apartamentos libres para el rango y el número de personas, en el orden pedido. */
    public ListaEnlazada<Apartamento> buscarDisponibles(Estancia e, int personas, Comparator<Apartamento> orden) {
        ColaPrioridad<Apartamento> cp = new ColaPrioridad<>(orden);
        for (Apartamento a : catalogo) if (esReservable(a, e, personas)) cp.insertar(a);
        ListaEnlazada<Apartamento> r = new ListaEnlazada<>();
        while (!cp.estaVacia()) r.agregarFinal(cp.extraer());
        return r;
    }

    public ListaEnlazada<LineaLiquidacion> cotizar(Apartamento a, Estancia e, ListaEnlazada<Ocupante> ocupantes) {
        int pagantes = 0;
        for (Ocupante o : ocupantes) if (o.pagaEn(e.getEntrada())) pagantes++;
        return Liquidador.liquidar(a, e, pagantes, calendario);
    }

    /** Estado de una noche para el calendario de ocupación. */
    public EstadoNoche estadoNoche(Apartamento a, LocalDate noche) {
        if (a.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO) return EstadoNoche.BLOQUEADA;
        return a.estaLibre(new Estancia(noche, noche.plusDays(1))) ? EstadoNoche.LIBRE : EstadoNoche.OCUPADA;
    }

    // =====================================================================  reservas

    private String siguienteId() {
        return String.format("R-%04d", ++consecutivoReserva);
    }

    /**
     * Crea una reserva por cualquier canal. Portal queda Pendiente (vence en 24 h si no
     * paga el anticipo del 30 %); Directa y Externa se confirman de inmediato.
     */
    public Reserva crearReserva(Huesped titular, Apartamento apto, Estancia e,
                                ListaEnlazada<Ocupante> ocupantes, Canal canal) {
        if (e.getEntrada().isBefore(hoy())) throw new FechasInvalidasException("La entrada no puede ser anterior a hoy");
        if (apto.getEstado() == EstadoApartamento.FUERA_DE_SERVICIO) {
            throw new IllegalStateException("El apartamento " + apto.getCodigo() + " está fuera de servicio: "
                    + apto.getMotivoFueraDeServicio());
        }
        Reserva choque = apto.buscarConflicto(e);
        if (choque != null) {
            throw new ReservaSolapadaException("El apartamento " + apto.getCodigo() + " ya tiene la reserva "
                    + choque.getId() + " (" + choque.getEstancia() + ")");
        }
        Reserva r = new Reserva(siguienteId(), apto, titular, ocupantes, e, canal, ahora());
        r.getFolio().setReloj(reloj);
        apto.agregarReserva(r);
        double total = Liquidador.total(Liquidador.liquidar(apto, e, r.pagantes(), calendario));
        if (total > 0) r.getFolio().registrarCargoInicial(total, "Estancia " + e);
        reservas.agregarFinal(r);
        titular.agregarAlHistorial(r);
        if (canal == Canal.PORTAL) {
            pendientes.insertar(r);
        } else {
            r.cambiarEstado(EstadoReserva.CONFIRMADA);
        }
        if (e.getEntrada().equals(hoy())) reordenarColaPreparacion();
        return r;
    }

    public Reserva buscarReserva(String id) {
        for (Reserva r : reservas) if (r.getId().equals(id)) return r;
        return null;
    }

    public void pagarReserva(Reserva r, double monto, String concepto) {
        if (!r.getEstado().esActiva()) throw new IllegalStateException("La reserva no admite pagos en estado " + r.getEstado());
        r.getFolio().registrarPago(monto, concepto);
        confirmarSiHayAnticipo(r);
    }

    /** Paga lo que falte del anticipo del 30 %. Devuelve el monto pagado. */
    public double pagarAnticipo(Reserva r) {
        double falta = r.anticipoRequerido(r.getFolio().totalCargos()) - r.getFolio().totalPagado();
        if (falta > 0.005) pagarReserva(r, falta, "Anticipo 30 %");
        return Math.max(falta, 0);
    }

    private void confirmarSiHayAnticipo(Reserva r) {
        double requerido = r.anticipoRequerido(r.getFolio().totalCargos());
        if (r.getEstado() == EstadoReserva.PENDIENTE && r.getFolio().totalPagado() >= requerido - 0.005) {
            r.cambiarEstado(EstadoReserva.CONFIRMADA);
            pendientes.eliminar(r);
        }
    }

    /** Cuánto se devolvería si se cancelara hoy. */
    public double reembolsoSiCancela(Reserva r) {
        return r.calcularReembolso(hoy());
    }

    /** Cancela y devuelve el valor reembolsado. */
    public double cancelarReserva(Reserva r) {
        double reembolso = r.calcularReembolso(hoy());
        cerrarSinEstancia(r, EstadoReserva.CANCELADA, reembolso);
        return reembolso;
    }

    public void marcarNoShow(Reserva r) {
        cerrarSinEstancia(r, EstadoReserva.NO_SHOW, 0);
    }

    private void cerrarSinEstancia(Reserva r, EstadoReserva destino, double reembolso) {
        r.cambiarEstado(destino); // valida la transición antes de tocar nada
        r.getApartamento().liberarReserva(r);
        pendientes.eliminar(r);
        Folio f = r.getFolio();
        double retenido = Math.max(f.totalPagado() - reembolso, 0);
        double aAnular = f.totalCargos() - retenido;
        if (reembolso > 0.005) f.registrarDevolucion(reembolso, "Devolución por " + destino);
        if (aAnular > 0.005) f.registrarAnulacionCargo(aAnular, "Cargos anulados por " + destino);
    }

    /** Cancela las reservas Pendientes cuyo plazo (24 h) ya venció. */
    public ListaEnlazada<Reserva> procesarVencimientos() {
        ListaEnlazada<Reserva> vencidas = new ListaEnlazada<>();
        while (!pendientes.estaVacia() && !pendientes.verMinimo().getVencimiento().isAfter(ahora())) {
            Reserva r = pendientes.extraer();
            if (r.getEstado() == EstadoReserva.PENDIENTE) {
                cerrarSinEstancia(r, EstadoReserva.CANCELADA, r.getFolio().totalPagado());
                vencidas.agregarFinal(r);
            }
        }
        return vencidas;
    }

    // =====================================================================  llegadas y salidas

    public void checkIn(Reserva r) {
        if (r.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new TransicionEstadoInvalidaException("Solo se puede hacer check-in a reservas Confirmadas (estado: "
                    + r.getEstado() + ")");
        }
        LocalDate hoy = hoy();
        if (hoy.isBefore(r.getEstancia().getEntrada()) || !hoy.isBefore(r.getEstancia().getSalida())) {
            throw new FechasInvalidasException("Hoy no está dentro de las fechas de la reserva " + r.getEstancia());
        }
        Apartamento a = r.getApartamento();
        if (a.getEstado() != EstadoApartamento.PREPARADO) {
            throw new ApartamentoNoPreparadoException("El apartamento " + a.getCodigo() + " no está Preparado (estado: "
                    + a.getEstado() + ")");
        }
        a.cambiarEstado(EstadoApartamento.OCUPADO);
        r.cambiarEstado(EstadoReserva.EN_CURSO);
    }

    public void checkOut(Reserva r) {
        r.cambiarEstado(EstadoReserva.FINALIZADA); // exige estar En curso
        Apartamento a = r.getApartamento();
        a.liberarReserva(r);
        a.cambiarEstado(EstadoApartamento.PENDIENTE_PREPARACION);
        colaPreparacion.insertar(crearTarea(a, ahora()));
    }

    // =====================================================================  preparación

    private TareaPreparacion crearTarea(Apartamento a, LocalDateTime encoladoEn) {
        LocalDate hoy = hoy();
        for (Reserva r : a.getReservas()) { // in-orden: por fecha de entrada
            if (r.getEstado() == EstadoReserva.CONFIRMADA && r.getEstancia().getEntrada().equals(hoy)) {
                return new TareaPreparacion(a, true, r.getHoraLlegadaEstimada(), encoladoEn);
            }
        }
        return new TareaPreparacion(a, false, null, encoladoEn);
    }

    /** Recalcula prioridades (por ejemplo, si se creó una reserva para hoy). */
    public void reordenarColaPreparacion() {
        ColaPrioridad<TareaPreparacion> nueva = new ColaPrioridad<>(Comparadores.prioridadPreparacion());
        for (TareaPreparacion t : colaPreparacion) nueva.insertar(crearTarea(t.getApartamento(), t.getEncoladoEn()));
        colaPreparacion = nueva;
    }

    public void ponerFueraDeServicio(Apartamento a, String motivo) {
        a.ponerFueraDeServicio(motivo);
        colaPreparacion.eliminar(new TareaPreparacion(a, false, null, null));
    }

    /** Al volver a servicio el apartamento debe prepararse antes de recibir huéspedes. */
    public void devolverAServicio(Apartamento a) {
        a.cambiarEstado(EstadoApartamento.PENDIENTE_PREPARACION);
        colaPreparacion.insertar(crearTarea(a, ahora()));
    }

    /** Atiende el siguiente de la cola y lo pasa a En preparación. */
    public TareaPreparacion despacharPreparacion() {
        TareaPreparacion t = colaPreparacion.extraer();
        t.getApartamento().cambiarEstado(EstadoApartamento.EN_PREPARACION);
        t.setInicio(ahora());
        enPreparacion.agregarFinal(t);
        return t;
    }

    public void terminarPreparacion(Apartamento a) {
        TareaPreparacion tarea = null;
        for (TareaPreparacion t : enPreparacion) {
            if (t.getApartamento().equals(a)) {
                tarea = t;
                break;
            }
        }
        if (tarea == null) throw new IllegalStateException("El apartamento " + a.getCodigo() + " no está en preparación");
        a.cambiarEstado(EstadoApartamento.PREPARADO);
        tarea.setFin(ahora());
        enPreparacion.eliminar(tarea);
        preparadas.agregarFinal(tarea);
    }

    /**
     * Ruta más corta desde recepción: primero los apartamentos con entrada hoy (en el orden
     * de la cola) y luego el resto por vecino más cercano, con Dijkstra.
     */
    public Ruta<String> rutaPreparacion() {
        ListaEnlazada<String> prioridad1 = new ListaEnlazada<>();
        ListaEnlazada<String> prioridad2 = new ListaEnlazada<>();
        for (TareaPreparacion t : colaPreparacion) {
            (t.isEntradaHoy() ? prioridad1 : prioridad2).agregarFinal(t.getApartamento().getCodigo());
        }
        Ruta<String> r1 = mapa.rutaPorDestinos(RECEPCION, prioridad1, true);
        Ruta<String> r2 = mapa.rutaPorDestinos(r1.camino().ultimo(), prioridad2, false);
        ListaEnlazada<String> camino = new ListaEnlazada<>();
        for (String s : r1.camino()) camino.agregarFinal(s);
        boolean primero = true;
        for (String s : r2.camino()) {
            if (primero) {
                primero = false;
                continue;
            }
            camino.agregarFinal(s);
        }
        return new Ruta<>(camino, r1.distancia() + r2.distancia());
    }

    // =====================================================================  canal externo

    public void recibirSolicitudExterna(String documento, String nombre, String codigoApartamento,
                                        Estancia estancia, ListaEnlazada<Ocupante> ocupantes) {
        solicitudes.encolar(new SolicitudExterna(documento, nombre, codigoApartamento, estancia, ocupantes, ahora()));
    }

    public int solicitudesPendientes() {
        return solicitudes.tamanio();
    }

    /** Procesa la solicitud más antigua (FIFO). Si choca con una reserva vigente la rechaza y registra el conflicto. */
    public ResultadoSolicitud procesarSiguienteSolicitud() {
        SolicitudExterna s = solicitudes.desencolar();
        try {
            Apartamento a = buscarApartamento(s.getCodigoApartamento());
            if (a == null) return new ResultadoSolicitud(s, null, "No existe el apartamento " + s.getCodigoApartamento());
            Huesped h = buscarHuesped(s.getDocumento());
            if (h == null) {
                h = new Huesped(s.getDocumento(), s.getNombre(), null, null, null);
                registrarHuesped(h);
            }
            return new ResultadoSolicitud(s, crearReserva(h, a, s.getEstancia(), s.getOcupantes(), Canal.EXTERNA), null);
        } catch (ReservaSolapadaException ex) {
            conflictos.agregarFinal(new ConflictoCanal(s, ex.getMessage(), ahora()));
            return new ResultadoSolicitud(s, null, ex.getMessage());
        } catch (RuntimeException ex) {
            return new ResultadoSolicitud(s, null, ex.getMessage());
        }
    }

    public ListaEnlazada<ResultadoSolicitud> procesarTodasLasSolicitudes() {
        ListaEnlazada<ResultadoSolicitud> r = new ListaEnlazada<>();
        while (!solicitudes.estaVacia()) r.agregarFinal(procesarSiguienteSolicitud());
        return r;
    }

    // =====================================================================  grupos grandes (backtracking)

    private static class Mejor {
        ListaEnlazada<Apartamento> seleccion;
        int capacidad;
    }

    /** Combinación de apartamentos libres que aloja a todo el grupo con el menor número de apartamentos. */
    public ListaEnlazada<Apartamento> asignarGrupo(Estancia e, int personas) {
        ColaPrioridad<Apartamento> cp = new ColaPrioridad<>(Comparadores.apartamentosPorCapacidad().reversed());
        for (Apartamento a : catalogo) if (esReservable(a, e, 1)) cp.insertar(a);
        int n = cp.tamanio();
        Apartamento[] cand = new Apartamento[n];
        for (int i = 0; i < n; i++) cand[i] = cp.extraer();
        int[] sufijo = new int[n + 1];
        for (int i = n - 1; i >= 0; i--) sufijo[i] = sufijo[i + 1] + cand[i].getCapacidad();

        Mejor mejor = new Mejor();
        backtrack(cand, sufijo, 0, personas, new ListaEnlazada<>(), 0, mejor);
        if (mejor.seleccion == null) {
            throw new CapacidadExcedidaException("No hay apartamentos disponibles que alojen a " + personas
                    + " personas del " + e.getEntrada() + " al " + e.getSalida());
        }
        return mejor.seleccion;
    }

    private void backtrack(Apartamento[] c, int[] sufijo, int i, int faltan,
                           ListaEnlazada<Apartamento> actual, int capActual, Mejor mejor) {
        if (faltan <= 0) { // todos alojados: ¿es mejor?
            boolean mejora = mejor.seleccion == null
                    || actual.tamanio() < mejor.seleccion.tamanio()
                    || (actual.tamanio() == mejor.seleccion.tamanio() && capActual < mejor.capacidad);
            if (mejora) {
                ListaEnlazada<Apartamento> copia = new ListaEnlazada<>();
                for (Apartamento a : actual) copia.agregarFinal(a);
                mejor.seleccion = copia;
                mejor.capacidad = capActual;
            }
            return;
        }
        if (i >= c.length || sufijo[i] < faltan) return; // lo que queda no alcanza
        if (mejor.seleccion != null && actual.tamanio() >= mejor.seleccion.tamanio()) return; // poda
        actual.agregarFinal(c[i]); // incluir
        backtrack(c, sufijo, i + 1, faltan - c[i].getCapacidad(), actual, capActual + c[i].getCapacidad(), mejor);
        actual.eliminarUltimo();   // deshacer (backtrack)
        backtrack(c, sufijo, i + 1, faltan, actual, capActual, mejor); // excluir
    }

    /** Reserva un apartamento por cada elemento de la combinación y reparte los ocupantes. */
    public ListaEnlazada<Reserva> reservarGrupo(Huesped titular, Estancia e,
                                                ListaEnlazada<Ocupante> ocupantes, Canal canal) {
        ListaEnlazada<Apartamento> asignados = asignarGrupo(e, ocupantes.tamanio());
        ListaEnlazada<Reserva> resultado = new ListaEnlazada<>();
        Iterator<Ocupante> it = ocupantes.iterator();
        for (Apartamento a : asignados) {
            ListaEnlazada<Ocupante> parte = new ListaEnlazada<>();
            while (parte.tamanio() < a.getCapacidad() && it.hasNext()) parte.agregarFinal(it.next());
            resultado.agregarFinal(crearReserva(titular, a, e, parte, canal));
        }
        return resultado;
    }

    // =====================================================================  administración

    public void actualizarApartamento(Apartamento a, String nombre, String bloque, int piso, int dormitorios,
                                      int capacidad, double baja, double media, double alta, String caracteristicas) {
        for (Reserva r : a.getReservas()) {
            if (r.getOcupantes().tamanio() > capacidad) {
                throw new CapacidadExcedidaException("La reserva " + r.getId() + " tiene " + r.getOcupantes().tamanio()
                        + " ocupantes: la capacidad no puede ser menor");
            }
        }
        a.editar(nombre, bloque, piso, dormitorios, capacidad);
        a.setTarifa(Temporada.BAJA, baja);
        a.setTarifa(Temporada.MEDIA, media);
        a.setTarifa(Temporada.ALTA, alta);
        a.setCaracteristicas(caracteristicas);
    }

    public void eliminarApartamento(Apartamento a) {
        if (!a.getReservas().estaVacio()) {
            throw new IllegalStateException("El apartamento " + a.getCodigo()
                    + " tiene reservas activas: cancélelas o finalícelas antes de eliminarlo");
        }
        if (a.getEstado() == EstadoApartamento.OCUPADO || a.getEstado() == EstadoApartamento.EN_PREPARACION) {
            throw new IllegalStateException("El apartamento " + a.getCodigo() + " está " + a.getEstado()
                    + " y no se puede eliminar");
        }
        colaPreparacion.eliminar(new TareaPreparacion(a, false, null, null));
        catalogo.eliminar(a);
        mapa.eliminarVertice(a.getCodigo());
    }

    public void eliminarHuesped(Huesped h) {
        for (Reserva r : h.getHistorial()) {
            if (r.getEstado().esActiva()) {
                throw new IllegalStateException("El huésped tiene la reserva " + r.getId()
                        + " activa: cancélela o finalícela antes de eliminarlo");
            }
        }
        huespedes.eliminar(h);
    }

    public void eliminarTemporada(CalendarioTemporadas.Periodo p) {
        calendario.eliminar(p);
    }

    public void eliminarPasillo(String a, String b) {
        if (!mapa.eliminarArista(a, b)) throw new IllegalArgumentException("No existe el pasillo " + a + "–" + b);
    }

    /** Solo se pueden eliminar puntos de servicio (escaleras, cuartos de aseo...). */
    public void eliminarNodoMapa(String nombre) {
        if (nombre.equals(RECEPCION) || buscarApartamento(nombre) != null) {
            throw new IllegalArgumentException("Solo se pueden eliminar puntos de servicio (no recepción ni apartamentos)");
        }
        if (!mapa.eliminarVertice(nombre)) throw new IllegalArgumentException("No existe el nodo " + nombre);
    }

    // =====================================================================  acceso para GUI / reportes

    public ArbolBinarioBusqueda<Apartamento> getCatalogo() {
        return catalogo;
    }

    public ArbolBinarioBusqueda<Huesped> getHuespedes() {
        return huespedes;
    }

    public ArbolBinarioBusqueda<Empleado> getEmpleados() {
        return empleados;
    }

    public Grafo<String> getMapa() {
        return mapa;
    }

    public CalendarioTemporadas getCalendario() {
        return calendario;
    }

    public ListaEnlazada<Reserva> getReservas() {
        return reservas;
    }

    public ColaPrioridad<TareaPreparacion> getColaPreparacion() {
        return colaPreparacion;
    }

    public ColaPrioridad<Reserva> getPendientes() {
        return pendientes;
    }

    public Cola<SolicitudExterna> getSolicitudes() {
        return solicitudes;
    }

    public ListaEnlazada<ConflictoCanal> getConflictos() {
        return conflictos;
    }

    public ListaEnlazada<TareaPreparacion> getPreparadas() {
        return preparadas;
    }

    public ListaEnlazada<TareaPreparacion> getEnPreparacion() {
        return enPreparacion;
    }

    public ListaCircular<Empleado> getTurnos() {
        return turnos;
    }

    public Clock getReloj() {
        return reloj;
    }
}
