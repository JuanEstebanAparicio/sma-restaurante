package pedidos.model;

public class IngredienteRequerido {
    private final long idIngrediente;
    private final String nombre;
    private final String unidadMedida;
    private double cantidadRequerida;

    public IngredienteRequerido(long idIngrediente, String nombre, String unidadMedida, double cantidadRequerida) {
        this.idIngrediente = idIngrediente;
        this.nombre = nombre;
        this.unidadMedida = unidadMedida;
        this.cantidadRequerida = cantidadRequerida;
    }

    public long getIdIngrediente() {
        return idIngrediente;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public double getCantidadRequerida() {
        return cantidadRequerida;
    }

    public void sumarCantidad(double extra) {
        this.cantidadRequerida += extra;
    }
}