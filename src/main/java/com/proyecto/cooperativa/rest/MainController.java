package com.proyecto.cooperativa.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @GetMapping("/")
    public String index(Model modelo) {
        modelo.addAttribute("mensaje", "hola mundo");
        return "index";
    }
}
