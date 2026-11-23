package pedidos.model;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PedidoPendiente {
    private final int id;
    private final String mesa;
    private final long timestampEnvio;
    private final List<PlatilloDetalle> platillos = new ArrayList<>();
    private final Map<Integer, Double> ingredientesTotales = new LinkedHashMap<>();
    private final Map<Integer, String> nombresIngredientes = new LinkedHashMap<>();

    public PedidoPendiente(int id, String mesa) {
        this.id = id;
        this.mesa = mesa;
        this.timestampEnvio = System.currentTimeMillis();
    }

    public int getId() {
        return id;
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

    public Map<Integer, Double> getIngredientesTotales() {
        return ingredientesTotales;
    }

    public void agregarPlatillo(PlatilloDetalle detalle) {
        platillos.add(detalle);
    }

    public void agregarRequerimientoIngrediente(int ingredienteId, String nombre, double cantidad) {
        ingredientesTotales.merge(ingredienteId, cantidad, Double::sum);
        nombresIngredientes.putIfAbsent(ingredienteId, nombre);
    }

    public double calcularTotalCuenta() {
        return platillos.stream().mapToDouble(PlatilloDetalle::getSubtotal).sum();
    }


    public String serializarSolicitudReserva() {
        StringBuilder listaPlatillos = new StringBuilder();
        for (PlatilloDetalle p : platillos) {
            String slug = normalizarTexto(p.getNombre());
            for (int i = 0; i < p.getCantidad(); i++) {
                listaPlatillos.append(slug).append(",");
            }
        }
        if (!listaPlatillos.isEmpty()) {
            listaPlatillos.deleteCharAt(listaPlatillos.length() - 1);
        }
        return "(reservar pedido_" + id + " " + listaPlatillos + ")";
    }

    public static String normalizarTexto(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase().trim().replaceAll("\\s+", "_");
    }

    public String resumenLegible() {
        return "Pedido #" + id + " [" + mesa + "] -> Platillos: " + platillos;
    }
}