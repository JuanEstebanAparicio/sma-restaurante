package sma.restaurante.domain;

import java.time.LocalDateTime;

public class Alerta {

    private Long idAlerta;

    private Long idIngrediente;

    private String tipoAlerta;

    private String mensaje;

    private Boolean activa;

    private LocalDateTime fechaGeneracion;

    private LocalDateTime fechaResolucion;

}
