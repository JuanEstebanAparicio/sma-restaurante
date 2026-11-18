package sma.restaurante.alerts;

import sma.restaurante.domain.Alerta;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.enums.TipoAlerta;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.LoteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlertaService {

    private final IngredienteRepository
            ingredienteRepository;

    private final LoteRepository
            loteRepository;

    public AlertaService(
            IngredienteRepository ingredienteRepository,
            LoteRepository loteRepository) {

        this.ingredienteRepository =
                ingredienteRepository;

        this.loteRepository =
                loteRepository;
    }

    public List<Alerta> detectarAgotados() {

        List<Alerta> alertas =
                new ArrayList<>();

        for (Ingrediente ingrediente :
                ingredienteRepository.listarTodos()) {

            BigDecimal stock = loteRepository
                    .listarPorIngrediente(
                            ingrediente.getIdIngrediente())
                    .stream()
                    .map(Lote::getCantidadDisponible)
                    .reduce(
                            BigDecimal.ZERO,
                            BigDecimal::add
                    );

            if (stock.compareTo(
                    BigDecimal.ZERO) <= 0) {

                alertas.add(

                        Alerta.builder()
                                .idIngrediente(
                                        ingrediente.getIdIngrediente())
                                .tipoAlerta(
                                        "AGOTADO")
                                .mensaje(
                                        "Ingrediente agotado: "
                                                + ingrediente.getNombre())
                                .activa(true)
                                .build()
                );
            }
        }

        return alertas;
    }

    public List<Alerta> detectarBajoStock() {

        List<Alerta> alertas =
                new ArrayList<>();

        for (Ingrediente ingrediente :
                ingredienteRepository.listarTodos()) {

            BigDecimal stockDisponible =

                    loteRepository
                            .listarPorIngrediente(
                                    ingrediente.getIdIngrediente())
                            .stream()
                            .map(Lote::getCantidadDisponible)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            if (stockDisponible.compareTo(
                    ingrediente.getStockMinimo()) <= 0) {

                alertas.add(

                        Alerta.builder()
                                .idIngrediente(
                                        ingrediente.getIdIngrediente())
                                .tipoAlerta(
                                        TipoAlerta.BAJO_STOCK.name())
                                .mensaje(
                                        "Stock bajo para "
                                                + ingrediente.getNombre()
                                                + ". Disponible: "
                                                + stockDisponible
                                                + " "
                                                + ingrediente.getUnidadMedida()
                                                + ". Stock mínimo: "
                                                + ingrediente.getStockMinimo())
                                .activa(true)
                                .build()
                );
            }
        }

        return alertas;
    }

    public List<Alerta> detectarProximosAVencer() {

        List<Alerta> alertas =
                new ArrayList<>();

        LocalDate hoy =
                LocalDate.now();

        LocalDate limite =
                hoy.plusDays(5);

        List<Lote> lotes =
                loteRepository.listarTodos();

        for (Lote lote : lotes) {

            if (lote.getFechaVencimiento() == null) {
                continue;
            }

            if (lote.getCantidadDisponible()
                    .compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            LocalDate fechaVencimiento =
                    lote.getFechaVencimiento();

            boolean proximoAVencer =

                    !fechaVencimiento.isBefore(hoy)

                            &&

                            !fechaVencimiento.isAfter(limite);

            if (proximoAVencer) {

                Optional<Ingrediente> ingredienteOpt =

                        ingredienteRepository
                                .buscarPorId(
                                        lote.getIdIngrediente()
                                );

                String nombreIngrediente =

                        ingredienteOpt
                                .map(Ingrediente::getNombre)
                                .orElse("DESCONOCIDO");

                alertas.add(

                        Alerta.builder()
                                .idIngrediente(
                                        lote.getIdIngrediente()
                                )
                                .tipoAlerta(
                                        TipoAlerta.POR_VENCER.name()
                                )
                                .mensaje(
                                        "Lote próximo a vencer de "
                                                + nombreIngrediente
                                                + ". Fecha de vencimiento: "
                                                + fechaVencimiento
                                )
                                .activa(true)
                                .build()
                );
            }
        }

        return alertas;
    }


}