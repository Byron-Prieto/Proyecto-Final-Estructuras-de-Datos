package co.edu.uq.sga.modelo;

import java.time.LocalDate;

public record LineaLiquidacion(LocalDate noche, Temporada temporada, double tarifa, int pagantes, double valor) {
}
