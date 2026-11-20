package sma.restaurante.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sma.restaurante.service.InventarioService;

@Controller
public class IngredienteController {

    private final InventarioService
            inventarioService;

    public IngredienteController(
            InventarioService inventarioService) {

        this.inventarioService =
                inventarioService;
    }

    @GetMapping("/ingredientes")
    public String listarIngredientes(
            Model model) {

        model.addAttribute(
                "ingredientes",
                inventarioService
                        .listarIngredientes()
        );

        return "ingredientes/lista";
    }
}