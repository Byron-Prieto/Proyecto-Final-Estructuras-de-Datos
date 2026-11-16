import co.edu.uq.sga.estructuras.*;
import co.edu.uq.sga.excepciones.*;
import co.edu.uq.sga.modelo.*;

import java.time.*;

public class PruebaDominio {
    public static void main(String[] args) {
        Apartamento a1 = new Apartamento("A101", "Café", "A", 1, 2, 4);
        Apartamento a2 = new Apartamento("B201", "Cumbre", "B", 2, 3, 6);
        for (Temporada t : Temporada.values()) { a1.setTarifa(t, 50000 + 10000 * t.ordinal()); a2.setTarifa(t, 40000 + 20000 * t.ordinal()); }

        CalendarioTemporadas cal = new CalendarioTemporadas();
        cal.definir(LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 10), Temporada.ALTA);
        cal.definir(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 19), Temporada.MEDIA);

        Huesped h = new Huesped("123", "Laura", LocalDate.of(1995, 5, 5), "300", "l@x.co");
        ListaEnlazada<Ocupante> oc = new ListaEnlazada<>();
        oc.agregarFinal(new Ocupante("Laura", LocalDate.of(1995, 5, 5)));
        oc.agregarFinal(new Ocupante("Tomás", LocalDate.of(2022, 1, 1)));   // menor de 6
        oc.agregarFinal(new Ocupante("Ana", LocalDate.of(2000, 1, 1)));

        Estancia e = new Estancia(LocalDate.of(2026, 12, 18), LocalDate.of(2026, 12, 22));
        System.out.println(e + " pagantes=" + new Reserva("R1", a1, h, oc, e, Canal.PORTAL, LocalDateTime.now()).pagantes());
        ListaEnlazada<LineaLiquidacion> lin = Liquidador.liquidar(a1, e, 2, cal);
        for (LineaLiquidacion l : lin) System.out.println("  " + l);
        System.out.println("total=" + Liquidador.total(lin));

        Reserva r1 = new Reserva("R1", a1, h, oc, e, Canal.PORTAL, LocalDateTime.now());
        a1.agregarReserva(r1);
        a1.agregarReserva(new Reserva("R2", a1, h, oc, new Estancia(LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 27)), Canal.DIRECTA, LocalDateTime.now()));
        a1.agregarReserva(new Reserva("R3", a1, h, oc, new Estancia(LocalDate.of(2026, 12, 22), LocalDate.of(2026, 12, 24)), Canal.DIRECTA, LocalDateTime.now())); // sale=entrada: válido
        try {
            a1.agregarReserva(new Reserva("R4", a1, h, oc, new Estancia(LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 23)), Canal.EXTERNA, LocalDateTime.now()));
        } catch (ReservaSolapadaException ex) { System.out.println("OK solape: " + ex.getMessage()); }
        System.out.println("libre 24-25? " + a1.estaLibre(new Estancia(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 25))));

        try { new Estancia(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 10)); } catch (FechasInvalidasException ex) { System.out.println("OK fechas: " + ex.getMessage()); }
        try {
            ListaEnlazada<Ocupante> muchos = new ListaEnlazada<>();
            for (int i = 0; i < 5; i++) muchos.agregarFinal(new Ocupante("P" + i, LocalDate.of(1990, 1, 1)));
            new Reserva("R5", a1, h, muchos, e, Canal.PORTAL, LocalDateTime.now());
        } catch (CapacidadExcedidaException ex) { System.out.println("OK capacidad: " + ex.getMessage()); }

        try { a1.cambiarEstado(EstadoApartamento.EN_PREPARACION); } catch (TransicionEstadoInvalidaException ex) { System.out.println("OK transición: " + ex.getMessage()); }
        try { r1.cambiarEstado(EstadoReserva.FINALIZADA); } catch (TransicionEstadoInvalidaException ex) { System.out.println("OK transición: " + ex.getMessage()); }
        a2.ponerFueraDeServicio("Fuga de agua"); System.out.println(a2.getEstado() + ": " + a2.getMotivoFueraDeServicio());

        Folio f = r1.getFolio();
        f.registrarCargo(200000, "Estancia"); f.registrarPago(60000, "Anticipo 30%"); f.registrarCargo(15000, "Minibar");
        System.out.println("saldo=" + f.saldo()); f.deshacer(); System.out.println("saldo tras deshacer=" + f.saldo() + " movs=" + f.getMovimientos().tamanio());
        System.out.println("reembolso (hoy 2026-12-01): " + r1.calcularReembolso(LocalDate.of(2026, 12, 1)));

        ColaPrioridad<TareaPreparacion> cp = new ColaPrioridad<>(Comparadores.prioridadPreparacion());
        LocalDateTime t0 = LocalDateTime.now();
        cp.insertar(new TareaPreparacion(a2, false, null, t0));
        cp.insertar(new TareaPreparacion(new Apartamento("C1", "x", "C", 1, 1, 2), true, LocalTime.of(15, 0), t0.plusMinutes(1)));
        cp.insertar(new TareaPreparacion(a1, true, LocalTime.of(13, 0), t0.plusMinutes(2)));
        for (TareaPreparacion t : cp) System.out.println("prep: " + t);

        ArbolBinarioBusqueda<Apartamento> cat = new ArbolBinarioBusqueda<>();
        cat.insertar(a2); cat.insertar(a1);
        System.out.println(cat.buscar(new Apartamento("A101", "?", "?", 0, 1, 1)));
        ColaPrioridad<Apartamento> porPrecio = new ColaPrioridad<>(Comparadores.apartamentosPorPrecio(Temporada.ALTA));
        for (Apartamento a : cat) porPrecio.insertar(a);
        System.out.println("más barato ALTA: " + porPrecio.extraer());
    }
}
