package co.edu.uq.sga.modelo;

public enum EstadoApartamento {
    PREPARADO("Preparado"),
    OCUPADO("Ocupado"),
    PENDIENTE_PREPARACION("Pendiente de preparación"),
    EN_PREPARACION("En preparación"),
    FUERA_DE_SERVICIO("Fuera de servicio");

    private final String etiqueta;

    EstadoApartamento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean puedePasarA(EstadoApartamento nuevo) {
        return switch (this) {
            case PREPARADO -> nuevo == OCUPADO || nuevo == FUERA_DE_SERVICIO;
            case OCUPADO -> nuevo == PENDIENTE_PREPARACION;
            case PENDIENTE_PREPARACION -> nuevo == EN_PREPARACION || nuevo == FUERA_DE_SERVICIO;
            case EN_PREPARACION -> nuevo == PREPARADO || nuevo == PENDIENTE_PREPARACION || nuevo == FUERA_DE_SERVICIO;
            case FUERA_DE_SERVICIO -> nuevo == PENDIENTE_PREPARACION;
        };
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
