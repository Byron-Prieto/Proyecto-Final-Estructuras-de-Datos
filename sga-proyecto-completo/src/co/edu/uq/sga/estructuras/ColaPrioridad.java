package co.edu.uq.sga.estructuras;

import java.io.Serializable;
import co.edu.uq.sga.excepciones.EstructuraVaciaException;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Cola de prioridad genérica (montículo binario). Sale primero el elemento
 * que el comparador considere "menor".
 */
public class ColaPrioridad<T> implements Iterable<T>, Serializable {
    private static final long serialVersionUID = 1L;


    private Object[] heap = new Object[16];
    private int tamanio;
    private final Comparator<? super T> comparador;

    public ColaPrioridad(Comparator<? super T> comparador) {
        if (comparador == null) throw new IllegalArgumentException("El comparador no puede ser nulo");
        this.comparador = comparador;
    }

    public void insertar(T dato) {
        if (tamanio == heap.length) {
            Object[] nuevo = new Object[heap.length * 2];
            for (int i = 0; i < tamanio; i++) nuevo[i] = heap[i];
            heap = nuevo;
        }
        heap[tamanio] = dato;
        subir(tamanio);
        tamanio++;
    }

    public T extraer() {
        if (tamanio == 0) throw new EstructuraVaciaException("La cola de prioridad está vacía");
        T min = get(0);
        tamanio--;
        heap[0] = heap[tamanio];
        heap[tamanio] = null;
        if (tamanio > 0) bajar(0);
        return min;
    }

    public T verMinimo() {
        if (tamanio == 0) throw new EstructuraVaciaException("La cola de prioridad está vacía");
        return get(0);
    }

    /** Elimina un elemento arbitrario (p. ej. una reserva cancelada). */
    public boolean eliminar(T dato) {
        for (int i = 0; i < tamanio; i++) {
            if (get(i).equals(dato)) {
                tamanio--;
                heap[i] = heap[tamanio];
                heap[tamanio] = null;
                if (i < tamanio) {
                    subir(i);
                    bajar(i);
                }
                return true;
            }
        }
        return false;
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    @SuppressWarnings("unchecked")
    private T get(int i) {
        return (T) heap[i];
    }

    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (comparador.compare(get(i), get(padre)) >= 0) break;
            intercambiar(i, padre);
            i = padre;
        }
    }

    private void bajar(int i) {
        while (true) {
            int izq = 2 * i + 1;
            int der = izq + 1;
            int menor = i;
            if (izq < tamanio && comparador.compare(get(izq), get(menor)) < 0) menor = izq;
            if (der < tamanio && comparador.compare(get(der), get(menor)) < 0) menor = der;
            if (menor == i) return;
            intercambiar(i, menor);
            i = menor;
        }
    }

    private void intercambiar(int a, int b) {
        Object t = heap[a];
        heap[a] = heap[b];
        heap[b] = t;
    }

    /** Recorre en orden de prioridad, sin modificar la cola original. */
    @Override
    public Iterator<T> iterator() {
        ColaPrioridad<T> copia = new ColaPrioridad<>(comparador);
        copia.heap = new Object[heap.length];
        for (int i = 0; i < tamanio; i++) copia.heap[i] = heap[i];
        copia.tamanio = tamanio;
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !copia.estaVacia();
            }

            @Override
            public T next() {
                if (copia.estaVacia()) throw new NoSuchElementException();
                return copia.extraer();
            }
        };
    }
}
