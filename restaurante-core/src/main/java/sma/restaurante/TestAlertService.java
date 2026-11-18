package sma.restaurante;

import sma.restaurante.alerts.AlertaService;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.LoteRepository;
import sma.restaurante.repository.impl.IngredienteRepositoryPostgres;
import sma.restaurante.repository.impl.LoteRepositoryPostgres;

public class TestAlertService {
    public static void main(String[] args) {
        IngredienteRepository ingredienteRepository =
                new IngredienteRepositoryPostgres();

        LoteRepository loteRepository =
                new LoteRepositoryPostgres();
        AlertaService alertaService =
                new AlertaService(
                        ingredienteRepository,
                        loteRepository
                );

        alertaService
                .detectarBajoStock()
                .forEach(System.out::println);
    }
}

