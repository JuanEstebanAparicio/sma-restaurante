package sma.restaurante;

import sma.restaurante.prediction.PrediccionService;
import sma.restaurante.repository.LoteRepository;
import sma.restaurante.repository.MovimientoRepository;
import sma.restaurante.repository.impl.LoteRepositoryPostgres;
import sma.restaurante.repository.impl.MovimientoRepositoryPostgres;

public class TestPrediccionService {

    public static void main(String[] args) {

        MovimientoRepository movimientoRepository =
                new MovimientoRepositoryPostgres();

        LoteRepository loteRepository =
                new LoteRepositoryPostgres();

        PrediccionService prediccionService =
                new PrediccionService(
                        loteRepository,
                        movimientoRepository
                );

        String prediccion =
                prediccionService
                        .predecirAgotamiento(
                                3L,
                                "POLLO",
                                "KG"
                        );

        System.out.println(
                prediccion
        );
    }
}
