package com.licence.web.controllers;

import com.licence.config.properties.RouteProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "${route.mydatabase}")
public class MyDatabaseController {

    private final RouteProperties routeProperties;

    @Autowired
    public MyDatabaseController(RouteProperties routeProperties) {
        this.routeProperties = routeProperties;
    }

    @GetMapping
    public String myDatabase(Model model) {
        return routeProperties.getMyDatabase();
    }
}
