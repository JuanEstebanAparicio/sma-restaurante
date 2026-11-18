package sma.restaurante.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movimiento {

    private Long idMovimiento;

    private Long idIngrediente;

    private Long idLote;

    private String tipoMovimiento;

    private BigDecimal cantidad;

    private LocalDateTime fechaMovimiento;

    private String motivo;

}
