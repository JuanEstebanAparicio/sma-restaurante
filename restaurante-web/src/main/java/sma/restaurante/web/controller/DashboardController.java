package sma.restaurante.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sma.restaurante.web.dto.DashboardDTO;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String dashboard(Model model) {

        DashboardDTO dashboard =

                DashboardDTO.builder()
                        .totalIngredientes(0)
                        .totalAlertas(0)
                        .totalAgotados(0)
                        .totalProximosAVencer(0)
                        .build();

        model.addAttribute(
                "dashboard",
                dashboard
        );

        return "dashboard";
    }
}