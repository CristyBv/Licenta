package com.licence.web.controllers.register_recovery;

import com.licence.config.properties.RouteProperties;
import com.licence.web.models.User;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

import java.util.Calendar;
import java.util.Locale;

@Controller
public class ConfirmationController {

    private final UserService userService;
    private final MessageSource messages;
    private final RouteProperties routeProperties;

    @Autowired
    public ConfirmationController(UserService userService, @Qualifier("messageSource") MessageSource messages, RouteProperties routeProperties) {
        this.userService = userService;
        this.messages = messages;
        this.routeProperties = routeProperties;
    }

    @RequestMapping(value = "${route.confirmation}", method = RequestMethod.GET)
    public String confirmRegistration(WebRequest request,
                                      Model model,
                                      @RequestParam("token") String token) {

        Locale locale = request.getLocale();
        User user = userService.findUserByToken(token);
        if (user == null) {
            System.out.println("RegistrationConfirm failed (user = null)!");
            String messageValue = messages.getMessage("register.confirmation.invalidToken.message", null, locale);
            model.addAttribute("registerConfirmationError", messageValue);
            return "forward:" + routeProperties.getIndex();
        }

        Calendar calendar = Calendar.getInstance();
        if ((user.getExpiryDate().getTime() - calendar.getTime().getTime()) <= 0) {
            System.out.println("RegistrationConfirm failed (token expired)!");
            String messageValue = messages.getMessage("register.confirmation.tokenExpired.message", null, locale);
            model.addAttribute("registerConfirmationError", messageValue);
            return "forward:" + routeProperties.getIndex();
        }
        userService.enableUser(user);
        String messageValue = messages.getMessage("register.confirmation.success.message", null, locale);
        model.addAttribute("registerConfirmationSuccess", messageValue);
        return "forward:" + routeProperties.getIndex();
    }
}
