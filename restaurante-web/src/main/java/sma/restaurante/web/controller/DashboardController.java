package sma.restaurante.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sma.restaurante.web.dto.DashboardDTO;
import sma.restaurante.web.service.DashboardService;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService) {

        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {

        DashboardDTO dashboard =
                dashboardService.obtenerDashboard();

        model.addAttribute(
                "dashboard",
                dashboard
        );

        return "dashboard";
    }
}