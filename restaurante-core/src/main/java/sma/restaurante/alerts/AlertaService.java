package sma.restaurante.alerts;

import sma.restaurante.domain.Alerta;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.ReglasStock;
import sma.restaurante.domain.enums.TipoAlerta;
import sma.restaurante.prediction.PrediccionService;
import sma.restaurante.prediction.Pronostico;
import sma.restaurante.repository.AlertaRepository;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.LoteRepository;
import java.util.EnumSet;
import sma.restaurante.domain.enums.EstadoIngrediente;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AlertaService {

    static final int DIAS_AVISO_VENCIMIENTO = 5;

    /** Solo se cierran automaticamente las alertas que este servicio sabe calcular. */
    private static final Set<TipoAlerta> TIPOS_GESTIONADOS = EnumSet.of(
            TipoAlerta.AGOTADO,
            TipoAlerta.BAJO_STOCK,
            TipoAlerta.POR_VENCER,
            TipoAlerta.VENCIDO,
            TipoAlerta.AGOTAMIENTO_PREDICTIVO);

    private final IngredienteRepository ingredienteRepository;
    private final LoteRepository loteRepository;
    private final AlertaRepository alertaRepository;
    private final PrediccionService prediccionService;
    private final Clock reloj;

    public AlertaService(
            IngredienteRepository ingredienteRepository,
            LoteRepository loteRepository,
            AlertaRepository alertaRepository,
            PrediccionService prediccionService,
            Clock reloj) {

        this.ingredienteRepository = ingredienteRepository;
        this.loteRepository = loteRepository;
        this.alertaRepository = alertaRepository;
        this.prediccionService = prediccionService;
        this.reloj = reloj;
    }

    /** Alertas que deberian estar activas segun el inventario actual. No escribe nada. */
    public List<Alerta> evaluar() {

        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();

        Map<Long, List<Lote>> lotesPorIngrediente = loteRepository.listarTodos().stream()
                .collect(Collectors.groupingBy(Lote::getIdIngrediente));

        List<Ingrediente> activos = ingredienteRepository.listarTodos().stream()
                .filter(i -> i.getEstado() != EstadoIngrediente.INACTIVO)
                .toList();

        List<Alerta> alertas = new ArrayList<>();

        for (Ingrediente ingrediente : activos) {

            Long id = ingrediente.getIdIngrediente();
            List<Lote> lotes = lotesPorIngrediente.getOrDefault(id, List.of());

            BigDecimal vigente = ReglasStock.stockVigente(lotes, hoy);
            BigDecimal vencido = ReglasStock.stockVencido(lotes, hoy);
            BigDecimal minimo = ingrediente.getStockMinimo() == null
                    ? BigDecimal.ZERO
                    : ingrediente.getStockMinimo();
            String nombre = ingrediente.getNombre();
            String unidad = ingrediente.getUnidadMedida();

            if (vigente.signum() <= 0) {
                alertas.add(nueva(id, TipoAlerta.AGOTADO,
                        "Ingrediente agotado: " + nombre));
            } else if (vigente.compareTo(minimo) <= 0) {
                alertas.add(nueva(id, TipoAlerta.BAJO_STOCK,
                        "Stock bajo de " + nombre + ". Disponible: " + vigente + " " + unidad
                                + ". Stock minimo: " + minimo));
            }

            if (vencido.signum() > 0) {
                alertas.add(nueva(id, TipoAlerta.VENCIDO,
                        "Hay " + vencido + " " + unidad + " vencidos de " + nombre
                                + ". Registre la merma."));
            }

            ReglasStock.proximoVencimiento(lotes, hoy, DIAS_AVISO_VENCIMIENTO)
                    .ifPresent(fecha -> alertas.add(nueva(id, TipoAlerta.POR_VENCER,
                            nombre + " tiene existencias que vencen el " + fecha)));
        }

        for (Pronostico pronostico : prediccionService.pronosticar(activos, lotesPorIngrediente, ahora)) {
            if (pronostico.requiereAlerta()) {
                alertas.add(nueva(pronostico.idIngrediente(),
                        TipoAlerta.AGOTAMIENTO_PREDICTIVO, pronostico.mensaje()));
            }
        }

        return alertas;
    }

    /** Guarda las alertas nuevas y cierra las que ya no aplican (por ejemplo, tras reponer). */
    public ResultadoSincronizacion sincronizar() {

        List<Alerta> deseadas = evaluar();

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

    private static Alerta nueva(Long idIngrediente, TipoAlerta tipo, String mensaje) {
        return Alerta.builder()
                .idIngrediente(idIngrediente)
                .tipoAlerta(tipo)
                .mensaje(mensaje)
                .activa(true)
                .build();
    }
}