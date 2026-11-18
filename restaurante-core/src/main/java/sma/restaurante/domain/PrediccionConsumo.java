package sma.restaurante.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PrediccionConsumo {

    private Long idPrediccion;

    private Long idIngrediente;

    private BigDecimal stockActual;

    private BigDecimal consumoPromedioDiario;

    private BigDecimal diasEstimadosRestantes;

    private LocalDateTime fechaCalculo;

}
