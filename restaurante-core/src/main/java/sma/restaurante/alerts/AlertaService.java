package sma.restaurante.alerts;

import sma.restaurante.domain.Alerta;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.ReglasStock;
import sma.restaurante.domain.enums.TipoAlerta;
import sma.restaurante.repository.AlertaRepository;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.LoteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AlertaService {

    static final int DIAS_AVISO_VENCIMIENTO = 5;

    /** Solo se cierran automaticamente las alertas que este servicio sabe calcular. */
    private static final Set<String> TIPOS_GESTIONADOS = Set.of(
            TipoAlerta.AGOTADO.name(),
            TipoAlerta.BAJO_STOCK.name(),
            TipoAlerta.POR_VENCER.name(),
            TipoAlerta.VENCIDO.name());

    private final IngredienteRepository ingredienteRepository;
    private final LoteRepository loteRepository;
    private final AlertaRepository alertaRepository;

    public AlertaService(
            IngredienteRepository ingredienteRepository,
            LoteRepository loteRepository,
            AlertaRepository alertaRepository) {

        this.ingredienteRepository = ingredienteRepository;
        this.loteRepository = loteRepository;
        this.alertaRepository = alertaRepository;
    }

    /** Alertas que deberian estar activas segun el inventario actual. No escribe nada. */
    public List<Alerta> evaluar(LocalDate hoy) {

        Map<Long, List<Lote>> lotesPorIngrediente = loteRepository.listarTodos().stream()
                .collect(Collectors.groupingBy(Lote::getIdIngrediente));

        List<Alerta> alertas = new ArrayList<>();

        for (Ingrediente ingrediente : ingredienteRepository.listarTodos()) {

            if ("INACTIVO".equals(ingrediente.getEstado())) {
                continue;
            }

            List<Lote> lotes = lotesPorIngrediente.getOrDefault(
                    ingrediente.getIdIngrediente(), List.of());

            BigDecimal vigente = ReglasStock.stockVigente(lotes, hoy);
            BigDecimal vencido = ReglasStock.stockVencido(lotes, hoy);
            BigDecimal minimo = ingrediente.getStockMinimo() == null
                    ? BigDecimal.ZERO
                    : ingrediente.getStockMinimo();
            String nombre = ingrediente.getNombre();
            String unidad = ingrediente.getUnidadMedida();

            if (vigente.signum() <= 0) {
                alertas.add(nueva(ingrediente, TipoAlerta.AGOTADO,
                        "Ingrediente agotado: " + nombre));
            } else if (vigente.compareTo(minimo) <= 0) {
                alertas.add(nueva(ingrediente, TipoAlerta.BAJO_STOCK,
                        "Stock bajo de " + nombre + ". Disponible: " + vigente + " " + unidad
                                + ". Stock minimo: " + minimo));
            }

            if (vencido.signum() > 0) {
                alertas.add(nueva(ingrediente, TipoAlerta.VENCIDO,
                        "Hay " + vencido + " " + unidad + " vencidos de " + nombre
                                + ". Registre la merma."));
            }

            ReglasStock.proximoVencimiento(lotes, hoy, DIAS_AVISO_VENCIMIENTO)
                    .ifPresent(fecha -> alertas.add(nueva(ingrediente, TipoAlerta.POR_VENCER,
                            nombre + " tiene existencias que vencen el " + fecha)));
        }

        return alertas;
    }

    /** Guarda las alertas nuevas y cierra las que ya no aplican (por ejemplo, tras reponer). */
    public ResultadoSincronizacion sincronizar(LocalDate hoy) {

        List<Alerta> deseadas = evaluar(hoy);

        List<Alerta> nuevas = new ArrayList<>();
        for (Alerta alerta : deseadas) {
            if (alertaRepository.guardarSiNoExiste(alerta)) {
                nuevas.add(alerta);
            }
        }

        Set<String> vigentes = deseadas.stream()
                .map(AlertaService::clave)
                .collect(Collectors.toSet());

        int resueltas = 0;
        for (Alerta activa : alertaRepository.listarActivas()) {
            boolean gestionada = TIPOS_GESTIONADOS.contains(activa.getTipoAlerta());
            if (gestionada && !vigentes.contains(clave(activa))) {
                alertaRepository.resolver(activa.getIdAlerta());
                resueltas++;
            }
        }

        return new ResultadoSincronizacion(nuevas, resueltas);
    }

    public record ResultadoSincronizacion(List<Alerta> nuevas, int resueltas) {
    }

    private static String clave(Alerta alerta) {
        return alerta.getIdIngrediente() + "|" + alerta.getTipoAlerta();
    }

    private static Alerta nueva(Ingrediente ingrediente, TipoAlerta tipo, String mensaje) {
        return Alerta.builder()
                .idIngrediente(ingrediente.getIdIngrediente())
                .tipoAlerta(tipo.name())
                .mensaje(mensaje)
                .activa(true)
                .build();
    }
}