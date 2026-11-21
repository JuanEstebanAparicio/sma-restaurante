
import org.junit.jupiter.api.Test;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.ReglasStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasStockTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 21);

    private static Lote lote(String disponible, LocalDate vence) {
        return Lote.builder()
                .idIngrediente(1L)
                .cantidad(new BigDecimal(disponible))
                .cantidadDisponible(new BigDecimal(disponible))
                .fechaVencimiento(vence)
                .build();
    }

    private static void assertCantidad(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void loteVencidoNoCuentaComoStock() {
        List<Lote> lotes = List.of(
                lote("10", HOY.minusDays(1)),
                lote("4", HOY.plusDays(10)));

        assertCantidad("4", ReglasStock.stockVigente(lotes, HOY));
        assertCantidad("10", ReglasStock.stockVencido(lotes, HOY));
    }

    @Test
    void loteQueVenceHoyAunCuenta() {
        assertCantidad("7", ReglasStock.stockVigente(List.of(lote("7", HOY)), HOY));
    }

    @Test
    void loteSinFechaDeVencimientoCuenta() {
        assertCantidad("3", ReglasStock.stockVigente(List.of(lote("3", null)), HOY));
    }

    @Test
    void proximoVencimientoDevuelveElMasCercanoDentroDelAviso() {
        List<Lote> lotes = List.of(
                lote("5", HOY.plusDays(4)),
                lote("5", HOY.plusDays(2)),
                lote("5", HOY.plusDays(30)));

        assertEquals(HOY.plusDays(2), ReglasStock.proximoVencimiento(lotes, HOY, 5).orElseThrow());
    }

    @Test
    void proximoVencimientoIgnoraLotesSinExistenciasYVencidos() {
        List<Lote> lotes = List.of(
                lote("0", HOY.plusDays(1)),
                lote("5", HOY.minusDays(1)));

        assertTrue(ReglasStock.proximoVencimiento(lotes, HOY, 5).isEmpty());
    }
}