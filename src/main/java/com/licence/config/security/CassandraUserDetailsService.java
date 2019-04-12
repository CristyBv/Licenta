package com.licence.config.security;

import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.User;
import com.licence.web.services.KeyspaceService;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CassandraUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) {

        if (email.isEmpty()) {
            System.out.println("LoadUser for log-in failed (email is empty)!");
            throw new UsernameNotFoundException("User's email is empty!");
        }
        System.out.println("Tring to loadUser (" + email + ") for log-in!");
        User user = userService.findUserByEmail(email);
        // if user was not found, returns UsernameNotFoundException
        // if user is not enabled, returns InternalAuthenticationServiceException
        // both erros are managed in CustomAuthenticationFailureHandler
        if (user == null) {
            System.out.println("LoadUser (" + email + ") for log-in failed (user not found)!");
            throw new UsernameNotFoundException("User's bad credentials!");
        } else if (!user.isEnabled()) {
            System.out.println("LoadUser (" + email + ") for log-in failed (user not enabled)!");
            throw new InternalAuthenticationServiceException("User not enabled!");
        }
        return new CassandraUserDetails(user);
    }
}
