package sma.restaurante;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.impl.IngredienteRepositoryPostgres;

import java.math.BigDecimal;

public class TestIngredienteRepository {

    public static void main(String[] args) {

        IngredienteRepository repository =
                new IngredienteRepositoryPostgres();

        System.out.println("===== CREAR =====");

        Ingrediente ingrediente =
                Ingrediente.builder()
                        .nombre("Zanahoria")
                        .descripcion("Tomate de prueba")
                        .categoria("Vegetales")
                        .unidadMedida("KG")
                        .stockMinimo(
                                BigDecimal.valueOf(5))
                        .estado("ACTIVO")
                        .observaciones("Prueba CRUD")
                        .build();

        ingrediente =
                repository.guardar(ingrediente);

        System.out.println(ingrediente);

        System.out.println("===== BUSCAR POR ID =====");

        repository.buscarPorId(
                        ingrediente.getIdIngrediente())
                .ifPresent(System.out::println);

        System.out.println("===== BUSCAR POR NOMBRE =====");

        repository.buscarPorNombre("Tomate")
                .ifPresent(System.out::println);

        System.out.println("===== ACTUALIZAR =====");

        ingrediente.setDescripcion(
                "Tomate actualizado");

        repository.actualizar(
                ingrediente);

        repository.buscarPorId(
                        ingrediente.getIdIngrediente())
                .ifPresent(System.out::println);

        System.out.println("===== LISTAR =====");

        repository.listarTodos()
                .forEach(System.out::println);

        System.out.println("===== ELIMINAR =====");

        repository.eliminar(
                ingrediente.getIdIngrediente()
        );

        repository.buscarPorId(
                        ingrediente.getIdIngrediente())
                .ifPresent(System.out::println);

        System.out.println("CRUD INGREDIENTE OK");
    }
}