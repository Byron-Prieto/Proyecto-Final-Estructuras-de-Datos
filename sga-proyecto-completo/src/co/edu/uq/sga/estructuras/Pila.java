package co.edu.uq.sga.estructuras;

import java.io.Serializable;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Pila LIFO genérica. */
public class Pila<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;


    private static class Nodo<T> implements Serializable {
        T dato;
        Nodo<T> sig;

        Nodo(T dato, Nodo<T> sig) {
            this.dato = dato;
            this.sig = sig;
        }
    }

    private Nodo<T> cima;
    private int tamanio;

    public void apilar(T dato) {
        cima = new Nodo<>(dato, cima);
        tamanio++;
    }

    public T desapilar() {
        if (cima == null) throw new EstructuraVaciaException("La pila está vacía");
        T d = cima.dato;
        cima = cima.sig;
        tamanio--;
        return d;
    }

    public T cima() {
        if (cima == null) throw new EstructuraVaciaException("La pila está vacía");
        return cima.dato;
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    /** Recorre de la cima hacia el fondo. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cima;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) throw new NoSuchElementException();
                T d = actual.dato;
                actual = actual.sig;
                return d;
            }
        };
    }
}
