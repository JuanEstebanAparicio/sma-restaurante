package pedidos.model;

public class PlatilloDetalle {
    private final int platilloId;
    private final String nombre;
    private final int cantidad;
    private final double precioUnitario;

    public PlatilloDetalle(int platilloId, String nombre, int cantidad, double precioUnitario) {
        this.platilloId = platilloId;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public int getPlatilloId() {
        return platilloId;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    @Override
    public String toString() {
        return cantidad + "x " + nombre;
    }
}