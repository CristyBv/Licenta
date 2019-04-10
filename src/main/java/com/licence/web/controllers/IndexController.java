package com.licence.web.controllers;

import com.licence.config.properties.RouteProperties;
import com.licence.web.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping(value = {"/", "${route.index}"})
public class IndexController {

    private final RouteProperties routeProperties;

    @Autowired
    public IndexController(RouteProperties routeProperties) {
        this.routeProperties = routeProperties;
    }

    @RequestMapping
    public String index(Model model) throws IOException {
        model.addAttribute("userObject", new User());
        return routeProperties.getIndex();
    }
}
