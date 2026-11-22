
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sma.restaurante.alerts.AlertaService;
import sma.restaurante.domain.Alerta;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.Movimiento;
import sma.restaurante.memoria.AlertaRepositoryMemoria;
import sma.restaurante.memoria.IngredienteRepositoryMemoria;
import sma.restaurante.memoria.LoteRepositoryMemoria;
import sma.restaurante.memoria.MovimientoRepositoryMemoria;
import sma.restaurante.prediction.PrediccionService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 21, 12, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();

    private IngredienteRepositoryMemoria ingredientes;
    private LoteRepositoryMemoria lotes;
    private AlertaRepositoryMemoria alertas;
    private MovimientoRepositoryMemoria movimientos;
    private AlertaService servicio;

    @BeforeEach
    void preparar() {
        ingredientes = new IngredienteRepositoryMemoria();
        lotes = new LoteRepositoryMemoria();
        alertas = new AlertaRepositoryMemoria();
        movimientos = new MovimientoRepositoryMemoria();

        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(AHORA.atZone(zona).toInstant(), zona);

        servicio = new AlertaService(
                ingredientes, lotes, alertas, new PrediccionService(movimientos), reloj);
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

    private void movimiento(Ingrediente ingrediente, String tipo, String cantidad, LocalDateTime fecha) {
        movimientos.guardar(Movimiento.builder()
                .idIngrediente(ingrediente.getIdIngrediente())
                .tipoMovimiento(tipo)
                .cantidad(new BigDecimal(cantidad))
                .fechaMovimiento(fecha)
                .motivo("prueba")
                .build());
    }

    private List<String> tipos(List<Alerta> lista) {
        return lista.stream().map(Alerta::getTipoAlerta).toList();
    }

    // ---------- alertas por stock y vencimiento ----------

    @Test
    void ingredienteSinLotesEstaAgotadoYNoTieneBajoStock() {
        ingrediente("POLLO", "5");

        assertEquals(List.of("AGOTADO"), tipos(servicio.evaluar()));
    }

    @Test
    void stockPorDebajoDelMinimoGeneraBajoStock() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "3", HOY.plusDays(30));

        assertEquals(List.of("BAJO_STOCK"), tipos(servicio.evaluar()));
    }

    @Test
    void stockSobreElMinimoNoGeneraAlertas() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "20", HOY.plusDays(30));

        assertTrue(servicio.evaluar().isEmpty());
    }

    @Test
    void loteVencidoGeneraVencidoYNoCuentaParaElStock() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "20", HOY.minusDays(2));

        List<String> tipos = tipos(servicio.evaluar());

        assertTrue(tipos.contains("AGOTADO"));
        assertTrue(tipos.contains("VENCIDO"));
    }

    @Test
    void loteProximoAVencerGeneraAviso() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "20", HOY.plusDays(3));

        assertEquals(List.of("POR_VENCER"), tipos(servicio.evaluar()));
    }

    @Test
    void ingredienteInactivoNoGeneraAlertas() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        pollo.setEstado("INACTIVO");

        assertTrue(servicio.evaluar().isEmpty());
    }

    // ---------- sincronizacion ----------

    @Test
    void sincronizarNoDuplicaAlertasEnCiclosSeguidos() {
        ingrediente("POLLO", "5");

        AlertaService.ResultadoSincronizacion primero = servicio.sincronizar();
        AlertaService.ResultadoSincronizacion segundo = servicio.sincronizar();

        assertEquals(1, primero.nuevas().size());
        assertEquals(0, segundo.nuevas().size());
        assertEquals(1, alertas.listarActivas().size());
    }

    @Test
    void sincronizarResuelveLaAlertaCuandoSeRepone() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        servicio.sincronizar();

        lote(pollo, "50", HOY.plusDays(30));
        AlertaService.ResultadoSincronizacion resultado = servicio.sincronizar();

        assertEquals(1, resultado.resueltas());
        assertTrue(alertas.listarActivas().isEmpty());
    }

    // ---------- alerta predictiva ----------

    @Test
    void consumoAltoGeneraAlertaPredictivaAunqueHayaStock() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "15", HOY.plusDays(30));
        movimiento(pollo, "ENTRADA", "200", AHORA.minusDays(30));
        movimiento(pollo, "CONSUMO", "105", AHORA.minusDays(5));   // 105 kg / 21 dias = 5 kg al dia

        assertEquals(List.of("AGOTAMIENTO_PREDICTIVO"), tipos(servicio.evaluar()));
    }

    @Test
    void consumoBajoNoGeneraAlertaPredictiva() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "100", HOY.plusDays(30));
        movimiento(pollo, "ENTRADA", "200", AHORA.minusDays(30));
        movimiento(pollo, "CONSUMO", "21", AHORA.minusDays(5));    // 1 kg al dia: alcanza para 100 dias

        assertTrue(servicio.evaluar().isEmpty());
    }

    @Test
    void unIngredienteNuevoUsaSusDiasRealesDeHistorial() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "15", HOY.plusDays(30));
        movimiento(pollo, "ENTRADA", "45", AHORA.minusDays(2));    // 3 dias de historial
        movimiento(pollo, "CONSUMO", "30", AHORA.minusDays(1));    // 10 kg al dia: 1.5 dias de stock

        // Si se dividiera siempre entre 21 dias saldrian 10.5 dias de stock y no habria alerta.
        assertEquals(List.of("AGOTAMIENTO_PREDICTIVO"), tipos(servicio.evaluar()));
    }

    @Test
    void elConsumoFueraDeLaVentanaNoCuenta() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "15", HOY.plusDays(30));
        movimiento(pollo, "ENTRADA", "600", AHORA.minusDays(60));
        movimiento(pollo, "CONSUMO", "500", AHORA.minusDays(40));  // hace mas de 21 dias

        assertTrue(servicio.evaluar().isEmpty());
    }

    @Test
    void laAlertaPredictivaSeResuelveCuandoSeRepone() {
        Ingrediente pollo = ingrediente("POLLO", "5");
        lote(pollo, "15", HOY.plusDays(30));
        movimiento(pollo, "ENTRADA", "200", AHORA.minusDays(30));
        movimiento(pollo, "CONSUMO", "105", AHORA.minusDays(5));
        servicio.sincronizar();

        lote(pollo, "500", HOY.plusDays(30));
        AlertaService.ResultadoSincronizacion resultado = servicio.sincronizar();

        assertEquals(1, resultado.resueltas());
        assertTrue(alertas.listarActivas().isEmpty());
    }
}