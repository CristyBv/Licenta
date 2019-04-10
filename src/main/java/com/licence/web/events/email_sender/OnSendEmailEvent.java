package com.licence.web.events.email_sender;

import com.licence.web.models.User;
import org.springframework.context.ApplicationEvent;

import java.util.Locale;

public class OnSendEmailEvent extends ApplicationEvent {

    private String appUrl;
    private Locale locale;
    private User user;
    private String type;

    public OnSendEmailEvent(User user, Locale locale, String appUrl, String type) {
        super(user);
        this.user = user;
        this.locale = locale;
        this.appUrl = appUrl;
        this.type = type;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
