package sma.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrediccionConsumo {

    private Long idPrediccion;

    private Long idIngrediente;

    private BigDecimal stockActual;

    private BigDecimal consumoPromedioDiario;

    private BigDecimal diasEstimadosRestantes;

    private LocalDateTime fechaCalculo;

}
