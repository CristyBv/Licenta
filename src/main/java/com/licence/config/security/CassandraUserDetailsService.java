package com.licence.config.security;

import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.UDT.UserNotification;
import com.licence.web.models.User;
import com.licence.web.services.KeyspaceService;
import com.licence.web.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${user.notification.max}")
    private Integer notificationMax;

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
        // verify how many notification the user has
        // if he has more than the maximum, we delete it
        if(user.getNotifications() != null && user.getNotifications().size() != 0) {
            List<UserNotification> userNotifications = user.getNotifications().stream().sorted((p1, p2) -> p2.getDate().compareTo(p1.getDate())).collect(Collectors.toList());
            List<UserNotification> newUserNotifications = new ArrayList<>();
            // put the unread notifications first
            userNotifications.forEach(p -> {
                if(!p.isRead())
                    newUserNotifications.add(p);
            });
            // if we store the read notifications too
//        if(newUserNotifications.size() < notificationMax) {
//            for (UserNotification userNotification : userNotifications) {
//                newUserNotifications.add(userNotification);
//                if (newUserNotifications.size() >= notificationMax)
//                    break;
//            }
//        }
            user.setNotifications(newUserNotifications);
            userService.save(user);
        }
        return new CassandraUserDetails(user);
    }
}
