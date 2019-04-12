package com.licence.config.security;

import com.licence.web.models.UDT.UserKeyspace;
import com.licence.web.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// Create my own User class for spring security context
public class CassandraUserDetails implements UserDetails {

    private User user;
    private String email;
    private String password;
    private List<GrantedAuthority> grantedAuthorities;

    CassandraUserDetails(User user) {
        this.user = user;
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.grantedAuthorities = AuthorityUtils.createAuthorityList(user.getRoles().toArray(new String[user.getRoles().size()]));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
