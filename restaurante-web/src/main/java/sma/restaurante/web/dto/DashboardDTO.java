package sma.restaurante.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardDTO {

    private Integer totalIngredientes;

    private Integer totalAlertas;

    private Integer totalAgotados;

    private Integer totalProximosAVencer;

    private Integer totalBajoStock;
}