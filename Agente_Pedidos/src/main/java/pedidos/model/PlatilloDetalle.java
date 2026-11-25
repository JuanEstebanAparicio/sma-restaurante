package pedidos.model;

public class PlatilloDetalle {
    private final long idPlatillo;
    private final String nombre;
    private final int cantidad;
    private final double precioUnitario;

    public PlatilloDetalle(long idPlatillo, String nombre, int cantidad, double precioUnitario) {
        this.idPlatillo = idPlatillo;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public long getIdPlatillo() {
        return idPlatillo;
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