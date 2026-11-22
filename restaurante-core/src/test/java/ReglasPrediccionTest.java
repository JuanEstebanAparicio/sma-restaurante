
import org.junit.jupiter.api.Test;
import sma.restaurante.domain.ReglasPrediccion;
import sma.restaurante.domain.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglasPrediccionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 21);

    private static void assertCantidad(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "esperado " + esperado + " pero fue " + real);
    }

    private static NivelRiesgo nivel(String dias) {
        return ReglasPrediccion.nivel(Optional.of(new BigDecimal(dias)));
    }

    @Test
    void losDiasObservadosTienenTopeEnLaVentana() {
        assertEquals(21, ReglasPrediccion.diasObservados(HOY.minusDays(100), HOY));
    }

    @Test
    void unIngredienteNuevoUsaSusDiasReales() {
        assertEquals(3, ReglasPrediccion.diasObservados(HOY.minusDays(2), HOY));
    }

    @Test
    void unPrimerMovimientoDeHoyCuentaComoUnDia() {
        assertEquals(1, ReglasPrediccion.diasObservados(HOY, HOY));
    }

    @Test
    void sinMovimientosNoHayDiasObservados() {
        assertEquals(0, ReglasPrediccion.diasObservados(null, HOY));
    }

    @Test
    void elConsumoDiarioEsElTotalEntreLosDiasObservados() {
        assertCantidad("5", ReglasPrediccion.consumoDiario(new BigDecimal("105"), 21));
    }

    @Test
    void sinConsumoNoSePuedeEstimarElAgotamiento() {
        assertTrue(ReglasPrediccion.diasRestantes(new BigDecimal("15"), BigDecimal.ZERO).isEmpty());
    }

    @Test
    void diasRestantesEsElStockEntreElConsumoDiario() {
        assertCantidad("3.0",
                ReglasPrediccion.diasRestantes(new BigDecimal("15"), new BigDecimal("5")).orElseThrow());
    }

    @Test
    void losNivelesRespetanSusLimites() {
        assertEquals(NivelRiesgo.CRITICO, nivel("3.0"));
        assertEquals(NivelRiesgo.ALTO, nivel("3.1"));
        assertEquals(NivelRiesgo.ALTO, nivel("7.0"));
        assertEquals(NivelRiesgo.MODERADO, nivel("7.1"));
        assertEquals(NivelRiesgo.MODERADO, nivel("15.0"));
        assertEquals(NivelRiesgo.BAJO, nivel("15.1"));
        assertEquals(NivelRiesgo.SIN_CONSUMO, ReglasPrediccion.nivel(Optional.empty()));
    }
}