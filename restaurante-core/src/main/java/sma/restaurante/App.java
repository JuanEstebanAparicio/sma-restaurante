package sma.restaurante;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.impl.IngredienteRepositoryPostgres;

import java.math.BigDecimal;

public class App {

    public static void main(String[] args) {

        IngredienteRepository repository =
                new IngredienteRepositoryPostgres();
        Ingrediente ingrediente =
                Ingrediente.builder()
                        .nombre("Tomate")
                        .descripcion("Tomate chonto")
                        .categoria("Vegetales")
                        .unidadMedida("kg")
                        .stockMinimo(BigDecimal.valueOf(5))
                        .estado("ACTIVO")
                        .observaciones("Prueba")
                        .build();

        repository.guardar(ingrediente);

        System.out.println(ingrediente);
    }
}