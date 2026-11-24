package sma.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sma.restaurante.domain.enums.TipoAlerta;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    private Long idAlerta;

    private Long idIngrediente;

    private TipoAlerta tipoAlerta;

    private String mensaje;

    private Boolean activa;

    private LocalDateTime fechaGeneracion;

    private LocalDateTime fechaResolucion;

}
