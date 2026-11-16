package co.edu.uq.sga.estructuras;

import java.io.Serializable;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Lista circular genérica; útil para la rotación de turnos. */
public class ListaCircular<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;


    private static class Nodo<T> implements Serializable {
        T dato;
        Nodo<T> sig;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> ultimo;   // ultimo.sig es el primero
    private Nodo<T> actual;   // elemento en turno
    private int tamanio;

    public void agregar(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        if (ultimo == null) {
            n.sig = n;
            ultimo = n;
            actual = n;
        } else {
            n.sig = ultimo.sig;
            ultimo.sig = n;
            ultimo = n;
        }
        tamanio++;
    }

    /** Elemento que tiene el turno actualmente. */
    public T actual() {
        if (actual == null) throw new EstructuraVaciaException("La lista circular está vacía");
        return actual.dato;
    }

    /** Pasa el turno al siguiente y lo devuelve. */
    public T rotar() {
        if (actual == null) throw new EstructuraVaciaException("La lista circular está vacía");
        actual = actual.sig;
        return actual.dato;
    }

    public boolean eliminar(T dato) {
        if (ultimo == null) return false;
        Nodo<T> prev = ultimo;
        Nodo<T> cur = ultimo.sig;
        for (int i = 0; i < tamanio; i++) {
            if (cur.dato.equals(dato)) {
                if (tamanio == 1) {
                    ultimo = null;
                    actual = null;
                } else {
                    prev.sig = cur.sig;
                    if (cur == ultimo) ultimo = prev;
                    if (cur == actual) actual = cur.sig;
                }
                tamanio--;
                return true;
            }
            prev = cur;
            cur = cur.sig;
        }
        return false;
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    /** Una vuelta completa empezando por quien tiene el turno. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> cur = actual;
            private int restantes = tamanio;

            @Override
            public boolean hasNext() {
                return restantes > 0;
            }

            @Override
            public T next() {
                if (restantes == 0) throw new NoSuchElementException();
                T d = cur.dato;
                cur = cur.sig;
                restantes--;
                return d;
            }
        };
    }
}
