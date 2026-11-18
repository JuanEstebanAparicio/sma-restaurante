package sma.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    private Long idAlerta;

    private Long idIngrediente;

    private String tipoAlerta;

    private String mensaje;

    private Boolean activa;

    private LocalDateTime fechaGeneracion;

    private LocalDateTime fechaResolucion;

}
