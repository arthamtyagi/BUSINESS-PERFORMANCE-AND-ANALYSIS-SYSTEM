package com.vynex.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects http://localhost:8080 → dupmain.html (the splash/landing page)
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/dupmain.html";
    }
}
