package com.licence.web.controllers.register_recovery;

import com.licence.config.properties.RouteProperties;
import com.licence.config.validation.password.match.PasswordMatches;
import com.licence.web.helpers.SendRegisterRecoveryEmail;
import com.licence.web.models.User;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.WebRequest;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class RegisterController {

    private final UserService userService;
    private final RouteProperties routeProperties;
    private final MessageSource messages;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public RegisterController(UserService userRepository, RouteProperties routeProperties, @Qualifier("messageSource") MessageSource messages, ApplicationEventPublisher eventPublisher) {
        this.userService = userRepository;
        this.routeProperties = routeProperties;
        this.messages = messages;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("${route.register}")
    public String registerUser(@ModelAttribute @Valid User user,
                               BindingResult bindingResult,
                               WebRequest request,
                               Model model) {
        Map<String, ArrayList<String>> errors = getErrorsMap();

        if (bindingResult.hasErrors()) {
            errors = getFieldErrors(bindingResult, errors);
            errors = addEntityErrors(bindingResult, errors);
            errors.entrySet().stream().map(Map.Entry::getValue).filter(x -> !x.isEmpty()).forEach(System.out::println);
        } else {
            if (userService.findUserByEmail(user.getEmail()) != null) {
                errors.get("emailErrors").add("Email: already exists");
            } else if (userService.findUserByUsername(user.getUserName()) != null) {
                errors.get("userNameErrors").add("Username: already exists");
            } else {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    SendRegisterRecoveryEmail registerRecoveryEmail = new SendRegisterRecoveryEmail(user, request, eventPublisher);
                    registerRecoveryEmail.sendConfirmationEmail();
                });

                userService.registerNewUser(user);
                String messageValue = messages.getMessage("register.success.message", null, request.getLocale());
                model.addAttribute("registerSuccess", messageValue);
                System.out.println(user.getEmail() + " s-a inregistrat cu succes!");
                return "forward:" + routeProperties.getIndex();
            }
        }
        model.addAttribute("registerError", true);
        model.addAttribute("validateErrors", errors);
        model.addAttribute("userObject", user);
        return routeProperties.getIndex();
    }

    private Map<String, ArrayList<String>> addEntityErrors(BindingResult bindingResult, Map<String, ArrayList<String>> errors) {
        List<ObjectError> list = bindingResult.getAllErrors();
        for (ObjectError obj : list) {
            if (Objects.equals(obj.getCode(), PasswordMatches.class.getSimpleName())) {
                errors.get("matchingPasswordErrors").add("Password: " + obj.getDefaultMessage());
            }
        }
        return errors;
    }

    private Map<String, ArrayList<String>> getFieldErrors(BindingResult bindingResult, Map<String, ArrayList<String>> errors) {

        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        for (FieldError fieldError : fieldErrors) {
            if (Objects.equals(fieldError.getField(), "userName"))
                if (Objects.equals(fieldError.getCode(), "Pattern"))
                    errors.get("userNameErrors").add("Username: incorrect pattern. Use a valid name");
                else errors.get("userNameErrors").add("Username: " + fieldError.getDefaultMessage());
            else if (Objects.equals(fieldError.getField(), "email"))
                errors.get("emailErrors").add("Email: " + fieldError.getDefaultMessage());
            else if (Objects.equals(fieldError.getField(), "password"))
                if (Objects.equals(fieldError.getCode(), "Pattern"))
                    errors.get("passwordErrors").add("Password: incorrect pattern");
                else errors.get("passwordErrors").add("Password: " + fieldError.getDefaultMessage());
            else if (Objects.equals(fieldError.getField(), "matchingPassword"))
                errors.get("matchingPasswordErrors").add("Password: " + fieldError.getDefaultMessage());
        }
        return errors;
    }

    private Map<String, ArrayList<String>> getErrorsMap() {
        Map<String, ArrayList<String>> errors = new HashMap<>();
        errors.put("userNameErrors", new ArrayList<>());
        errors.put("emailErrors", new ArrayList<>());
        errors.put("passwordErrors", new ArrayList<>());
        errors.put("matchingPasswordErrors", new ArrayList<>());
        return errors;
    }
}
