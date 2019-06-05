package com.licence.web.controllers.register_recovery;

import com.licence.config.properties.RouteProperties;
import com.licence.web.helpers.SendRegisterRecoveryEmail;
import com.licence.web.models.User;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class RecoveryController {

    private final UserService userService;
    private final MessageSource messageSource;
    private final RouteProperties routeProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public RecoveryController(UserService userService, MessageSource messageSource, RouteProperties routeProperties, ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.messageSource = messageSource;
        this.routeProperties = routeProperties;
        this.eventPublisher = eventPublisher;
    }

    @RequestMapping("${route.recovery}")
    public String recoveryAccount(WebRequest request, Model model) {

        Locale locale = request.getLocale();
        String email = request.getParameter("email");
        String userName = request.getParameter("userName");
        String messageValue;
        User user = getUser(email, userName);
        if (user != null) {
            if (user.isEnabled()) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    SendRegisterRecoveryEmail registerRecoveryEmail = new SendRegisterRecoveryEmail(user, request, eventPublisher);
                    registerRecoveryEmail.sendRecoveryEmail();
                });
                messageValue = messageSource.getMessage("recovery.reset.success.message", null, locale);
                model.addAttribute("recoveryResetSuccess", messageValue);
            } else {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    SendRegisterRecoveryEmail registerRecoveryEmail = new SendRegisterRecoveryEmail(user, request, eventPublisher);
                    registerRecoveryEmail.sendConfirmationEmail();
                });
                messageValue = messageSource.getMessage("recovery.resend.success.message", null, locale);
                model.addAttribute("recoveryResendSuccess", messageValue);
                return "forward:" + routeProperties.getIndex();
            }
        } else {
            messageValue = messageSource.getMessage("recovery.error.notFound.message", null, locale);
            model.addAttribute("recoveryError", messageValue);
        }
        return "forward:" + routeProperties.getIndex();
    }

    private User getUser(String email, String userName) {
        User user;
        if (!Objects.equals(email, "")) {
            user = userService.findUserByEmail(email);
        } else if(!Objects.equals(userName, "")) {
            user = userService.findUserByUsername(userName);
        } else {
            user = null;
        }
        return user;
    }

}
