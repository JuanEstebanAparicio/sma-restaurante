package sma.restaurante.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String dashboard(Model model) {

        model.addAttribute(
                "titulo",
                "Sistema Multiagente de Restaurante"
        );

        return "dashboard";
    }
}