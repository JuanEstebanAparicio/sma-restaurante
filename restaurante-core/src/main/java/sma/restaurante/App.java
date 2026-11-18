package sma.restaurante;

import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.impl.IngredienteRepositoryPostgres;

public class App {

    public static void main(String[] args) {

        IngredienteRepository repository =
                new IngredienteRepositoryPostgres();

        repository
                .listarTodos()
                .forEach(System.out::println);
    }
}