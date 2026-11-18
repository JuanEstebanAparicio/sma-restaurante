package sma.restaurante;

import sma.restaurante.domain.Movimiento;
import sma.restaurante.repository.MovimientoRepository;
import sma.restaurante.repository.impl.MovimientoRepositoryPostgres;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TestMovimientoRepository {

    public static void main(String[] args) {

        MovimientoRepository repository =
                new MovimientoRepositoryPostgres();

        System.out.println("===== GUARDAR =====");

        Movimiento movimiento =
                Movimiento.builder()
                        .idIngrediente(1L)
                        .idLote(7L)
                        .tipoMovimiento("ENTRADA")
                        .cantidad(BigDecimal.valueOf(10))
                        .motivo("Carga inicial")
                        .build();

        movimiento = repository.guardar(movimiento);

        System.out.println(movimiento);

        System.out.println("===== BUSCAR POR ID =====");

        repository.buscarPorId(
                        movimiento.getIdMovimiento())
                .ifPresent(System.out::println);

        System.out.println("===== LISTAR TODOS =====");

        repository.listarTodos()
                .forEach(System.out::println);

        System.out.println("===== LISTAR POR INGREDIENTE =====");

        repository.listarPorIngrediente(1L)
                .forEach(System.out::println);

        System.out.println("===== LISTAR POR TIPO =====");

        repository.listarPorTipo("ENTRADA")
                .forEach(System.out::println);

        System.out.println("===== LISTAR POR PERIODO =====");

        repository.listarPorPeriodo(
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now()
        ).forEach(System.out::println);

        System.out.println(
                "CRUD MOVIMIENTO OK"
        );
    }
}