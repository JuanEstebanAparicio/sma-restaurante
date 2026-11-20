package sma.restaurante.web.service;

import sma.restaurante.alerts.AlertaService;
import sma.restaurante.domain.Alerta;
import sma.restaurante.service.InventarioService;
import sma.restaurante.web.dto.DashboardDTO;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final InventarioService inventarioService;

    private final AlertaService alertaService;

    public DashboardService(
            InventarioService inventarioService,
            AlertaService alertaService) {

        this.inventarioService = inventarioService;
        this.alertaService = alertaService;
    }

    public DashboardDTO obtenerDashboard() {

        List<Alerta> agotados =
                alertaService.detectarAgotados();

        List<Alerta> proximosAVencer =
                alertaService.detectarProximosAVencer();

        List<Alerta> bajoStock =
                alertaService.detectarBajoStock();

        return DashboardDTO.builder()
                .totalIngredientes(
                        inventarioService
                                .listarIngredientes()
                                .size())
                .totalAlertas(
                        agotados.size()
                                + bajoStock.size()
                                + proximosAVencer.size())
                .totalAgotados(
                        agotados.size())
                .totalProximosAVencer(
                        proximosAVencer.size())
                .build();
    }
}