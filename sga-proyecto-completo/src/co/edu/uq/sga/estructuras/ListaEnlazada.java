package co.edu.uq.sga.estructuras;

import java.io.Serializable;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Lista doblemente enlazada genérica. */
public class ListaEnlazada<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;


    private static class Nodo<T> implements Serializable {
        T dato;
        Nodo<T> sig;
        Nodo<T> ant;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamanio;

    public void agregarInicio(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        if (cabeza == null) {
            cabeza = cola = n;
        } else {
            n.sig = cabeza;
            cabeza.ant = n;
            cabeza = n;
        }
        tamanio++;
    }

    public void agregarFinal(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        if (cola == null) {
            cabeza = cola = n;
        } else {
            n.ant = cola;
            cola.sig = n;
            cola = n;
        }
        tamanio++;
    }

    public void insertar(int indice, T dato) {
        if (indice < 0 || indice > tamanio) throw new IndexOutOfBoundsException("Índice: " + indice);
        if (indice == 0) {
            agregarInicio(dato);
        } else if (indice == tamanio) {
            agregarFinal(dato);
        } else {
            Nodo<T> actual = nodoEn(indice);
            Nodo<T> n = new Nodo<>(dato);
            n.ant = actual.ant;
            n.sig = actual;
            actual.ant.sig = n;
            actual.ant = n;
            tamanio++;
        }
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tamanio) throw new IndexOutOfBoundsException("Índice: " + indice);
        return nodoEn(indice).dato;
    }

    public T primero() {
        if (cabeza == null) throw new EstructuraVaciaException("La lista está vacía");
        return cabeza.dato;
    }

    public T ultimo() {
        if (cola == null) throw new EstructuraVaciaException("La lista está vacía");
        return cola.dato;
    }

    public T eliminarPrimero() {
        if (cabeza == null) throw new EstructuraVaciaException("La lista está vacía");
        return desenlazar(cabeza);
    }

    public T eliminarUltimo() {
        if (cola == null) throw new EstructuraVaciaException("La lista está vacía");
        return desenlazar(cola);
    }

    public T eliminarEn(int indice) {
        if (indice < 0 || indice >= tamanio) throw new IndexOutOfBoundsException("Índice: " + indice);
        return desenlazar(nodoEn(indice));
    }

    /** Elimina la primera aparición (según equals). */
    public boolean eliminar(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.sig) {
            if (igual(n.dato, dato)) {
                desenlazar(n);
                return true;
            }
        }
        return false;
    }

    public boolean contiene(T dato) {
        return indiceDe(dato) >= 0;
    }

    public int indiceDe(T dato) {
        int i = 0;
        for (Nodo<T> n = cabeza; n != null; n = n.sig, i++) {
            if (igual(n.dato, dato)) return i;
        }
        return -1;
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    public void limpiar() {
        cabeza = cola = null;
        tamanio = 0;
    }

    private boolean igual(T a, T b) {
        return a == null ? b == null : a.equals(b);
    }

    private Nodo<T> nodoEn(int indice) {
        Nodo<T> n;
        if (indice < tamanio / 2) {
            n = cabeza;
            for (int i = 0; i < indice; i++) n = n.sig;
        } else {
            n = cola;
            for (int i = tamanio - 1; i > indice; i--) n = n.ant;
        }
        return n;
    }

    private T desenlazar(Nodo<T> n) {
        if (n.ant == null) cabeza = n.sig; else n.ant.sig = n.sig;
        if (n.sig == null) cola = n.ant; else n.sig.ant = n.ant;
        tamanio--;
        return n.dato;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = cabeza;

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

    /** Recorrido del último al primero. */
    public Iterator<T> iteradorInverso() {
        return new Iterator<>() {
            private Nodo<T> actual = cola;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) throw new NoSuchElementException();
                T d = actual.dato;
                actual = actual.ant;
                return d;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Nodo<T> n = cabeza; n != null; n = n.sig) {
            sb.append(n.dato);
            if (n.sig != null) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}
