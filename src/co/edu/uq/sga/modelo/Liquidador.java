package co.edu.uq.sga.modelo;

import co.edu.uq.sga.estructuras.ListaEnlazada;

import java.time.LocalDate;
import java.util.Iterator;

/** Liquida una estancia noche por noche (recursivo, apoyado en el Iterator de Estancia). */
public final class Liquidador {

    private Liquidador() {
    }

    public static ListaEnlazada<LineaLiquidacion> liquidar(Apartamento apto, Estancia estancia,
                                                           int pagantes, CalendarioTemporadas calendario) {
        ListaEnlazada<LineaLiquidacion> lineas = new ListaEnlazada<>();
        liquidar(estancia.iterator(), apto, pagantes, calendario, lineas);
        return lineas;
    }

    private static void liquidar(Iterator<LocalDate> noches, Apartamento apto, int pagantes,
                                 CalendarioTemporadas cal, ListaEnlazada<LineaLiquidacion> lineas) {
        if (!noches.hasNext()) return; // caso base
        LocalDate noche = noches.next();
        Temporada t = cal.temporadaDe(noche);
        double tarifa = apto.getTarifa(t);
        lineas.agregarFinal(new LineaLiquidacion(noche, t, tarifa, pagantes, tarifa * pagantes));
        liquidar(noches, apto, pagantes, cal, lineas);
    }

    public static double total(ListaEnlazada<LineaLiquidacion> lineas) {
        double t = 0;
        for (LineaLiquidacion l : lineas) t += l.valor();
        return t;
    }
}
