package com.licence.config.handlers;

import com.licence.config.context.ApplicationContextProvider;
import com.licence.web.models.User;
import com.licence.web.services.UserService;
import jnr.ffi.annotations.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Objects;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String badCredentialsUrl = "/index?badCredentialsLoginError";
    private final String disabledUserUrl = "/index?disabledUserLoginError";

    private final UserService userService;

    @Autowired
    public CustomAuthenticationFailureHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        super.setRedirectStrategy(redirectStrategy);
    }


    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // if the exception is
        if(exception.getClass() == InternalAuthenticationServiceException.class)
            setDefaultFailureUrl(disabledUserUrl);
        else setDefaultFailureUrl(badCredentialsUrl);
        setUseForward(false);
        setRedirectStrategy(new DefaultRedirectStrategy());
        super.onAuthenticationFailure(request, response, exception);
    }
}
