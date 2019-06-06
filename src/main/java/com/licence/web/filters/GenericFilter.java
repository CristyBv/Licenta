package com.licence.web.filters;

import com.licence.config.security.CassandraUserDetails;
import com.licence.web.models.User;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@Order(1)
public class GenericFilter implements Filter {

    private final UserService userService;

    @Autowired
    public GenericFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // if the user is authenticated
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            User authUser = ((CassandraUserDetails) authentication.getPrincipal()).getUser();
            User dbUser = userService.findById(authUser.getId());
            authUser.setNotifications(dbUser.getNotifications());
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    // other methods
}