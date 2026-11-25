package pedidos.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PedidoPendiente {
    private final long idPedido;
    private final String mesa;
    private final long timestampEnvio;
    private final List<PlatilloDetalle> platillos = new ArrayList<>();
    private final Map<Long, IngredienteRequerido> ingredientes = new LinkedHashMap<>();

    public PedidoPendiente(long idPedido, String observaciones) {
        this.idPedido = idPedido;
        this.mesa = (observaciones != null && !observaciones.isBlank())
                ? observaciones.trim().replaceAll("\\s+", "_")
                : "mesa_" + idPedido;
        this.timestampEnvio = System.currentTimeMillis();
    }

    public long getIdPedido() {
        return idPedido;
    }

    public String getMesa() {
        return mesa;
    }

    public long getTimestampEnvio() {
        return timestampEnvio;
    }

    public List<PlatilloDetalle> getPlatillos() {
        return platillos;
    }

    public Map<Long, IngredienteRequerido> getIngredientes() {
        return ingredientes;
    }

    public void agregarPlatillo(PlatilloDetalle detalle) {
        platillos.add(detalle);
    }

    public void agregarIngrediente(long idIngrediente, String nombre, String unidadMedida, double cantidad) {
        ingredientes.compute(idIngrediente, (id, existente) -> {
            if (existente == null) {
                return new IngredienteRequerido(idIngrediente, nombre, unidadMedida, cantidad);
            }
            existente.sumarCantidad(cantidad);
            return existente;
        });
    }

    public double calcularTotal() {
        return platillos.stream().mapToDouble(PlatilloDetalle::getSubtotal).sum();
    }

    public String serializarSolicitudReserva() {
        StringBuilder sbPlatillos = new StringBuilder();
        for (PlatilloDetalle p : platillos) {
            String slug = normalizarTexto(p.getNombre());
            if (!sbPlatillos.isEmpty()) {
                sbPlatillos.append("_");
            }
            sbPlatillos.append(slug);
        }
        if (sbPlatillos.isEmpty()) {
            sbPlatillos.append("general");
        }

        return "(reservar pedido_" + idPedido + " " + sbPlatillos + ")";
    }

    public static String normalizarTexto(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase().trim().replaceAll("\\s+", "_");
    }

    public String resumenLegible() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pedido #").append(idPedido).append(" [").append(mesa).append("] -> Platillos: ")
                .append(platillos).append(" | Total: $").append(calcularTotal()).append(" | Receta: {");
        for (IngredienteRequerido ing : ingredientes.values()) {
            sb.append(ing.getNombre()).append("=").append(ing.getCantidadRequerida())
                    .append(" ").append(ing.getUnidadMedida()).append(", ");
        }
        if (!ingredientes.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }
}