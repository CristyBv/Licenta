package com.licence.web.controllers;

import com.licence.config.properties.RouteProperties;
import com.licence.config.security.CassandraUserDetails;
import com.licence.config.validation.email.EmailValidator;
import com.licence.config.validation.password.pattern.PasswordPatternValidator;
import com.licence.web.helpers.SendRegisterRecoveryEmail;
import com.licence.web.models.UDT.UserNotification;
import com.licence.web.models.User;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messageSource;
    private final RouteProperties routeProperties;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    @Value("${user.avatar.maxLength}")
    private String avatarLength;

    @Autowired
    public ProfileController(ApplicationEventPublisher eventPublisher, MessageSource messageSource, RouteProperties routeProperties, UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.eventPublisher = eventPublisher;
        this.messageSource = messageSource;
        this.routeProperties = routeProperties;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @RequestMapping("${route.profile}")
    public String profile(Authentication authentication,
                          Model model) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        if (user.getNotifications() != null && !user.getNotifications().isEmpty()) {
            List<UserNotification> userNotifications = user.getNotifications().stream().sorted((p1, p2) -> p2.getDate().compareTo(p1.getDate())).collect(Collectors.toList());
            Boolean[] unread = {false};
            userNotifications.forEach(p -> {
                if (!p.isRead())
                    unread[0] = true;
            });
            model.addAttribute("notifications", userNotifications);
            if (unread[0])
                model.addAttribute("unreadNotifications", true);
        }
        return routeProperties.getProfile();
    }

    @ResponseBody
    @PostMapping(value = "${route.change[notification-read]}")
    public String changeNotificationRead(@RequestBody Map<String, String> data,
                                         Authentication authentication,
                                         WebRequest request) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        if (user.getNotifications() != null) {
            // search for the notification with the same date
            UserNotification notification = null;
            for (UserNotification userNotification : user.getNotifications()) {
                if (userNotification.getDate().toString().equals(data.get("notificationDate")))
                    notification = userNotification;
            }
            if (notification != null) {
                notification.setRead(true);
                userService.save(user);
                return JSONObject.quote("success");
            }
        }
        return JSONObject.quote("error");
    }

    @ResponseBody
    @PostMapping(value = "${route.change[avatar]}", produces = "application/json")
    public String changeAvatar(@RequestBody Map<String, String> avatar,
                               Authentication authentication,
                               WebRequest request) {
        Integer maxLength = Integer.parseInt(avatarLength);
        String url = avatar.get("avatar");
        if (url.length() == 0) {
            return JSONObject.quote(messageSource.getMessage("changeAvatar.error.empty", null, request.getLocale()));
        } else if (avatar.get("avatar").length() <= maxLength) {
            User user = ((CassandraUserDetails) authentication.getPrincipal()).getUser();
            user.setAvatar(avatar.get("avatar"));
            userService.save(user);
            return JSONObject.quote(messageSource.getMessage("changeAvatar.success", null, request.getLocale()));
        }
        return JSONObject.quote(messageSource.getMessage("changeAvatar.error.length", null, request.getLocale()));
    }


    @PostMapping(value = "${route.change[email]}")
    public String changeEmail(@RequestParam String email,
                              WebRequest request,
                              Authentication authentication,
                              Model model) {
        EmailValidator emailValidator = new EmailValidator();
        Locale locale = request.getLocale();
        String messageValue;
        if (emailValidator.isValid(email, null)) {
            if (userService.findUserByEmail(email) == null) {
                CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
                User user = userDetails.getUser();
                user.setEmailToken(email);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    SendRegisterRecoveryEmail registerRecoveryEmail = new SendRegisterRecoveryEmail(user, request, eventPublisher);
                    registerRecoveryEmail.sendChangeEmail();
                });
                messageValue = messageSource.getMessage("changeEmail.request.success", null, locale);
                model.addAttribute("changeEmailSuccess", messageValue);
            } else {
                messageValue = messageSource.getMessage("changeEmail.request.alreadyExists", null, locale);
                model.addAttribute("changeEmailError", messageValue);
            }
        } else {
            messageValue = messageSource.getMessage("changeEmail.request.invalid", null, locale);
            model.addAttribute("changeEmailError", messageValue);
        }
        return "forward:" + routeProperties.getProfile();
    }

    @GetMapping(value = "${route.change[email]}")
    public String changeEmailConfirmation(@RequestParam String token,
                                          Authentication authentication,
                                          HttpServletResponse response,
                                          HttpServletRequest request,
                                          Model model) throws IOException, ServletException {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        String messageValue;
        String email = token.substring(0, token.length() - 10);
        String code = token.substring(token.length() - 10, token.length());
        if (Objects.equals(user.getEmailToken(), token) && !Objects.equals(user.getEmail(), email)) {
            user.setEmail(email);
            user.setEmailToken(code);
            userService.save(user);
            messageValue = messageSource.getMessage("changeEmail.confirm.success", null, request.getLocale());
            model.addAttribute("changeEmailSuccess", messageValue);
        } else {
            messageValue = messageSource.getMessage("changeEmail.confirm.badToken", null, request.getLocale());
            model.addAttribute("changeEmailError", messageValue);
        }
        return "forward:" + routeProperties.getProfile();
    }

    @PostMapping("${route.change[password]}")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String matchingPassword,
                                 Authentication authentication,
                                 WebRequest request,
                                 Model model) {
        CassandraUserDetails userDetails = (CassandraUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
        String messageValue;
        Locale locale = request.getLocale();
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            if (Objects.equals(newPassword, matchingPassword)) {
                PasswordPatternValidator patternValidator = new PasswordPatternValidator();
                if (patternValidator.isValid(newPassword, null)) {
                    user.setPassword(newPassword);
                    userService.save(user);
                    messageValue = messageSource.getMessage("changePassword.success", null, locale);
                    model.addAttribute("changePasswordSuccess", messageValue);
                } else {
                    messageValue = messageSource.getMessage("changePassword.error.badPattern", null, locale);
                    model.addAttribute("changePasswordError", messageValue);
                }
            } else {
                messageValue = messageSource.getMessage("changePassword.error.notMatch", null, locale);
                model.addAttribute("changePasswordError", messageValue);
            }
        } else {
            messageValue = messageSource.getMessage("changePassword.error.invalidOldPassword", null, locale);
            model.addAttribute("changePasswordError", messageValue);
        }
        return "forward:" + routeProperties.getProfile();
    }
}
