package pedidos.model;

public class ItemConsumo {
    private final String nombrePlatillo;
    private final int cantidad;
    private final double precioUnitario;

    public ItemConsumo(String nombrePlatillo, int cantidad, double precioUnitario) {
        this.nombrePlatillo = nombrePlatillo;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public String getNombrePlatillo() {
        return nombrePlatillo;
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
        return cantidad + "x " + nombrePlatillo + " ($" + getSubtotal() + ")";
    }
}