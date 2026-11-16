package co.edu.uq.sga.estructuras;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Grafo genérico, no dirigido y ponderado (lista de adyacencia propia).
 * Los vértices se identifican con equals().
 */
public class Grafo<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;


    public static class Arista<T> implements Serializable {
        private final Vertice<T> destino;
        private final double peso;

        private Arista(Vertice<T> destino, double peso) {
            this.destino = destino;
            this.peso = peso;
        }

        public Vertice<T> getDestino() {
            return destino;
        }

        public double getPeso() {
            return peso;
        }
    }

    public static class Vertice<T> implements Serializable {
        private final T dato;
        private int indice;
        private final ListaEnlazada<Arista<T>> adyacentes = new ListaEnlazada<>();

        private Vertice(T dato, int indice) {
            this.dato = dato;
            this.indice = indice;
        }

        public T getDato() {
            return dato;
        }

        public ListaEnlazada<Arista<T>> getAdyacentes() {
            return adyacentes;
        }
    }

    private record Entrada(int vertice, double distancia) {
    }

    private final ListaEnlazada<Vertice<T>> vertices = new ListaEnlazada<>();

    // ---------------------------------------------------------------- construcción

    public boolean agregarVertice(T dato) {
        if (buscar(dato) != null) return false;
        vertices.agregarFinal(new Vertice<>(dato, vertices.tamanio()));
        return true;
    }

    public void agregarArista(T a, T b, double peso) {
        Vertice<T> va = obligatorio(a);
        Vertice<T> vb = obligatorio(b);
        if (peso < 0) throw new IllegalArgumentException("El peso no puede ser negativo");
        if (buscarArista(va, vb) != null) return;
        va.adyacentes.agregarFinal(new Arista<>(vb, peso));
        vb.adyacentes.agregarFinal(new Arista<>(va, peso));
    }

    public boolean eliminarArista(T a, T b) {
        Vertice<T> va = buscar(a);
        Vertice<T> vb = buscar(b);
        if (va == null || vb == null) return false;
        Arista<T> ab = buscarArista(va, vb);
        Arista<T> ba = buscarArista(vb, va);
        if (ab == null) return false;
        va.adyacentes.eliminar(ab);
        vb.adyacentes.eliminar(ba);
        return true;
    }

    /** Elimina un nodo con todos sus pasillos. */
    public boolean eliminarVertice(T dato) {
        Vertice<T> v = buscar(dato);
        if (v == null) return false;
        ListaEnlazada<Vertice<T>> vecinos = new ListaEnlazada<>();
        for (Arista<T> a : v.adyacentes) vecinos.agregarFinal(a.destino);
        for (Vertice<T> w : vecinos) eliminarArista(v.dato, w.dato);
        vertices.eliminar(v);
        int i = 0;
        for (Vertice<T> x : vertices) x.indice = i++; // los índices de los algoritmos deben ser contiguos
        return true;
    }

    public boolean contiene(T dato) {
        return buscar(dato) != null;
    }

    public int cantidadVertices() {
        return vertices.tamanio();
    }

    public Vertice<T> getVertice(T dato) {
        return buscar(dato);
    }

    /** Recorre los datos de los vértices en orden de inserción. */
    @Override
    public Iterator<T> iterator() {
        Iterator<Vertice<T>> it = vertices.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return it.next().dato;
            }
        };
    }

    // ---------------------------------------------------------------- recorridos

    public ListaEnlazada<T> bfs(T inicio) {
        Vertice<T> v0 = obligatorio(inicio);
        ListaEnlazada<T> orden = new ListaEnlazada<>();
        boolean[] visitado = new boolean[vertices.tamanio()];
        Cola<Vertice<T>> cola = new Cola<>();
        visitado[v0.indice] = true;
        cola.encolar(v0);
        while (!cola.estaVacia()) {
            Vertice<T> v = cola.desencolar();
            orden.agregarFinal(v.dato);
            for (Arista<T> a : v.adyacentes) {
                if (!visitado[a.destino.indice]) {
                    visitado[a.destino.indice] = true;
                    cola.encolar(a.destino);
                }
            }
        }
        return orden;
    }

    /** DFS recursivo. */
    public ListaEnlazada<T> dfs(T inicio) {
        Vertice<T> v0 = obligatorio(inicio);
        ListaEnlazada<T> orden = new ListaEnlazada<>();
        dfs(v0, new boolean[vertices.tamanio()], orden);
        return orden;
    }

    private void dfs(Vertice<T> v, boolean[] visitado, ListaEnlazada<T> orden) {
        visitado[v.indice] = true;
        orden.agregarFinal(v.dato);
        for (Arista<T> a : v.adyacentes) {
            if (!visitado[a.destino.indice]) dfs(a.destino, visitado, orden);
        }
    }

    /** Componentes conexas (grupos de nodos conectados entre sí), usando DFS. */
    public ListaEnlazada<ListaEnlazada<T>> componentesConexas() {
        ListaEnlazada<ListaEnlazada<T>> grupos = new ListaEnlazada<>();
        boolean[] visitado = new boolean[vertices.tamanio()];
        for (Vertice<T> v : vertices) {
            if (!visitado[v.indice]) {
                ListaEnlazada<T> grupo = new ListaEnlazada<>();
                dfs(v, visitado, grupo);
                grupos.agregarFinal(grupo);
            }
        }
        return grupos;
    }

    /**
     * Nodos que dejarían de ser alcanzables desde {@code origen} si se cierra
     * el pasillo a–b. El grafo queda intacto al terminar.
     */
    public ListaEnlazada<T> inalcanzablesSiSeCierra(T origen, T a, T b) {
        Vertice<T> va = obligatorio(a);
        Vertice<T> vb = obligatorio(b);
        Arista<T> ab = buscarArista(va, vb);
        if (ab == null) throw new IllegalArgumentException("No existe ese pasillo");
        double peso = ab.peso;
        eliminarArista(a, b);
        ListaEnlazada<T> alcanzados = bfs(origen);
        agregarArista(a, b, peso);
        ListaEnlazada<T> perdidos = new ListaEnlazada<>();
        for (T t : this) if (!alcanzados.contiene(t)) perdidos.agregarFinal(t);
        return perdidos;
    }

    // ---------------------------------------------------------------- caminos mínimos

    /** Dijkstra origen→destino. Devuelve null si no hay camino. */
    public Ruta<T> dijkstra(T origen, T destino) {
        Vertice<T> vo = obligatorio(origen);
        Vertice<T> vd = obligatorio(destino);
        int n = vertices.tamanio();
        double[] dist = new double[n];
        int[] previo = new int[n];
        boolean[] cerrado = new boolean[n];
        Vertice<T>[] porIndice = arreglo();
        for (int i = 0; i < n; i++) {
            dist[i] = Double.POSITIVE_INFINITY;
            previo[i] = -1;
        }
        dist[vo.indice] = 0;
        ColaPrioridad<Entrada> cp = new ColaPrioridad<>(Comparator.comparingDouble(Entrada::distancia));
        cp.insertar(new Entrada(vo.indice, 0));
        while (!cp.estaVacia()) {
            Entrada e = cp.extraer();
            if (cerrado[e.vertice]) continue;
            cerrado[e.vertice] = true;
            if (e.vertice == vd.indice) break;
            for (Arista<T> a : porIndice[e.vertice].adyacentes) {
                int j = a.destino.indice;
                double nueva = dist[e.vertice] + a.peso;
                if (!cerrado[j] && nueva < dist[j]) {
                    dist[j] = nueva;
                    previo[j] = e.vertice;
                    cp.insertar(new Entrada(j, nueva));
                }
            }
        }
        if (dist[vd.indice] == Double.POSITIVE_INFINITY) return null;
        ListaEnlazada<T> camino = new ListaEnlazada<>();
        for (int i = vd.indice; i != -1; i = previo[i]) camino.agregarInicio(porIndice[i].dato);
        return new Ruta<>(camino, dist[vd.indice]);
    }

    /**
     * Recorrido para visitar varios destinos partiendo de {@code origen}:
     * en cada paso va al destino pendiente más cercano (vecino más cercano,
     * apoyado en Dijkstra). Si {@code respetarOrden} es true visita los
     * destinos en el orden dado (útil para respetar prioridades).
     */
    public Ruta<T> rutaPorDestinos(T origen, ListaEnlazada<T> destinos, boolean respetarOrden) {
        ListaEnlazada<T> pendientes = new ListaEnlazada<>();
        for (T d : destinos) pendientes.agregarFinal(d);
        ListaEnlazada<T> total = new ListaEnlazada<>();
        total.agregarFinal(origen);
        double distancia = 0;
        T actual = origen;
        while (!pendientes.estaVacia()) {
            Ruta<T> mejor = null;
            T elegido = null;
            for (T d : pendientes) {
                Ruta<T> r = dijkstra(actual, d);
                if (r != null && (mejor == null || r.distancia() < mejor.distancia())) {
                    mejor = r;
                    elegido = d;
                }
                if (respetarOrden && mejor != null) break;
            }
            if (mejor == null) break; // el resto es inalcanzable
            boolean primero = true;
            for (T t : mejor.camino()) {
                if (primero) {
                    primero = false;
                    continue;
                }
                total.agregarFinal(t);
            }
            distancia += mejor.distancia();
            pendientes.eliminar(elegido);
            actual = elegido;
        }
        return new Ruta<>(total, distancia);
    }

    // ---------------------------------------------------------------- auxiliares

    private Vertice<T> buscar(T dato) {
        for (Vertice<T> v : vertices) if (v.dato.equals(dato)) return v;
        return null;
    }

    private Vertice<T> obligatorio(T dato) {
        Vertice<T> v = buscar(dato);
        if (v == null) throw new IllegalArgumentException("El nodo no existe en el grafo: " + dato);
        return v;
    }

    private Arista<T> buscarArista(Vertice<T> a, Vertice<T> b) {
        for (Arista<T> ar : a.adyacentes) if (ar.destino == b) return ar;
        return null;
    }

    @SuppressWarnings("unchecked")
    private Vertice<T>[] arreglo() {
        Vertice<T>[] arr = (Vertice<T>[]) new Vertice[vertices.tamanio()];
        for (Vertice<T> v : vertices) arr[v.indice] = v;
        return arr;
    }
}
