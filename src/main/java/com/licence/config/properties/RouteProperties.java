package com.licence.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "route")
public class RouteProperties {
    private String index;
    private String confirmation;
    private String recovery;
    private String register;
    private String login;
    private String logout;
    private String profile;
    private String changeEmail;
    private String changePassword;
    private String changeAvatar;
    private String myDatabase;
}
