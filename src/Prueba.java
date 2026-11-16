import co.edu.uq.sga.estructuras.*;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;
import java.util.Comparator;

public class Prueba {
    public static void main(String[] args) {
        ArbolBinarioBusqueda<Integer> abb = new ArbolBinarioBusqueda<>();
        for (int x : new int[]{5, 3, 8, 1, 4, 7, 9}) abb.insertar(x);
        System.out.println(abb.inOrden() + " altura=" + abb.altura() + " dup=" + abb.insertar(5));
        abb.eliminar(5);
        System.out.println(abb.preOrden() + " tam=" + abb.tamanio());

        ColaPrioridad<String> cp = new ColaPrioridad<>(Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()));
        cp.insertar("ccc"); cp.insertar("a"); cp.insertar("bb"); cp.insertar("aa");
        for (String s : cp) System.out.print(s + " ");
        System.out.println();

        Grafo<String> g = new Grafo<>();
        for (String s : new String[]{"REC", "ESC", "101", "102", "201", "ASEO"}) g.agregarVertice(s);
        g.agregarArista("REC", "ESC", 5); g.agregarArista("ESC", "101", 3); g.agregarArista("101", "102", 2);
        g.agregarArista("ESC", "201", 6); g.agregarArista("201", "ASEO", 1); g.agregarArista("102", "201", 10);
        System.out.println(g.dijkstra("REC", "102"));
        System.out.println(g.bfs("REC") + " " + g.dfs("REC"));
        System.out.println(g.inalcanzablesSiSeCierra("REC", "ESC", "201"));
        ListaEnlazada<String> d = new ListaEnlazada<>(); d.agregarFinal("102"); d.agregarFinal("ASEO");
        System.out.println(g.rutaPorDestinos("REC", d, false));
        System.out.println(g.componentesConexas());

        ListaCircular<String> t = new ListaCircular<>();
        t.agregar("Ana"); t.agregar("Luis"); t.agregar("Eva");
        System.out.println(t.actual() + " " + t.rotar() + " " + t.rotar() + " " + t.rotar());
        Pila<Integer> p = new Pila<>(); p.apilar(1); p.apilar(2); System.out.println(p.desapilar());
        Cola<Integer> c = new Cola<>();
        try { c.desencolar(); } catch (EstructuraVaciaException e) { System.out.println("OK: " + e.getMessage()); }
        Conjunto<String> cj = new Conjunto<>(); System.out.println(cj.agregar("a") + " " + cj.agregar("a"));
    }
}
