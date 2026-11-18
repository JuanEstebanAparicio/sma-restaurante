package sma.restaurante.service;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.Movimiento;
import sma.restaurante.exception.IngredienteDuplicadoException;
import sma.restaurante.exception.ValidacionInventarioException;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.LoteRepository;
import sma.restaurante.repository.MovimientoRepository;
import sma.restaurante.util.NormalizadorTexto;

import java.math.BigDecimal;
import java.util.List;

public class InventarioService {

    private final IngredienteRepository ingredienteRepository;
    private final LoteRepository loteRepository;
    private final MovimientoRepository movimientoRepository;

    public InventarioService(
            IngredienteRepository ingredienteRepository,
            LoteRepository loteRepository,
            MovimientoRepository movimientoRepository) {

        this.ingredienteRepository = ingredienteRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public Ingrediente registrarIngrediente(
            Ingrediente ingrediente) {

        validarIngrediente(ingrediente);

        normalizarIngrediente(ingrediente);

        ingredienteRepository
                .buscarPorNombre(
                        ingrediente.getNombre()
                )
                .ifPresent(i -> {
                    throw new IngredienteDuplicadoException(
                            ingrediente.getNombre()
                    );
                });

        return ingredienteRepository
                .guardar(ingrediente);
    }

    public Lote registrarLote(
            Lote lote) {

        Lote loteGuardado =
                loteRepository.guardar(lote);

        Movimiento movimiento =
                Movimiento.builder()
                        .idIngrediente(
                                lote.getIdIngrediente())
                        .idLote(
                                loteGuardado.getIdLote())
                        .tipoMovimiento("ENTRADA")
                        .cantidad(
                                lote.getCantidad())
                        .motivo(
                                "REGISTRO DE LOTE")
                        .build();

        movimientoRepository.guardar(
                movimiento
        );

        return loteGuardado;
    }

    public List<Ingrediente> listarIngredientes() {

        return ingredienteRepository.listarTodos();
    }

    public List<Lote> obtenerLotesIngrediente(
            Long idIngrediente) {

        return loteRepository
                .listarPorIngrediente(
                        idIngrediente
                );
    }

    public BigDecimal obtenerStockDisponible(
            Long idIngrediente) {

        return loteRepository
                .listarPorIngrediente(
                        idIngrediente)
                .stream()
                .map(
                        Lote::getCantidadDisponible)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    public boolean consultarDisponibilidad(
            Long idIngrediente) {

        return obtenerStockDisponible(
                idIngrediente
        ).compareTo(
                BigDecimal.ZERO
        ) > 0;
    }

    public List<Movimiento> obtenerHistorialMovimientos(
            Long idIngrediente) {

        return movimientoRepository
                .listarPorIngrediente(
                        idIngrediente
                );
    }

    private void validarIngrediente(
            Ingrediente ingrediente) {

        if (ingrediente.getNombre() == null
                || ingrediente.getNombre().isBlank()) {

            throw new ValidacionInventarioException(
                    "El nombre es obligatorio"
            );
        }

        if (ingrediente.getUnidadMedida() == null
                || ingrediente.getUnidadMedida().isBlank()) {

            throw new ValidacionInventarioException(
                    "La unidad de medida es obligatoria"
            );
        }
    }

    private void normalizarIngrediente(
            Ingrediente ingrediente) {

        ingrediente.setNombre(
                NormalizadorTexto.normalizar(
                        ingrediente.getNombre()));

        ingrediente.setCategoria(
                NormalizadorTexto.normalizar(
                        ingrediente.getCategoria()));

        ingrediente.setEstado(
                NormalizadorTexto.normalizar(
                        ingrediente.getEstado()));
    }
}