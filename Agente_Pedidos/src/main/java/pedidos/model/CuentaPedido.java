package pedidos.model;

import java.util.ArrayList;
import java.util.List;

public class CuentaPedido {
    private final long idCuenta;
    private final long idPedido;
    private final String mesa;
    private final double subtotal;
    private final double impuesto;
    private final double total;
    private final String estado;
    private final List<ItemConsumo> items = new ArrayList<>();

    public CuentaPedido(long idCuenta, long idPedido, String mesa,
                        double subtotal, double impuesto, double total, String estado) {
        this.idCuenta = idCuenta;
        this.idPedido = idPedido;
        this.mesa = mesa;
        this.subtotal = subtotal;
        this.impuesto = impuesto;
        this.total = total;
        this.estado = estado;
    }

    public long getIdCuenta() {
        return idCuenta;
    }

    public long getIdPedido() {
        return idPedido;
    }

    public String getMesa() {
        return mesa;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getImpuesto() {
        return impuesto;
    }

    public double getTotal() {
        return total;
    }

    public String getEstado() {
        return estado;
    }

    public List<ItemConsumo> getItems() {
        return items;
    }

    public void agregarItem(ItemConsumo item) {
        items.add(item);
    }

    public String resumenFactura() {
        return "Cuenta #" + idCuenta + " | Pedido #" + idPedido + " [" + mesa + "] | Items: "
                + items + " | Total: $" + total + " | Estado: " + estado;
    }
}