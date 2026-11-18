package sma.restaurante;

import sma.restaurante.domain.Lote;
import sma.restaurante.repository.LoteRepository;
import sma.restaurante.repository.impl.LoteRepositoryPostgres;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TestLoteRepository {

    public static void main(String[] args) {

        LoteRepository repository =
                new LoteRepositoryPostgres();

        System.out.println("===== CREAR =====");

        Lote lote =
                Lote.builder()
                        .idIngrediente(1L)
                        .cantidad(
                                BigDecimal.valueOf(20))
                        .cantidadDisponible(
                                BigDecimal.valueOf(20))
                        .costoUnitario(
                                BigDecimal.valueOf(18000))
                        .proveedor("Proveedor Demo")
                        .ubicacion("Nevera 1")
                        .fechaIngreso(
                                LocalDate.now())
                        .fechaVencimiento(
                                LocalDate.now().plusDays(15))
                        .build();

        lote = repository.guardar(lote);

        System.out.println(lote);

        System.out.println("===== BUSCAR POR ID =====");

        repository.buscarPorId(
                        lote.getIdLote())
                .ifPresent(System.out::println);

        System.out.println("===== LISTAR POR INGREDIENTE =====");

        repository.listarPorIngrediente(
                        lote.getIdIngrediente())
                .forEach(System.out::println);

        System.out.println("===== LISTAR TODOS =====");

        repository.listarTodos()
                .forEach(System.out::println);

        System.out.println("===== ACTUALIZAR =====");

        lote.setCantidadDisponible(
                BigDecimal.valueOf(15));

        repository.actualizar(lote);

        repository.buscarPorId(
                        lote.getIdLote())
                .ifPresent(System.out::println);

        System.out.println("===== ELIMINAR =====");

        repository.eliminar(
                lote.getIdLote());

        System.out.println("CRUD LOTE OK");
    }
}