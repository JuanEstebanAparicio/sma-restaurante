import org.junit.jupiter.api.Test;
import sma.restaurante.domain.enums.NivelRiesgo;
import sma.restaurante.prediction.Pronostico;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PronosticoTest {

    private static Pronostico con(String diasRestantes, int diasObservados) {
        return new Pronostico(1L, "POLLO", "KG", new BigDecimal("20"), new BigDecimal("14"),
                new BigDecimal(diasRestantes), diasObservados, NivelRiesgo.CRITICO);
    }

    @Test
    void losDiasSeMuestranEnterosYRedondeadosHaciaAbajo() {
        assertEquals("Aunque quedan 20 KG de POLLO, segun el consumo observado en 21 dias"
                + " se agotara en aprox. 1 dia", con("1.4", 21).mensaje());
        assertTrue(con("1.9", 21).mensaje().endsWith("en aprox. 1 dia"));
        assertTrue(con("3.4", 21).mensaje().endsWith("en aprox. 3 dias"));
        assertTrue(con("7.0", 21).mensaje().endsWith("en aprox. 7 dias"));
    }

    @Test
    void conMenosDeUnDiaAvisaQueSeAgotaEnMenosDeUnDia() {
        assertTrue(con("0.6", 21).mensaje().endsWith("en menos de 1 dia"));
    }

    @Test
    void conUnSoloDiaDeHistorialUsaElSingular() {
        assertTrue(con("2.5", 1).mensaje().contains("observado en 1 dia se agotara"));
    }
}