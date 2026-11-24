package sma.restaurante.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sma.restaurante.domain.enums.EstadoIngrediente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ingrediente {

    private Long idIngrediente;

    private String nombre;

    private String descripcion;

    private String categoria;

    private String unidadMedida;

    private BigDecimal stockMinimo;

    private EstadoIngrediente estado;

    private String observaciones;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;


}

