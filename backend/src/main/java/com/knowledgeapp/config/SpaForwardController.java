package com.knowledgeapp.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the bundled SPA for client-side routes; /api is handled by REST controllers. */
@Controller
public class SpaForwardController {

    @GetMapping({"/knowledge", "/knowledge/", "/knowledge/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
