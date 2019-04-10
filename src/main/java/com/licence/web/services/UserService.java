package com.licence.web.services;

import com.licence.config.properties.QueryProperties;
import com.licence.web.models.User;
import com.licence.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Service
public class UserService {

    private final CassandraAdminOperations adminOperations;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private final QueryProperties queryProperties;

    @Autowired
    public UserService(BCryptPasswordEncoder bCryptPasswordEncoder, @Qualifier("operations") CassandraAdminOperations adminOperations, QueryProperties queryProperties) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.adminOperations = adminOperations;
        this.queryProperties = queryProperties;
    }

    public User findUserByEmail(String email) {
        String query = String.format(queryProperties.getSelectUser().get("by_email"), "*", email);
        return adminOperations.selectOne(query, User.class);
    }

    public User findUserByUsername(String userName) {
        String query = String.format(queryProperties.getSelectUser().get("by_username"), "*", userName);
        return adminOperations.selectOne(query, User.class);
    }

    public User findUserByToken(String token) {
        String query = String.format(queryProperties.getSelectUser().get("by_register_token"), "*", token);
        return adminOperations.selectOne(query, User.class);
    }

    public void save(User user) {
        if(user.getId() == null)
            user.setId(UUID.randomUUID().toString());
        if(user.getPassword().length() <= 40)
            user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        if(user.getRegisterDate() == null)
            user.setRegisterDate(Calendar.getInstance().getTime());
        adminOperations.insert(user);
    }

    public void registerNewAdmin(User user) {
        user.setRoles(Arrays.asList("USER", "ADMIN"));
        save(user);
    }
    public void registerNewUser(User user) {
        user.setRoles(Collections.singletonList("USER"));
        save(user);
    }

    public void enableUser(User user) {
        user.setEnabled(true);
        user.setExpiryDate(Calendar.getInstance().getTime());
        save(user);
    }
}

