package sma.restaurante;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.LoteRepository;
import sma.restaurante.repository.MovimientoRepository;
import sma.restaurante.repository.impl.IngredienteRepositoryPostgres;
import sma.restaurante.repository.impl.LoteRepositoryPostgres;
import sma.restaurante.repository.impl.MovimientoRepositoryPostgres;
import sma.restaurante.service.InventarioService;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TestInventarioService {

    public static void main(String[] args) {

        IngredienteRepository ingredienteRepository =
                new IngredienteRepositoryPostgres();

        LoteRepository loteRepository =
                new LoteRepositoryPostgres();

        MovimientoRepository movimientoRepository =
                new MovimientoRepositoryPostgres();

        InventarioService inventarioService =
                new InventarioService(
                        ingredienteRepository,
                        loteRepository,
                        movimientoRepository
                );

        System.out.println("===== REGISTRAR INGREDIENTE =====");

        Ingrediente ingrediente =
                Ingrediente.builder()
                        .nombre("   Pollo Pechuga  ")
                        .descripcion("Pechuga fresca")
                        .categoria(" carnes ")
                        .unidadMedida("kg")
                        .stockMinimo(
                                BigDecimal.valueOf(5))
                        .estado("activo")
                        .observaciones("Prueba")
                        .build();

        Ingrediente ingredienteGuardado =
                inventarioService
                        .registrarIngrediente(
                                ingrediente);

        System.out.println(
                ingredienteGuardado
        );

        System.out.println();
        System.out.println(
                "===== REGISTRAR LOTE ====="
        );

        Lote lote =
                Lote.builder()
                        .idIngrediente(
                                ingredienteGuardado.getIdIngrediente())
                        .cantidad(
                                BigDecimal.valueOf(20))
                        .cantidadDisponible(
                                BigDecimal.valueOf(20))
                        .costoUnitario(
                                BigDecimal.valueOf(15000))
                        .proveedor(
                                "Proveedor Demo")
                        .ubicacion(
                                "Nevera Principal")
                        .fechaIngreso(
                                LocalDate.now())
                        .fechaVencimiento(
                                LocalDate.now().plusDays(15))
                        .build();

        Lote loteGuardado =
                inventarioService
                        .registrarLote(lote);

        System.out.println(
                loteGuardado
        );

        System.out.println();
        System.out.println(
                "===== STOCK DISPONIBLE ====="
        );

        System.out.println(
                inventarioService
                        .obtenerStockDisponible(
                                ingredienteGuardado.getIdIngrediente())
        );

        System.out.println();
        System.out.println(
                "===== DISPONIBILIDAD ====="
        );

        System.out.println(
                inventarioService
                        .consultarDisponibilidad(
                                ingredienteGuardado.getIdIngrediente())
        );

        System.out.println();
        System.out.println(
                "===== LOTES ====="
        );

        inventarioService
                .obtenerLotesIngrediente(
                        ingredienteGuardado.getIdIngrediente())
                .forEach(System.out::println);

        System.out.println();
        System.out.println(
                "===== MOVIMIENTOS ====="
        );

        inventarioService
                .obtenerHistorialMovimientos(
                        ingredienteGuardado.getIdIngrediente())
                .forEach(System.out::println);

        System.out.println();
        System.out.println(
                "TEST INVENTARIO SERVICE OK"
        );
    }
}