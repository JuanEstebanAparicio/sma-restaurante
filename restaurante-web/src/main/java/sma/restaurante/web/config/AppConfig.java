package sma.restaurante.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import sma.restaurante.alerts.AlertaService;
import sma.restaurante.repository.*;
import sma.restaurante.repository.impl.*;
import sma.restaurante.service.InventarioService;

@Configuration
public class AppConfig {

    @Bean
    public IngredienteRepository ingredienteRepository() {
        return new IngredienteRepositoryPostgres();
    }

    @Bean
    public LoteRepository loteRepository() {
        return new LoteRepositoryPostgres();
    }

    @Bean
    public MovimientoRepository movimientoRepository() {
        return new MovimientoRepositoryPostgres();
    }

    @Bean
    public InventarioService inventarioService(
            IngredienteRepository ingredienteRepository,
            LoteRepository loteRepository,
            MovimientoRepository movimientoRepository) {

        return new InventarioService(
                ingredienteRepository,
                loteRepository,
                movimientoRepository
        );
    }

    @Bean
    public AlertaService alertaService(
            IngredienteRepository ingredienteRepository,
            LoteRepository loteRepository) {

        return new AlertaService(
                ingredienteRepository,
                loteRepository
        );
    }
}