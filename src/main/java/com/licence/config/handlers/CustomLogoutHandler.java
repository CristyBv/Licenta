package com.licence.config.handlers;

import com.licence.config.properties.RouteProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

public class CustomLogoutHandler extends SimpleUrlLogoutSuccessHandler implements LogoutSuccessHandler {

    @Autowired
    private RouteProperties routeProperties;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String refererUrl = request.getHeader("Referer");
        System.out.println("Logout from: " + refererUrl);
        Cookie[] cookies = request.getCookies();
        for(Cookie cookie:cookies) {
            if(Objects.equals(cookie.getName(), "JSESSIONID"))
                cookie.setMaxAge(0);
        }
        try {
            response.sendRedirect(routeProperties.getIndex() + "?logout");
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onLogoutSuccess(request, response, authentication);
    }
}
