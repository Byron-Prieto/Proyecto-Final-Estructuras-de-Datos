package co.edu.uq.sga.estructuras;

import java.io.Serializable;
import java.util.Iterator;

/** Conjunto genérico (sin repetidos) basado en la lista enlazada propia. */
public class Conjunto<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;


    private final ListaEnlazada<T> elementos = new ListaEnlazada<>();

    /** @return true si se agregó; false si ya estaba. */
    public boolean agregar(T dato) {
        if (elementos.contiene(dato)) return false;
        elementos.agregarFinal(dato);
        return true;
    }

    public boolean eliminar(T dato) {
        return elementos.eliminar(dato);
    }

    public boolean contiene(T dato) {
        return elementos.contiene(dato);
    }

    public int tamanio() {
        return elementos.tamanio();
    }

    public boolean estaVacio() {
        return elementos.estaVacia();
    }

    public Conjunto<T> union(Conjunto<T> otro) {
        Conjunto<T> r = new Conjunto<>();
        for (T t : this) r.agregar(t);
        for (T t : otro) r.agregar(t);
        return r;
    }

    public Conjunto<T> interseccion(Conjunto<T> otro) {
        Conjunto<T> r = new Conjunto<>();
        for (T t : this) if (otro.contiene(t)) r.agregar(t);
        return r;
    }

    @Override
    public Iterator<T> iterator() {
        return elementos.iterator();
    }

    @Override
    public String toString() {
        return elementos.toString();
    }
}
