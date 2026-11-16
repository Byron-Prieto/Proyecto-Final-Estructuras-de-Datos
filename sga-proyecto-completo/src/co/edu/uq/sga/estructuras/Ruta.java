package co.edu.uq.sga.estructuras;

/** Resultado de un camino en el grafo: nodos en orden y distancia total. */
public record Ruta<T>(ListaEnlazada<T> camino, double distancia) {
}
