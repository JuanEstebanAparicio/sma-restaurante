package sma.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sma.restaurante.domain.enums.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movimiento {

    private Long idMovimiento;

    private Long idIngrediente;

    private Long idLote;

    private TipoMovimiento tipoMovimiento;

    private BigDecimal cantidad;

    private LocalDateTime fechaMovimiento;

    private String motivo;

}
