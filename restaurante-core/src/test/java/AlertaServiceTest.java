
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sma.restaurante.alerts.AlertaService;
import sma.restaurante.domain.Alerta;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.memoria.AlertaRepositoryMemoria;
import sma.restaurante.memoria.IngredienteRepositoryMemoria;
import sma.restaurante.memoria.LoteRepositoryMemoria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 21);

    private IngredienteRepositoryMemoria ingredientes;
    private LoteRepositoryMemoria lotes;
    private AlertaRepositoryMemoria alertas;
    private AlertaService servicio;

    @BeforeEach
    void preparar() {
        ingredientes = new IngredienteRepositoryMemoria();
        lotes = new LoteRepositoryMemoria();
        alertas = new AlertaRepositoryMemoria();
        servicio = new AlertaService(ingredientes, lotes, alertas);
    }

    private Ingrediente ingrediente(String nombre, String minimo) {
        return ingredientes.guardar(Ingrediente.builder()
                .nombre(nombre)
                .unidadMedida("KG")
                .stockMinimo(new BigDecimal(minimo))
                .estado("ACTIVO")
                .build());
    }

    private void lote(Ingrediente ingrediente, String disponible, LocalDate vence) {
        lotes.guardar(Lote.builder()
                .idIngrediente(ingrediente.getIdIngrediente())
                .cantidad(new BigDecimal(disponible))
                .cantidadDisponible(new BigDecimal(disponible))
                .fechaIngreso(HOY)
                .fechaVencimiento(vence)
                .build());
    }

    private List<String> tipos(List<Alerta> lista) {
        return lista.stream().map(Alerta::getTipoAlerta).toList();
    }

    @Test
    void ingredienteSinLotesEstaAgotadoYNoTieneBajoStock() {
        ingrediente("POLLO", "5");

        assertEquals(List.of("AGOTADO"), tipos(servicio.evaluar(HOY)));
    }

    @Test
    void stockPorDebajoDelMinimoGeneraBajoStock() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "3", HOY.plusDays(30));

        assertEquals(List.of("BAJO_STOCK"), tipos(servicio.evaluar(HOY)));
    }

    @Test
    void stockSobreElMinimoNoGeneraAlertas() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "20", HOY.plusDays(30));

        assertTrue(servicio.evaluar(HOY).isEmpty());
    }

    @Test
    void loteVencidoGeneraVencidoYNoCuentaParaElStock() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "20", HOY.minusDays(2));

        List<String> tipos = tipos(servicio.evaluar(HOY));

        assertTrue(tipos.contains("AGOTADO"));
        assertTrue(tipos.contains("VENCIDO"));
    }

    @Test
    void loteProximoAVencerGeneraAviso() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "20", HOY.plusDays(3));

        assertEquals(List.of("POR_VENCER"), tipos(servicio.evaluar(HOY)));
    }

    @Test
    void ingredienteInactivoNoGeneraAlertas() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        pollo.setEstado("INACTIVO");

        assertTrue(servicio.evaluar(HOY).isEmpty());
    }

    @Test
    void sincronizarNoDuplicaAlertasEnCiclosSeguidos() {
        ingrediente("POLLO", "5");

        AlertaService.ResultadoSincronizacion primero = servicio.sincronizar(HOY);
        AlertaService.ResultadoSincronizacion segundo = servicio.sincronizar(HOY);

        assertEquals(1, primero.nuevas().size());
        assertEquals(0, segundo.nuevas().size());
        assertEquals(1, alertas.listarActivas().size());
    }

    @Test
    void sincronizarResuelveLaAlertaCuandoSeRepone() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        servicio.sincronizar(HOY);

        lote(pollo, "50", HOY.plusDays(30));
        AlertaService.ResultadoSincronizacion resultado = servicio.sincronizar(HOY);

        assertEquals(1, resultado.resueltas());
        assertTrue(alertas.listarActivas().isEmpty());
    }
}