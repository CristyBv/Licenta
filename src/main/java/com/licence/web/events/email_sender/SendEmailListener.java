package com.licence.web.events.email_sender;

import com.licence.config.properties.RouteProperties;
import com.licence.web.helpers.RandomString;
import com.licence.web.models.User;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSource;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Component
public class SendEmailListener implements ApplicationListener<OnSendEmailEvent> {

    @Value("${register.mail.token.expiry_hours}")
    private String EXPIRATION;
    @Value("${app.domain.name}")
    private String DOMAIN;

    private final UserService userService;
    private final MessageSource messages;
    private final JavaMailSender mailSender;
    private final RouteProperties routeProperties;

    @Autowired
    public SendEmailListener(UserService userService, @Qualifier("messageSource") MessageSource messages, JavaMailSender mailSender, RouteProperties routeProperties) {
        this.userService = userService;
        this.messages = messages;
        this.mailSender = mailSender;
        this.routeProperties = routeProperties;
    }

    @Override
    public void onApplicationEvent(@Nullable OnSendEmailEvent event) {
        if (Objects.equals(event.getType(), "confirmation")) {
            System.out.println("ApplicationEvent (OnSendEmailEvent) Confirmation");
            this.confirmationEvent(event);
        } else if (Objects.equals(event.getType(), "recovery")) {
            System.out.println("ApplicationEvent (OnSendEmailEvent) Recovery");
            this.recoveryEvent(event);
        } else if (Objects.equals(event.getType(), "change-email")) {
            this.changeEmailEvent(event);
        }
    }

    private void confirmationEvent(OnSendEmailEvent event) {
        User user = event.getUser();
        String token = UUID.randomUUID().toString();
        user.setToken(token);
        user.setExpiryDate(calculateExpiryDate(60 * Integer.parseInt(EXPIRATION)));
        userService.save(user);

        String recipientAddress = user.getEmail();
        String subject = messages.getMessage("sendMail.register.confirmation.subject", null, event.getLocale());
        String confirmationUrl = event.getAppUrl() + routeProperties.getConfirmation() + "?token=" + token;
        String message = messages.getMessage("sendMail.register.confirmation.content", null, event.getLocale());

        System.out.println("RegistrationConfirm Mail is sending...");
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message + " : " + DOMAIN + confirmationUrl);
        mailSender.send(email);
    }

    private void recoveryEvent(OnSendEmailEvent event) {
        User user = event.getUser();
        RandomString randomString = new RandomString(20);
        String newPassword = UUID.randomUUID().toString();
        user.setPassword(newPassword);
        userService.save(user);

        String recipientAddress = user.getEmail();
        String subject = messages.getMessage("sendMail.recovery.resetPassword.subject", null, event.getLocale());
        String message = messages.getMessage("sendMail.recovery.resetPassword.content", null, event.getLocale());

        System.out.println("Recovery Mail is sending...");
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message + " : " + newPassword);
        mailSender.send(email);
    }

    private void changeEmailEvent(OnSendEmailEvent event) {
        User user = event.getUser();
        RandomString randomString = new RandomString(10);
        String recipientAddress = user.getEmailToken();
        String token = user.getEmailToken() + randomString.nextString();
        user.setEmailToken(token);
        userService.save(user);

        String subject = messages.getMessage("sendMail.change-email.subject", null, event.getLocale());
        String confirmationUrl = event.getAppUrl() + routeProperties.getChange().get("email") + "?token=" + token;
        String message = messages.getMessage("sendMail.change-email.content", null, event.getLocale());

        System.out.println("Change Email is sending...");
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(message + " : " + DOMAIN + confirmationUrl);
        mailSender.send(email);
    }

    private Date calculateExpiryDate(int expiryTimeInMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Timestamp(calendar.getTime().getTime()));
        calendar.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(calendar.getTime().getTime());
    }
}
