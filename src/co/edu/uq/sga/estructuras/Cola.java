package co.edu.uq.sga.estructuras;

import java.io.Serializable;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Cola FIFO genérica. */
public class Cola<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;


    private static class Nodo<T> implements Serializable {
        T dato;
        Nodo<T> sig;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamanio;

    public void encolar(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        if (fin == null) frente = n; else fin.sig = n;
        fin = n;
        tamanio++;
    }

    public T desencolar() {
        if (frente == null) throw new EstructuraVaciaException("La cola está vacía");
        T d = frente.dato;
        frente = frente.sig;
        if (frente == null) fin = null;
        tamanio--;
        return d;
    }

    public T frente() {
        if (frente == null) throw new EstructuraVaciaException("La cola está vacía");
        return frente.dato;
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = frente;

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
