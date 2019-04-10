package com.licence.web.helpers;

import com.licence.web.events.email_sender.OnSendEmailEvent;
import com.licence.web.models.User;
import lombok.Data;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.context.request.WebRequest;

@Data
public class SendRegisterRecoveryEmail {
    private User user;
    private WebRequest request;
    private ApplicationEventPublisher eventPublisher;

    public SendRegisterRecoveryEmail(User user, WebRequest request, ApplicationEventPublisher eventPublisher) {
        this.user = user;
        this.request = request;
        this.eventPublisher = eventPublisher;
    }

    public boolean sendConfirmationEmail() {
        try {
            System.out.println("Email event is setting up...");
            String appUrl = request.getContextPath();
            eventPublisher.publishEvent(new OnSendEmailEvent(user, request.getLocale(), appUrl, "confirmation"));
        } catch (Exception error) {
            error.printStackTrace();
            System.out.println("RegistrationCompleteEvent failed!");
            return false;
        }
        return true;
    }

    public boolean sendRecoveryEmail() {
        try{
            System.out.println("Email recovery event is setting up...");
            String appUrl = request.getContextPath();
            eventPublisher.publishEvent(new OnSendEmailEvent(user, request.getLocale(), appUrl, "recovery"));
        } catch (Exception error) {
            System.out.println("RecoveryEmail failed!");
            return false;
        }
        return true;
    }

    public boolean sendChangeEmail() {
        try{
            System.out.println("Change Email event is setting up...");
            String appUrl = request.getContextPath();
            eventPublisher.publishEvent(new OnSendEmailEvent(user, request.getLocale(), appUrl, "change-email"));
        } catch (Exception error) {
            System.out.println("ChangeEmail failed!");
            return false;
        }
        return true;
    }
}
