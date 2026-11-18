package sma.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lote {

    private Long idLote;

    private Long idIngrediente;

    private BigDecimal cantidad;

    private BigDecimal cantidadDisponible;

    private BigDecimal costoUnitario;

    private String proveerdor;

    private String ubicacion;

    private LocalDate fechaIngreso;

    private LocalDate fechaVencimiento;

}
