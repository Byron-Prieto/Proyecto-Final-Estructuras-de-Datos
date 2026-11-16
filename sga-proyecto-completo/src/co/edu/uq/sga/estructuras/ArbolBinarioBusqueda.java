package co.edu.uq.sga.estructuras;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Árbol binario de búsqueda genérico. Todas las operaciones son recursivas.
 * No admite elementos duplicados (compareTo == 0).
 */
public class ArbolBinarioBusqueda<T extends Comparable<T>> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;


    /** Nodo público de solo lectura para que el dominio pueda hacer recorridos propios. */
    public static class Nodo<T> implements Serializable {
        private T dato;
        private Nodo<T> izq;
        private Nodo<T> der;

        private Nodo(T dato) {
            this.dato = dato;
        }

        public T getDato() {
            return dato;
        }

        public Nodo<T> getIzq() {
            return izq;
        }

        public Nodo<T> getDer() {
            return der;
        }
    }

    private Nodo<T> raiz;
    private int tamanio;

    public Nodo<T> getRaiz() {
        return raiz;
    }

    /** @return false si ya existía un elemento equivalente. */
    public boolean insertar(T dato) {
        if (dato == null) throw new IllegalArgumentException("Dato nulo");
        if (buscar(dato) != null) return false;
        raiz = insertar(raiz, dato);
        tamanio++;
        return true;
    }

    private Nodo<T> insertar(Nodo<T> n, T dato) {
        if (n == null) return new Nodo<>(dato);
        if (dato.compareTo(n.dato) < 0) n.izq = insertar(n.izq, dato);
        else n.der = insertar(n.der, dato);
        return n;
    }

    /** Devuelve el elemento equivalente a la clave o null si no existe. */
    public T buscar(T clave) {
        return buscar(raiz, clave);
    }

    private T buscar(Nodo<T> n, T clave) {
        if (n == null) return null;
        int c = clave.compareTo(n.dato);
        if (c == 0) return n.dato;
        return c < 0 ? buscar(n.izq, clave) : buscar(n.der, clave);
    }

    public boolean contiene(T clave) {
        return buscar(clave) != null;
    }

    public boolean eliminar(T clave) {
        if (buscar(clave) == null) return false;
        raiz = eliminar(raiz, clave);
        tamanio--;
        return true;
    }

    private Nodo<T> eliminar(Nodo<T> n, T clave) {
        if (n == null) return null;
        int c = clave.compareTo(n.dato);
        if (c < 0) {
            n.izq = eliminar(n.izq, clave);
        } else if (c > 0) {
            n.der = eliminar(n.der, clave);
        } else {
            if (n.izq == null) return n.der;
            if (n.der == null) return n.izq;
            Nodo<T> sucesor = minimo(n.der);
            n.dato = sucesor.dato;
            n.der = eliminar(n.der, sucesor.dato);
        }
        return n;
    }

    private Nodo<T> minimo(Nodo<T> n) {
        return n.izq == null ? n : minimo(n.izq);
    }

    public int altura() {
        return altura(raiz);
    }

    private int altura(Nodo<T> n) {
        if (n == null) return 0;
        return 1 + Math.max(altura(n.izq), altura(n.der));
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacio() {
        return tamanio == 0;
    }

    public ListaEnlazada<T> inOrden() {
        ListaEnlazada<T> r = new ListaEnlazada<>();
        inOrden(raiz, r);
        return r;
    }

    private void inOrden(Nodo<T> n, ListaEnlazada<T> r) {
        if (n == null) return;
        inOrden(n.izq, r);
        r.agregarFinal(n.dato);
        inOrden(n.der, r);
    }

    public ListaEnlazada<T> preOrden() {
        ListaEnlazada<T> r = new ListaEnlazada<>();
        preOrden(raiz, r);
        return r;
    }

    private void preOrden(Nodo<T> n, ListaEnlazada<T> r) {
        if (n == null) return;
        r.agregarFinal(n.dato);
        preOrden(n.izq, r);
        preOrden(n.der, r);
    }

    public ListaEnlazada<T> postOrden() {
        ListaEnlazada<T> r = new ListaEnlazada<>();
        postOrden(raiz, r);
        return r;
    }

    private void postOrden(Nodo<T> n, ListaEnlazada<T> r) {
        if (n == null) return;
        postOrden(n.izq, r);
        postOrden(n.der, r);
        r.agregarFinal(n.dato);
    }

    /** Recorrido in-orden (ascendente por orden natural). */
    @Override
    public Iterator<T> iterator() {
        return inOrden().iterator();
    }
}
