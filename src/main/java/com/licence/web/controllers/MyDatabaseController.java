package com.licence.web.controllers;

import com.licence.config.properties.RouteProperties;
import com.licence.config.security.CassandraUserDetails;
import com.licence.config.validation.password.match.PasswordMatches;
import com.licence.web.models.Keyspace;
import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.User;
import com.licence.web.services.KeyspaceService;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
public class MyDatabaseController {

    private final RouteProperties routeProperties;
    private final KeyspaceService keyspaceService;
    private final UserService userService;
    private final MessageSource messages;

    @Autowired
    public MyDatabaseController(RouteProperties routeProperties, KeyspaceService keyspaceService, UserService userService,@Qualifier("messageSource") MessageSource messages) {
        this.routeProperties = routeProperties;
        this.keyspaceService = keyspaceService;
        this.userService = userService;
        this.messages = messages;
    }

    @RequestMapping(value = "${route.myDatabase}")
    public String myDatabase(Model model) {
        model.addAttribute("keyspaceObject", new Keyspace());
        return routeProperties.getMyDatabase();
    }

    @PostMapping(value = "${route.createKeyspace}")
    public String createKeyspace(@ModelAttribute @Valid Keyspace keyspace,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 WebRequest request,
                                 Model model) {
        Map<String, ArrayList<String>> errors = getErrorsMap();
        if (bindingResult.hasErrors()) {
            errors = getErrors(bindingResult, errors);
            errors.entrySet().stream().map(Map.Entry::getValue).filter(x -> !x.isEmpty()).forEach(System.out::println);
        } else {
            CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();
            if (keyspaceService.findKeyspaceByName(user.getUserName() + "_" + keyspace.getName()) != null) {
                errors.get("nameErrors").add("Name: already exists!");
            } else {
                String name = keyspace.getName();
                keyspace.setName(user.getUserName() + "_" + name);
                keyspaceService.save(keyspace, true);
                keyspace.setName(name);
                UserKeyspace userKeyspace = UserKeyspace.builder()
                        .keyspace(keyspace)
                        .name(name)
                        .access(0)
                        .active(true)
                        .creatorName(user.getUserName())
                        .build();
                user.getKeyspaces().add(userKeyspace);
                userDetails.getKeyspaces().get(user.getUserName()).add(userKeyspace);
                userService.save(user);
                String messageValue = messages.getMessage("database.keyspaces.create.success", null, request.getLocale());
                model.addAttribute("createKeyspaceSuccess", messageValue);
                System.out.println(keyspace.getName() + " - keyspace created!");
                return "forward:" + routeProperties.getMyDatabase();
            }
        }
        model.addAttribute("createKeyspaceError", true);
        model.addAttribute("validateErrors", errors);
        model.addAttribute("keyspaceObject", keyspace);
        return routeProperties.getMyDatabase();
    }

    private Map<String, ArrayList<String>> getErrors(BindingResult bindingResult, Map<String, ArrayList<String>> errors) {
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        for (FieldError fieldError : fieldErrors) {
            if (Objects.equals(fieldError.getField(), "name"))
                if (Objects.equals(fieldError.getCode(), "Pattern"))
                    errors.get("nameErrors").add("Name: incorrect pattern. Use a valid name");
                else errors.get("nameErrors").add("Name: " + fieldError.getDefaultMessage());
            else if (Objects.equals(fieldError.getField(), "password"))
                errors.get("passwordErrors").add("Password: incorrect pattern");
            else if (Objects.equals(fieldError.getField(), "matchingPassword"))
                errors.get("matchingPasswordErrors").add("Password: " + fieldError.getDefaultMessage());
        }
        List<ObjectError> list = bindingResult.getAllErrors();
        for (ObjectError obj : list) {
            if (Objects.equals(obj.getCode(), PasswordMatches.class.getSimpleName())) {
                errors.get("matchingPasswordErrors").add("Password: " + obj.getDefaultMessage());
            }
        }
        return errors;
    }

    private Map<String, ArrayList<String>> getErrorsMap() {
        Map<String, ArrayList<String>> errors = new HashMap<>();
        errors.put("nameErrors", new ArrayList<>());
        errors.put("passwordErrors", new ArrayList<>());
        errors.put("matchingPasswordErrors", new ArrayList<>());
        return errors;
    }
}
