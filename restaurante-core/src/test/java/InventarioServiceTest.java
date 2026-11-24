

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.Movimiento;
import sma.restaurante.domain.enums.EstadoIngrediente;
import sma.restaurante.domain.enums.TipoMovimiento;
import sma.restaurante.exception.IngredienteDuplicadoException;
import sma.restaurante.exception.IngredienteNoEncontradoException;
import sma.restaurante.exception.ValidacionInventarioException;
import sma.restaurante.memoria.IngredienteRepositoryMemoria;
import sma.restaurante.memoria.LoteRepositoryMemoria;
import sma.restaurante.memoria.MovimientoRepositoryMemoria;
import sma.restaurante.memoria.TransaccionMemoria;
import sma.restaurante.service.InventarioService;
import sma.restaurante.util.Tiempo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioServiceTest {

    private LoteRepositoryMemoria lotes;
    private MovimientoRepositoryMemoria movimientos;
    private TransaccionMemoria transaccion;
    private InventarioService servicio;

    @BeforeEach
    void preparar() {
        lotes = new LoteRepositoryMemoria();
        movimientos = new MovimientoRepositoryMemoria();
        transaccion = new TransaccionMemoria();
        servicio = new InventarioService(
                new IngredienteRepositoryMemoria(), lotes, movimientos, transaccion);
    }

    private Ingrediente pollo() {
        return servicio.registrarIngrediente(Ingrediente.builder()
                .nombre("  pollo ")
                .unidadMedida("KG")
                .build());
    }

    private Lote.LoteBuilder loteDe(Ingrediente ingrediente) {
        return Lote.builder()
                .idIngrediente(ingrediente.getIdIngrediente())
                .cantidad(new BigDecimal("10"))
                .costoUnitario(new BigDecimal("8000"));
    }

    @Test
    void registrarIngredienteAplicaValoresPorDefectoYNormaliza() {
        Ingrediente pollo = pollo();

        assertEquals("POLLO", pollo.getNombre());
        assertEquals(EstadoIngrediente.ACTIVO, pollo.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(pollo.getStockMinimo()));
    }

    @Test
    void registrarIngredienteDuplicadoFalla() {
        pollo();

        assertThrows(IngredienteDuplicadoException.class, this::pollo);
    }

    @Test
    void registrarIngredienteSinUnidadFalla() {
        assertThrows(ValidacionInventarioException.class, () ->
                servicio.registrarIngrediente(Ingrediente.builder().nombre("ARROZ").build()));
    }

    @Test
    void registrarLoteGuardaUnMovimientoDeEntradaDentroDeUnaTransaccion() {
        Ingrediente pollo = pollo();

        Lote guardado = servicio.registrarLote(loteDe(pollo).build());

        List<Movimiento> historial = movimientos.listarPorIngrediente(pollo.getIdIngrediente());
        assertEquals(1, historial.size());
        assertEquals(TipoMovimiento.ENTRADA, historial.get(0).getTipoMovimiento());
        assertEquals(guardado.getIdLote(), historial.get(0).getIdLote());
        assertEquals(1, transaccion.ejecuciones());
    }

    @Test
    void registrarLoteUsaLaCantidadComoDisponibleSiNoSeIndica() {
        Ingrediente pollo = pollo();

        Lote guardado = servicio.registrarLote(loteDe(pollo).build());

        assertEquals(0, new BigDecimal("10").compareTo(guardado.getCantidadDisponible()));
        assertEquals(Tiempo.hoy(), guardado.getFechaIngreso());
    }

    @Test
    void registrarLoteConCantidadCeroFallaSinTocarLaBase() {
        Ingrediente pollo = pollo();

        assertThrows(ValidacionInventarioException.class, () ->
                servicio.registrarLote(loteDe(pollo).cantidad(BigDecimal.ZERO).build()));

        assertEquals(0, transaccion.ejecuciones());
        assertEquals(0, lotes.listarTodos().size());
    }

    @Test
    void registrarLoteDeUnIngredienteInexistenteFalla() {
        assertThrows(IngredienteNoEncontradoException.class, () ->
                servicio.registrarLote(Lote.builder()
                        .idIngrediente(99L)
                        .cantidad(BigDecimal.TEN)
                        .costoUnitario(BigDecimal.ONE)
                        .build()));
    }

    @Test
    void registrarLoteQueVenceAntesDeIngresarFalla() {
        Ingrediente pollo = pollo();

        assertThrows(ValidacionInventarioException.class, () ->
                servicio.registrarLote(loteDe(pollo)
                        .fechaIngreso(Tiempo.hoy())
                        .fechaVencimiento(Tiempo.hoy().minusDays(1))
                        .build()));
    }

    @Test
    void elStockDisponibleIgnoraLosLotesVencidos() {
        Ingrediente pollo = pollo();
        lotes.guardar(loteDe(pollo)
                .cantidadDisponible(new BigDecimal("10"))
                .fechaVencimiento(Tiempo.hoy().minusDays(1))
                .build());
        lotes.guardar(loteDe(pollo)
                .cantidadDisponible(new BigDecimal("4"))
                .fechaVencimiento(Tiempo.hoy().plusDays(10))
                .build());

        assertEquals(0, new BigDecimal("4").compareTo(servicio.obtenerStockDisponible(pollo.getIdIngrediente())));
    }
}