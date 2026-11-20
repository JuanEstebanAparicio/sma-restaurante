package sma.restaurante.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.service.InventarioService;
import sma.restaurante.web.dto.IngredienteFormDTO;

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

    @GetMapping("/ingredientes/nuevo")
    public String mostrarFormulario(
            Model model) {

        model.addAttribute(
                "ingrediente",
                new IngredienteFormDTO()
        );

        return "ingredientes/formulario";
    }

    @PostMapping("/ingredientes")
    public String guardarIngrediente(
            @ModelAttribute("ingrediente")
            IngredienteFormDTO form) {

        Ingrediente ingrediente =
                Ingrediente.builder()
                        .nombre(form.getNombre())
                        .descripcion(form.getDescripcion())
                        .categoria(form.getCategoria())
                        .unidadMedida(form.getUnidadMedida())
                        .stockMinimo(form.getStockMinimo())
                        .estado("ACTIVO")
                        .observaciones(form.getObservaciones())
                        .build();

        inventarioService
                .registrarIngrediente(
                        ingrediente
                );

        return "redirect:/ingredientes";
    }
}