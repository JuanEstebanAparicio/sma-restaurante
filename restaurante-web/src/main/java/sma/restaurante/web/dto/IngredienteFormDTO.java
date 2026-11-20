package sma.restaurante.web.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IngredienteFormDTO {

    private String nombre;

    private String descripcion;

    private String categoria;

    private String unidadMedida;

    private BigDecimal stockMinimo;

    private String observaciones;
}