package sma.restaurante.service;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.Movimiento;
import sma.restaurante.domain.ReglasStock;
import sma.restaurante.domain.enums.TipoMovimiento;
import sma.restaurante.exception.IngredienteDuplicadoException;
import sma.restaurante.exception.IngredienteNoEncontradoException;
import sma.restaurante.exception.ValidacionInventarioException;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.LoteRepository;
import sma.restaurante.repository.MovimientoRepository;
import sma.restaurante.repository.Transaccion;
import sma.restaurante.util.NormalizadorTexto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventarioService {

    private final IngredienteRepository ingredienteRepository;
    private final LoteRepository loteRepository;
    private final MovimientoRepository movimientoRepository;
    private final Transaccion transaccion;

    public InventarioService(
            IngredienteRepository ingredienteRepository,
            LoteRepository loteRepository,
            MovimientoRepository movimientoRepository,
            Transaccion transaccion) {

        this.ingredienteRepository = ingredienteRepository;
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
        this.transaccion = transaccion;
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

    /** Guarda el lote y su movimiento de entrada: o se guardan los dos, o ninguno. */
    public Lote registrarLote(
            Lote lote) {

        validarLote(lote);

        return transaccion.ejecutar(() -> {

            Lote loteGuardado = loteRepository.guardar(lote);

            movimientoRepository.guardar(
                    Movimiento.builder()
                            .idIngrediente(lote.getIdIngrediente())
                            .idLote(loteGuardado.getIdLote())
                            .tipoMovimiento(TipoMovimiento.ENTRADA.name())
                            .cantidad(lote.getCantidad())
                            .motivo("REGISTRO DE LOTE")
                            .build());

            return loteGuardado;
        });
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

        return ReglasStock.stockVigente(
                loteRepository.listarPorIngrediente(idIngrediente),
                LocalDate.now());
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

    private void validarLote(Lote lote) {

        if (lote.getIdIngrediente() == null) {
            throw new ValidacionInventarioException("El lote necesita un ingrediente");
        }

        ingredienteRepository.buscarPorId(lote.getIdIngrediente())
                .orElseThrow(() -> new IngredienteNoEncontradoException(lote.getIdIngrediente()));

        if (lote.getCantidad() == null || lote.getCantidad().signum() <= 0) {
            throw new ValidacionInventarioException("La cantidad del lote debe ser mayor que cero");
        }

        if (lote.getCostoUnitario() == null || lote.getCostoUnitario().signum() < 0) {
            throw new ValidacionInventarioException("El costo unitario no puede ser negativo");
        }

        if (lote.getFechaIngreso() == null) {
            lote.setFechaIngreso(LocalDate.now());
        }

        if (lote.getFechaVencimiento() != null
                && lote.getFechaVencimiento().isBefore(lote.getFechaIngreso())) {
            throw new ValidacionInventarioException("El lote no puede vencer antes de su ingreso");
        }

        if (lote.getCantidadDisponible() == null) {
            lote.setCantidadDisponible(lote.getCantidad());
        }
    }

    private void normalizarIngrediente(
            Ingrediente ingrediente) {

        if (ingrediente.getEstado() == null || ingrediente.getEstado().isBlank()) {
            ingrediente.setEstado("ACTIVO");
        }
        if (ingrediente.getStockMinimo() == null) {
            ingrediente.setStockMinimo(BigDecimal.ZERO);
        }

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