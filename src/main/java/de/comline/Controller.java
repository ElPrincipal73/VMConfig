package de.comline;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.GetMapping;

import java.util.logging.Logger;

@org.springframework.stereotype.Controller
public class Controller {
    @Value("${db.url}")
    private String DB_URL;
    @Value("${db.user}")
    private String USER;
    @Value("${db.pass}")
    private String PASS;



    @GetMapping("/index")
    public String index() {
        return "index";
    }


    @GetMapping("/show_Hilfe")
    public String showHelp() {
        return "show_Hilfe";
    }
}
