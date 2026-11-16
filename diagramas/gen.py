# -*- coding: utf-8 -*-
"""Genera los diagramas UML del SGA con Graphviz (fuente .dot, .png, .svg, .pdf)."""
import subprocess

FUENTE = "DejaVu Sans"

def esc(t):
    return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def clase(id_, titulo, attrs=(), metodos=(), estereo=None, head="#DCE6F5", fill="#FFFFFF",
          abstracto=False, nota=None, ancho=None):
    """Nodo UML: nombre, atributos, métodos (y una nota opcional de uso en el SGA)."""
    cab = ""
    if estereo:
        cab += f'<FONT POINT-SIZE="9">«{esc(estereo)}»</FONT><BR/>'
    t = esc(titulo)
    cab += f"<I><B>{t}</B></I>" if abstracto else f"<B>{t}</B>"
    h = [f'<<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="4" BGCOLOR="{fill}">']
    h.append(f'<TR><TD BGCOLOR="{head}">{cab}</TD></TR>')
    def bloque(lineas):
        s = "".join(f'<FONT POINT-SIZE="9">{esc(l)}</FONT><BR ALIGN="LEFT"/>' for l in lineas)
        return f'<TR><TD SIDES="T" ALIGN="LEFT" BALIGN="LEFT">{s}</TD></TR>'
    if attrs:
        h.append(bloque(attrs))
    if metodos:
        h.append(bloque(metodos))
    if nota:
        s = "".join(f'<FONT POINT-SIZE="8" COLOR="#7A4B00">{esc(l)}</FONT><BR ALIGN="LEFT"/>' for l in nota)
        h.append(f'<TR><TD SIDES="T" ALIGN="LEFT" BALIGN="LEFT" BGCOLOR="#FFF4D6">{s}</TD></TR>')
    h.append("</TABLE>>")
    return f'  {id_} [label={"".join(h)}];\n'

def enum(id_, titulo, valores, head="#E6DCF5"):
    return clase(id_, titulo, valores, estereo="enumeration", head=head)

def simple(id_, titulo, head="#EEEEEE", estereo=None):
    return clase(id_, titulo, estereo=estereo, head=head)

ESTILOS = {
    "herencia":  'arrowhead=onormal',
    "realiza":   'arrowhead=onormal, style=dashed',
    "compone":   'dir=back, arrowtail=diamond',
    "agrega":    'dir=back, arrowtail=odiamond',
    "asocia":    'arrowhead=vee',
    "asocia_n":  'arrowhead=none',
    "depende":   'arrowhead=vee, style=dashed',
}

def rel(a, b, tipo, label="", tail="", head="", extra=""):
    attrs = [ESTILOS[tipo]]
    if label: attrs.append(f'label="{label}"')
    if tail: attrs.append(f'taillabel="{tail}"')
    if head: attrs.append(f'headlabel="{head}"')
    if extra: attrs.append(extra)
    return f'  {a} -> {b} [{", ".join(attrs)}];\n'

def grafo(nombre, titulo, cuerpo, rankdir="TB", extra=""):
    g = f'''digraph {nombre} {{
  graph [fontname="{FUENTE}", fontsize=18, label="{titulo}", labelloc=t, rankdir={rankdir},
         splines=true, nodesep=0.45, ranksep=0.75, pad=0.4, bgcolor="white" {extra}];
  node  [shape=plain, fontname="{FUENTE}", fontsize=10];
  edge  [fontname="{FUENTE}", fontsize=9, color="#444444", labeldistance=2.2, labelangle=-25];
{cuerpo}}}
'''
    open(f"{nombre}.dot", "w", encoding="utf-8").write(g)
    for fmt, extra_args in (("png", ["-Gdpi=170"]), ("svg", []), ("pdf", [])):
        subprocess.run(["dot", f"-T{fmt}", *extra_args, f"{nombre}.dot", "-o", f"{nombre}.{fmt}"], check=True)
    print("ok", nombre)

# =====================================================================  A. DOMINIO
A = ""
A += clase("Apartamento", "Apartamento", [
    "-codigo: String {id}", "-nombre: String", "-bloque: String", "-piso: int", "-dormitorios: int",
    "-capacidad: int", "-caracteristicas: ListaEnlazada<String>", "-tarifas: double[] (una por Temporada)",
    "-estado: EstadoApartamento", "-motivoFueraDeServicio: String",
    "-reservas: ArbolBinarioBusqueda<Reserva>", "-totalReservas: int"], [
    "+cambiarEstado(EstadoApartamento)", "+ponerFueraDeServicio(motivo: String)",
    "+buscarConflicto(Estancia): Reserva   «recursivo»", "+estaLibre(Estancia): boolean",
    "+agregarReserva(Reserva)", "+liberarReserva(Reserva): boolean",
    "+getTarifa(Temporada): double", "+compareTo(Apartamento): int   «por código»"])
A += clase("Huesped", "Huesped", [
    "-documento: String {id}", "-nombre: String", "-fechaNacimiento: LocalDate", "-telefono: String",
    "-correo: String", "-fotografia: String  (opcional)", "-favoritos: Conjunto<Apartamento>",
    "-historial: ListaEnlazada<Reserva>"], [
    "+agregarFavorito(Apartamento): boolean", "+quitarFavorito(Apartamento): boolean",
    "+agregarAlHistorial(Reserva)", "+compareTo(Huesped): int   «por documento»"])
A += clase("Reserva", "Reserva", [
    "-id: String", "-estado: EstadoReserva", "-canal: Canal", "-creadaEn: LocalDateTime",
    "-horaLlegadaEstimada: LocalTime", "-ocupantes: ListaEnlazada<Ocupante>",
    "+PORCENTAJE_ANTICIPO = 0.30", "+HORAS_VENCIMIENTO = 24"], [
    "+cambiarEstado(EstadoReserva)", "+pagantes(): int", "+getVencimiento(): LocalDateTime",
    "+anticipoRequerido(total: double): double", "+calcularReembolso(hoy: LocalDate): double",
    "+compareTo(Reserva): int   «por fecha de entrada»"])
A += clase("Estancia", "Estancia", ["-entrada: LocalDate", "-salida: LocalDate  (no se cobra)"], [
    "+noches(): int", "+solapa(Estancia): boolean", "+incluyeNoche(LocalDate): boolean",
    "+iterator(): Iterator<LocalDate>   «una por noche»"])
A += clase("Ocupante", "Ocupante", ["-nombre: String", "-fechaNacimiento: LocalDate",
                                     "+EDAD_MINIMA_PAGO = 6"], [
    "+edadEn(LocalDate): int", "+pagaEn(LocalDate): boolean"])
A += clase("Folio", "Folio", ["-movimientos: ListaEnlazada<Movimiento>", "-operaciones: Pila<Movimiento>"], [
    "+registrarCargo(monto, concepto): Movimiento", "+registrarPago(monto, concepto): Movimiento",
    "+deshacer(): Movimiento   «registra el contrario»", "+saldo(): double", "+totalCargos(): double",
    "+totalPagado(): double"])
A += clase("Movimiento", "Movimiento", ["-tipo: TipoMovimiento", "-monto: double", "-concepto: String",
                                         "-fecha: LocalDateTime"], estereo="inmutable")
A += clase("TareaPreparacion", "TareaPreparacion", [
    "-entradaHoy: boolean", "-horaLlegada: LocalTime", "-encoladoEn: LocalDateTime",
    "-inicio: LocalDateTime", "-fin: LocalDateTime"], ["+isEntradaHoy(): boolean"],
    nota=["Elemento de la cola de preparación"])
A += clase("Empleado", "Empleado", ["-documento: String {id}", "-nombre: String"],
           ["+compareTo(Empleado): int   «por documento»"])
A += clase("CalendarioTemporadas", "CalendarioTemporadas", ["-periodos: ListaEnlazada<Periodo>"], [
    "+definir(inicio, fin, Temporada)", "+eliminar(Periodo): boolean", "+temporadaDe(LocalDate): Temporada"])
A += clase("Periodo", "Periodo", ["inicio: LocalDate", "fin: LocalDate  (inclusive)"], estereo="record")
A += clase("Liquidador", "Liquidador", metodos=[
    "+liquidar(Apartamento, Estancia, pagantes, CalendarioTemporadas)",
    "      : ListaEnlazada<LineaLiquidacion>   «recursivo»", "+total(ListaEnlazada<LineaLiquidacion>): double"],
    estereo="utilidad", head="#E8F0E0",
    nota=["Usa: Apartamento (tarifas), Estancia (Iterator),", "CalendarioTemporadas → produce LineaLiquidacion"])
A += clase("LineaLiquidacion", "LineaLiquidacion", ["noche: LocalDate", "tarifa: double", "pagantes: int",
                                                    "valor: double"], estereo="record")
A += clase("Comparadores", "Comparadores", metodos=[
    "+apartamentosPorPrecio(Temporada)", "+apartamentosPorCapacidad()", "+apartamentosPorDormitorios()",
    "+apartamentosPorNumeroReservas()", "+prioridadPreparacion()   «thenComparing»",
    "+reservasPorVencimiento()"], estereo="utilidad  (Comparator)", head="#E8F0E0",
    nota=["Ordena: Apartamento, TareaPreparacion, Reserva"])
A += clase("PoliticaCancelacion", "PoliticaCancelacion", metodos=[
    "+porcentajeReembolso(hoy, entrada): double"], estereo="utilidad", head="#E8F0E0",
    nota=["Usada por Reserva.calcularReembolso", "Supuesto: ≥7 días 100 %, 3–6 días 50 %, menos 0 %"])
A += enum("EstadoApartamento", "EstadoApartamento", ["PREPARADO", "OCUPADO", "PENDIENTE_PREPARACION",
                                                     "EN_PREPARACION", "FUERA_DE_SERVICIO"])
A += enum("EstadoReserva", "EstadoReserva", ["PENDIENTE", "CONFIRMADA", "EN_CURSO", "FINALIZADA",
                                             "CANCELADA", "NO_SHOW"])
A += enum("Temporada", "Temporada", ["BAJA", "MEDIA", "ALTA"])
A += enum("Canal", "Canal", ["PORTAL", "DIRECTA", "EXTERNA"])
A += enum("TipoMovimiento", "TipoMovimiento", ["CARGO", "PAGO", "ANULACION_CARGO", "ANULACION_PAGO"])
A += enum("Rol", "Rol", ["RECEPCIONISTA", "ADMINISTRADOR"])
A += clase("Comparable", "Comparable<T>", estereo="interface", head="#F5F5D5")
A += clase("Iterable", "Iterable<T>", estereo="interface", head="#F5F5D5")

A += rel("Apartamento", "Comparable", "realiza")
A += rel("Huesped", "Comparable", "realiza")
A += rel("Reserva", "Comparable", "realiza")
A += rel("Empleado", "Comparable", "realiza")
A += rel("Estancia", "Iterable", "realiza")
A += rel("Huesped", "Reserva", "asocia_n", "titular / historial", "1", "0..*")
A += rel("Huesped", "Apartamento", "asocia", "favoritos", "", "0..*")
A += rel("Apartamento", "Reserva", "agrega", "reservas activas\\n(árbol por fecha de entrada)", "1", "0..*")
A += rel("Reserva", "Estancia", "compone", "", "", "1")
A += rel("Reserva", "Ocupante", "compone", "", "", "1..*")
A += rel("Reserva", "Folio", "compone", "", "", "1")
A += rel("Folio", "Movimiento", "compone", "", "", "0..*")
A += rel("TareaPreparacion", "Apartamento", "asocia", "", "", "1")
A += rel("CalendarioTemporadas", "Periodo", "compone", "", "", "0..*")
A += rel("Reserva", "EstadoReserva", "asocia")
A += rel("Reserva", "Canal", "asocia")
A += rel("Apartamento", "EstadoApartamento", "asocia")
A += rel("Apartamento", "Temporada", "asocia", "tarifa por", "", "3")
A += rel("Periodo", "Temporada", "asocia")
A += rel("Movimiento", "TipoMovimiento", "asocia")
A += rel("Empleado", "Rol", "asocia")
A += rel("LineaLiquidacion", "Temporada", "asocia")
grafo("A_diagrama_clases_dominio", "Diagrama de clases — Modelo de dominio del SGA", A, extra=', ranksep=0.9, nodesep=0.55')

# =====================================================================  B. SERVICIOS Y EXCEPCIONES
B = ""
B += clase("Alojamiento", "Alojamiento", [
    "-reloj: Clock", "-catalogo: ArbolBinarioBusqueda<Apartamento>", "-huespedes: ArbolBinarioBusqueda<Huesped>",
    "-empleados: ArbolBinarioBusqueda<Empleado>", "-mapa: Grafo<String>", "-calendario: CalendarioTemporadas",
    "-reservas: ListaEnlazada<Reserva>", "-colaPreparacion: ColaPrioridad<TareaPreparacion>",
    "-pendientes: ColaPrioridad<Reserva>   (por vencimiento)", "-solicitudes: Cola<SolicitudExterna>",
    "-turnos: ListaCircular<Empleado>", "-conflictos: ListaEnlazada<ConflictoCanal>",
    "-enPreparacion, preparadas: ListaEnlazada<TareaPreparacion>"], [
    "+buscarDisponibles(Estancia, personas, Comparator<Apartamento>): ListaEnlazada<Apartamento>",
    "+cotizar(Apartamento, Estancia, ocupantes): ListaEnlazada<LineaLiquidacion>",
    "+crearReserva(Huesped, Apartamento, Estancia, ocupantes, Canal): Reserva",
    "+pagarAnticipo(Reserva): double      +pagarReserva(Reserva, monto, concepto)",
    "+reembolsoSiCancela(Reserva): double  +cancelarReserva(Reserva): double  +marcarNoShow(Reserva)",
    "+procesarVencimientos(): ListaEnlazada<Reserva>",
    "+checkIn(Reserva)      +checkOut(Reserva)",
    "+despacharPreparacion(): TareaPreparacion   +terminarPreparacion(Apartamento)",
    "+rutaPreparacion(): Ruta<String>   «Dijkstra»",
    "+recibirSolicitudExterna(...)   +procesarSiguienteSolicitud(): ResultadoSolicitud",
    "+asignarGrupo(Estancia, personas): ListaEnlazada<Apartamento>   «backtracking»",
    "+reservarGrupo(...): ListaEnlazada<Reserva>",
    "+inalcanzablesSiSeCierra(a, b)   +agruparPorBloque()   +estadoNoche(Apartamento, LocalDate)",
    "+registrarApartamento / actualizarApartamento / eliminarApartamento",
    "+registrarHuesped / eliminarHuesped   +registrarEmpleado / eliminarEmpleado",
    "+definirTemporada(...)   +turnoActual()   +rotarTurno()"],
    estereo="fachada, Serializable", head="#FAD9C1")
B += clase("Reportes", "Reportes", metodos=[
    "+nochesOcupadas(Apartamento, desde, hasta): int", "+porcentajeOcupacion(...): double",
    "+nochesPorTemporada(): int[]", "+ingresos(desde, hasta): double", "+reservasPorCanal(Canal): int",
    "+reservasPorCanalYEstado(Canal, EstadoReserva): int", "+conflictosDeCanal(): int",
    "+tiempoPromedioPreparacionMinutos(): double"], head="#FAD9C1")
B += clase("CargadorDatos", "CargadorDatos", metodos=["+cargar(Alojamiento, Path): int", "+cargar(Alojamiento, Reader): int"],
           estereo="utilidad", head="#E8F0E0", nota=["Lee el archivo plano de escenario (datos/escenario.txt)"])
B += clase("Persistencia", "Persistencia", metodos=["+guardar(Alojamiento, Path)", "+cargar(Path, Clock): Alojamiento"],
           estereo="utilidad", head="#E8F0E0",
           nota=["Serialización de Java: guarda todo el estado", "en datos/sga.dat (escritura atómica, hilo con pila grande)"])
B += clase("SolicitudExterna", "SolicitudExterna", [
    "-documento, nombre: String", "-codigoApartamento: String", "-estancia: Estancia",
    "-ocupantes: ListaEnlazada<Ocupante>", "-recibidaEn: LocalDateTime"], head="#FAD9C1")
B += clase("ConflictoCanal", "ConflictoCanal", ["solicitud: SolicitudExterna", "motivo: String", "fecha: LocalDateTime"],
           estereo="record", head="#FAD9C1")
B += clase("ResultadoSolicitud", "ResultadoSolicitud", ["solicitud: SolicitudExterna", "reserva: Reserva",
                                                        "motivoRechazo: String"], ["+aceptada(): boolean"],
           estereo="record", head="#FAD9C1")
B += enum("EstadoNoche", "EstadoNoche", ["LIBRE", "OCUPADA", "BLOQUEADA"], head="#FAD9C1")
B += clase("Clock", "Clock", estereo="java.time", head="#F5F5D5")
B += clase("RelojAjustable", "RelojAjustable", ["-desfase: Duration"], [
    "+enMarcha(): RelojAjustable", "+avanzar(Duration)"], head="#FAD9C1",
    nota=["Permite adelantar el tiempo en la demostración"])
# dominio (referencias)
for n in ("Apartamento", "Huesped", "Reserva", "Empleado", "TareaPreparacion", "CalendarioTemporadas"):
    B += simple(n, n, head="#DCE6F5")
# estructuras (referencias)
for n, t in (("ABB", "ArbolBinarioBusqueda<T>"), ("Grafo", "Grafo<T>"), ("CP", "ColaPrioridad<T>"),
             ("Cola", "Cola<T>"), ("LC", "ListaCircular<T>"), ("LE", "ListaEnlazada<T>")):
    B += simple(n, t, head="#D5EFE3")
B += rel("Alojamiento", "ABB", "compone", "catalogo, huespedes,\\nempleados", "", "3")
B += rel("Alojamiento", "Grafo", "compone", "mapa")
B += rel("Alojamiento", "CP", "compone", "colaPreparacion,\\npendientes", "", "2")
B += rel("Alojamiento", "Cola", "compone", "solicitudes")
B += rel("Alojamiento", "LC", "compone", "turnos")
B += rel("Alojamiento", "LE", "compone", "reservas, conflictos,\\npreparadas...")
B += rel("Alojamiento", "CalendarioTemporadas", "compone")
B += rel("Alojamiento", "Apartamento", "agrega", "", "", "0..*")
B += rel("Alojamiento", "Huesped", "agrega", "", "", "0..*")
B += rel("Alojamiento", "Reserva", "agrega", "", "", "0..*")
B += rel("Alojamiento", "Empleado", "agrega", "", "", "0..*")
B += rel("Alojamiento", "TareaPreparacion", "agrega", "", "", "0..*")
B += rel("Alojamiento", "SolicitudExterna", "depende")
B += rel("Alojamiento", "ConflictoCanal", "depende")
B += rel("Alojamiento", "ResultadoSolicitud", "depende")
B += rel("Alojamiento", "EstadoNoche", "depende")
B += rel("Alojamiento", "Clock", "asocia", "reloj")
B += rel("RelojAjustable", "Clock", "herencia")
B += rel("Reportes", "Alojamiento", "asocia", "", "", "1")
B += rel("CargadorDatos", "Alojamiento", "depende")
B += rel("Persistencia", "Alojamiento", "depende")
B += rel("SolicitudExterna", "ConflictoCanal", "agrega", "", "", "")
grafo("B_diagrama_clases_servicios", "Diagrama de clases — Servicios y reglas de negocio", B, rankdir="LR",
      extra=', ranksep=1.3, nodesep=0.3')
E = ""
E += clase("RuntimeException", "RuntimeException", estereo="java.lang", head="#F5F5D5")
exc = [
    ("ReservaSolapadaException", ["Dos reservas activas comparten una noche"],
     ["Apartamento.agregarReserva", "Alojamiento.crearReserva"]),
    ("CapacidadExcedidaException", ["Más ocupantes que la capacidad"],
     ["Reserva (constructor)", "Alojamiento.asignarGrupo", "Alojamiento.actualizarApartamento"]),
    ("FechasInvalidasException", ["Salida no posterior a entrada, fechas nulas,", "periodos de temporada cruzados"],
     ["Estancia (constructor)", "CalendarioTemporadas.definir", "Alojamiento.crearReserva / checkIn"]),
    ("TransicionEstadoInvalidaException", ["Cambio de estado no permitido"],
     ["Apartamento.cambiarEstado", "Reserva.cambiarEstado", "Alojamiento.checkIn"]),
    ("EstructuraVaciaException", ["Extraer/consultar de una estructura vacía"],
     ["Cola, Pila, ColaPrioridad,", "ListaEnlazada, ListaCircular"]),
    ("ApartamentoNoPreparadoException", ["Check-in con apartamento no Preparado"], ["Alojamiento.checkIn"]),
]
for nombre, desc, donde in exc:
    E += clase(nombre, nombre, desc, head="#F7D4D4", nota=["Lanzada por:"] + ["  " + d for d in donde])
    E += rel(nombre, "RuntimeException", "herencia")

grafo("E_diagrama_excepciones", "Excepciones propias del SGA", E, rankdir="LR", extra=', ranksep=1.2, nodesep=0.35')


# =====================================================================  C. GUI Y PAQUETES
C = ""
panel = "#DCEBFA"
C += clase("Main", "Main", metodos=["+main(String[])"], head="#FAD9C1")
C += clase("VentanaPrincipal", "VentanaPrincipal", [
    "-pestanas: JTabbedPane", "-historial: Pila<Integer>   (navegación «Atrás»)"], [
    "-atras()", "-cargarEjemplo() / cargarArchivo()", "+refrescar()"], estereo="JFrame", head="#FAD9C1",
    nota=["Timer: cancela reservas pendientes vencidas"])
C += clase("Contexto", "Contexto", ["-aloj: Alojamiento", "-reloj: RelojAjustable", "-ruta: Ruta<String>"], [
    "+aloj(): Alojamiento", "+reemplazar(Alojamiento)", "+refrescar()", "+ejecutar(Component, Runnable)  «guarda tras cada operación»",
    "+guardar()", "+iniciarDesdeArchivo(): String",
    "+error(Component, Exception)"], head="#FAD9C1", nota=["Estado compartido entre los paneles"])
C += clase("Actualizable", "Actualizable", metodos=["+actualizar()"], estereo="interface", head="#F5F5D5",
           nota=["Implementada por los 8 paneles", "(cada uno se redibuja con el estado actual)"])
paneles = [
    ("TableroPanel", "Inicio: tablero de apartamentos por estado (verde/azul/amarillo/rojo)"),
    ("ReservasPanel", "Disponibilidad, reservas, folio, check-in/out, canal externo"),
    ("CalendarioPanel", "Cuadrícula apartamentos × noches del mes"),
    ("UsuarioPanel", "Huésped: perfil, reservas, saldo, favoritos, historial"),
    ("PreparacionPanel", "Cola de preparación y ruta sugerida"),
    ("MapaPanel", "Grafo interactivo del edificio y rutas (Dijkstra)"),
    ("EstadisticasPanel", "Ocupación, ingresos, canales, conflictos, preparación"),
    ("AdminPanel", "Apartamentos, huéspedes, empleados, temporadas, mapa"),
]
for n, d in paneles:
    C += clase(n, n, estereo="JPanel, Actualizable", nota=[d], head=panel)
    C += rel(n, "Contexto", "asocia")
    C += rel("VentanaPrincipal", n, "compone")
C += clase("Dialogos", "Dialogos", metodos=["+fecha(String)", "+ocupantes(String)", "+nuevoHuesped(...)",
                                           "+cotizacion(...)", "+folio(...)"], estereo="utilidad", head="#E8F0E0")
C += clase("Estilo", "Estilo", metodos=["+colorEstado(EstadoApartamento): Color", "+tabla(...)", "+boton(...)",
                                       "+dinero(double): String"], estereo="utilidad", head="#E8F0E0", nota=["Colores de estado y componentes comunes"])
C += clase("GraficoBarras", "GraficoBarras", metodos=["+setDatos(String[], double[])"], estereo="JPanel", head=panel)
C += clase("Lienzo", "Lienzo (clase interna de MapaPanel)", [
    "-nombres, x, y: disposición de nodos", "-ea, eb, ep: pasillos"], [
    "+reorganizar()   «Fruchterman–Reingold»", "#paintComponent(Graphics)", "arrastre de nodos con el mouse"],
    estereo="JPanel", head=panel)
C += clase("Alojamiento", "Alojamiento", estereo="servicio", head="#FAD9C1")
C += rel("Main", "VentanaPrincipal", "depende", "crea")
C += rel("VentanaPrincipal", "Contexto", "asocia")
C += rel("Contexto", "Alojamiento", "asocia", "", "", "1")
C += rel("MapaPanel", "Lienzo", "compone")
C += rel("EstadisticasPanel", "GraficoBarras", "compone", "", "", "3")
for n in ("ReservasPanel", "UsuarioPanel", "AdminPanel"):
    C += rel(n, "Dialogos", "depende")
grafo("C_diagrama_clases_gui", "Diagrama de clases — Interfaz gráfica (Swing)", C, extra=', ranksep=0.9, nodesep=0.3')

# =====================================================================  P. PAQUETES
P = '''  node [shape=plain];
'''
def paquete(id_, titulo, contenido, color):
    s = esc(contenido).replace("\n", '<BR ALIGN="LEFT"/>')
    return (f'  {id_} [label=<<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="6" BGCOLOR="{color}">'
            f'<TR><TD ALIGN="LEFT"><B>{esc(titulo)}</B></TD></TR>'
            f'<TR><TD SIDES="T" ALIGN="LEFT" BALIGN="LEFT"><FONT POINT-SIZE="10">{s}<BR ALIGN="LEFT"/></FONT></TD></TR></TABLE>>];\n')
P += paquete("gui", "co.edu.uq.sga.gui", "Main, VentanaPrincipal, Contexto\nTableroPanel, ReservasPanel, CalendarioPanel,\nUsuarioPanel, PreparacionPanel, MapaPanel,\nEstadisticasPanel, AdminPanel\nDialogos, Estilo, GraficoBarras, Actualizable", "#FAD9C1")
P += paquete("servicio", "co.edu.uq.sga.servicio", "Alojamiento (fachada), Reportes, CargadorDatos\nSolicitudExterna, ConflictoCanal, ResultadoSolicitud\nEstadoNoche, RelojAjustable", "#FFE9B8")
P += paquete("modelo", "co.edu.uq.sga.modelo", "Apartamento, Huesped, Reserva, Estancia, Ocupante\nFolio, Movimiento, TareaPreparacion, Empleado\nCalendarioTemporadas, Liquidador, LineaLiquidacion\nComparadores, PoliticaCancelacion\nenums: EstadoApartamento, EstadoReserva, Temporada,\nCanal, TipoMovimiento, Rol", "#DCE6F5")
P += paquete("estructuras", "co.edu.uq.sga.estructuras", "ListaEnlazada<T>, Cola<T>, Pila<T>, ColaPrioridad<T>\nArbolBinarioBusqueda<T>, Grafo<T>, Conjunto<T>\nListaCircular<T>, Ruta<T>", "#D5EFE3")
P += paquete("excepciones", "co.edu.uq.sga.excepciones", "ReservaSolapadaException, CapacidadExcedidaException\nFechasInvalidasException, TransicionEstadoInvalidaException\nEstructuraVaciaException, ApartamentoNoPreparadoException", "#F7D4D4")
P += '  gui -> servicio [style=dashed, arrowhead=vee, label="usa"];\n'
P += '  servicio -> modelo [style=dashed, arrowhead=vee, label="usa"];\n'
P += '  modelo -> estructuras [style=dashed, arrowhead=vee, label="usa"];\n'
P += '  servicio -> estructuras [style=dashed, arrowhead=vee];\n'
P += '  gui -> modelo [style=dashed, arrowhead=vee];\n'
P += '  servicio -> excepciones [style=dashed, arrowhead=vee];\n'
P += '  modelo -> excepciones [style=dashed, arrowhead=vee];\n'
P += '  estructuras -> excepciones [style=dashed, arrowhead=vee];\n'
grafo("P_diagrama_paquetes", "Arquitectura por capas — Paquetes del SGA", P, extra=', nodesep=0.7, ranksep=0.9')

# =====================================================================  D. ESTRUCTURAS PROPIAS
D = ""
verde = "#D5EFE3"
D += clase("Iterable", "Iterable<T>", metodos=["+iterator(): Iterator<T>"], estereo="interface", head="#F5F5D5")
D += clase("ListaEnlazada", "ListaEnlazada<T>", [
    "-cabeza: Nodo<T>", "-cola: Nodo<T>", "-tamanio: int"], [
    "+agregarInicio(T)   +agregarFinal(T)", "+insertar(int, T)   +obtener(int): T", "+primero(): T   +ultimo(): T",
    "+eliminar(T): boolean   +eliminarEn(int): T", "+eliminarPrimero(): T   +eliminarUltimo(): T",
    "+contiene(T)   +indiceDe(T): int", "+iterator()   +iteradorInverso()"], head=verde,
    nota=["Uso en el SGA:", "• historial de estancias del huésped", "• ocupantes de una reserva",
          "• desglose de noches liquidadas", "• movimientos del folio"])
D += clase("NodoLE", "Nodo<T>", ["dato: T", "sig: Nodo<T>", "ant: Nodo<T>"], estereo="clase interna", head="#EEEEEE")
D += clase("Cola", "Cola<T>", ["-frente: Nodo<T>", "-fin: Nodo<T>", "-tamanio: int"], [
    "+encolar(T)", "+desencolar(): T", "+frente(): T", "+estaVacia(): boolean", "+iterator()"], head=verde,
    nota=["Uso en el SGA:", "• solicitudes del canal externo (FIFO)", "• recorrido BFS del grafo"])
D += clase("NodoC", "Nodo<T>", ["dato: T", "sig: Nodo<T>"], estereo="clase interna", head="#EEEEEE")
D += clase("Pila", "Pila<T>", ["-cima: Nodo<T>", "-tamanio: int"], [
    "+apilar(T)", "+desapilar(): T", "+cima(): T", "+estaVacia(): boolean", "+iterator()"], head=verde,
    nota=["Uso en el SGA:", "• deshacer la última operación del folio", "• navegación «Atrás» entre pantallas"])
D += clase("NodoP", "Nodo<T>", ["dato: T", "sig: Nodo<T>"], estereo="clase interna", head="#EEEEEE")
D += clase("ColaPrioridad", "ColaPrioridad<T>", [
    "-heap: Object[]   (montículo binario)", "-tamanio: int", "-comparador: Comparator<? super T>"], [
    "+insertar(T)", "+extraer(): T", "+verMinimo(): T", "+eliminar(T): boolean",
    "+iterator()   «en orden de prioridad»"], head=verde,
    nota=["Uso en el SGA:", "• cola de preparación (entrada hoy primero)",
          "• reservas Pendientes por vencimiento", "• Dijkstra  • orden de resultados de búsqueda"])
D += clase("ABB", "ArbolBinarioBusqueda<T extends Comparable<T>>", [
    "-raiz: Nodo<T>", "-tamanio: int"], [
    "+insertar(T): boolean   «recursivo»", "+buscar(T): T   «recursivo»", "+eliminar(T): boolean   «recursivo»",
    "+altura(): int   «recursivo»", "+inOrden() / preOrden() / postOrden(): ListaEnlazada<T>",
    "+getRaiz(): Nodo<T>", "+iterator()   «in-orden»"], head=verde,
    nota=["Uso en el SGA:", "• catálogo de apartamentos (por código)", "• huéspedes (por documento)",
          "• empleados (por documento)", "• reservas de cada apartamento (por fecha de entrada)",
          "  → detección de solapamientos recursiva"])
D += clase("NodoABB", "Nodo<T>", ["dato: T", "izq: Nodo<T>", "der: Nodo<T>"], estereo="clase interna", head="#EEEEEE")
D += clase("Grafo", "Grafo<T>", ["-vertices: ListaEnlazada<Vertice<T>>"], [
    "+agregarVertice(T)   +eliminarVertice(T)", "+agregarArista(a, b, peso)   +eliminarArista(a, b)",
    "+bfs(T): ListaEnlazada<T>", "+dfs(T): ListaEnlazada<T>   «recursivo»", "+componentesConexas()",
    "+inalcanzablesSiSeCierra(origen, a, b)", "+dijkstra(origen, destino): Ruta<T>",
    "+rutaPorDestinos(origen, destinos, respetarOrden)"], head=verde,
    nota=["Uso en el SGA:", "• mapa del edificio: apartamentos, recepción,", "  escaleras y cuartos de aseo (nodos)",
          "• pasillos con peso en metros (aristas)", "• ruta de preparación (Dijkstra) y análisis de cierres"])
D += clase("Vertice", "Vertice<T>", ["dato: T", "indice: int", "adyacentes: ListaEnlazada<Arista<T>>"],
           estereo="clase interna", head="#EEEEEE")
D += clase("Arista", "Arista<T>", ["destino: Vertice<T>", "peso: double"], estereo="clase interna", head="#EEEEEE")
D += clase("Conjunto", "Conjunto<T>", ["-elementos: ListaEnlazada<T>"], [
    "+agregar(T): boolean   «sin repetidos»", "+eliminar(T)   +contiene(T)", "+union(Conjunto<T>)",
    "+interseccion(Conjunto<T>)", "+iterator()"], head=verde,
    nota=["Uso en el SGA:", "• apartamentos favoritos de cada huésped"])
D += clase("ListaCircular", "ListaCircular<T>", ["-ultimo: Nodo<T>", "-actual: Nodo<T>", "-tamanio: int"], [
    "+agregar(T)", "+actual(): T", "+rotar(): T", "+eliminar(T): boolean", "+iterator()   «una vuelta»"], head=verde,
    nota=["Uso en el SGA:", "• rotación de turnos de los recepcionistas"])
D += clase("NodoLC", "Nodo<T>", ["dato: T", "sig: Nodo<T>  (el último apunta al primero)"], estereo="clase interna", head="#EEEEEE")
D += clase("Ruta", "Ruta<T>", ["camino: ListaEnlazada<T>", "distancia: double"], estereo="record", head=verde)
D += clase("Vacia", "EstructuraVaciaException", estereo="extends RuntimeException", head="#F7D4D4",
           nota=["Se lanza al desencolar, desapilar, extraer", "o consultar una estructura sin elementos"])
for e in ("ListaEnlazada", "Cola", "Pila", "ColaPrioridad", "ABB", "Grafo", "Conjunto", "ListaCircular"):
    D += rel(e, "Iterable", "realiza")
D += rel("ListaEnlazada", "NodoLE", "compone", "", "", "0..*")
D += rel("Cola", "NodoC", "compone", "", "", "0..*")
D += rel("Pila", "NodoP", "compone", "", "", "0..*")
D += rel("ABB", "NodoABB", "compone", "", "", "0..*")
D += rel("ListaCircular", "NodoLC", "compone", "", "", "1..*")
D += rel("Grafo", "Vertice", "compone", "", "", "0..*")
D += rel("Vertice", "Arista", "compone", "", "", "0..*")
D += rel("Arista", "Vertice", "asocia", "destino")
D += rel("Conjunto", "ListaEnlazada", "compone")
D += rel("Grafo", "ListaEnlazada", "depende", "recorridos")
D += rel("Grafo", "Cola", "depende", "BFS")
D += rel("Grafo", "ColaPrioridad", "depende", "Dijkstra")
D += rel("Grafo", "Ruta", "depende")
D += rel("ABB", "ListaEnlazada", "depende", "recorridos")
for e in ("Cola", "Pila", "ColaPrioridad", "ListaEnlazada", "ListaCircular"):
    D += rel(e, "Vacia", "depende", extra="constraint=false, color=\"#B04040\"")
grafo("D_diagrama_estructuras_propias", "Diagrama de estructuras de datos propias (genéricas, sin java.util)", D,
      extra=', ranksep=0.9, nodesep=0.4')
