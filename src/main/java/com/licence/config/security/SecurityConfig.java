package com.licence.config.security;

import com.licence.config.handlers.CustomAuthenticationFailureHandler;
import com.licence.config.handlers.CustomLogoutHandler;
import com.licence.config.properties.RouteProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final CustomAuthenticationFailureHandler authFailureHandler;
    private final RouteProperties routeProperties;

    @Autowired
    public SecurityConfig(CustomAuthenticationFailureHandler authFailureHandler, RouteProperties routeProperties) {
        this.authFailureHandler = authFailureHandler;
        this.routeProperties = routeProperties;
    }

    @Bean
    public UserDetailsService cassandraUserDetails() {
        return new CassandraUserDetailsService();
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new CustomLogoutHandler();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception {
        UserDetailsService userDetailsService = cassandraUserDetails();
        auth.userDetailsService(userDetailsService);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
       //http.csrf().disable();
        http.
                authorizeRequests()
                // auth not request for those
                    .antMatchers("/",
                            routeProperties.getIndex(),
                            routeProperties.getRegister(),
                            routeProperties.getConfirmation(),
                            routeProperties.getRecovery())
                    .permitAll()
                // for any other request, must be auth
                    .anyRequest().authenticated()
                    .and()
                .formLogin()
                    .loginPage(routeProperties.getIndex() + "?login")
                    .loginProcessingUrl(routeProperties.getLogin())
                    .failureUrl(routeProperties.getIndex())
                    .failureHandler(authFailureHandler)
                    .usernameParameter("email")
                    .passwordParameter("password")
                    .permitAll()
                    .and()
                .logout()
                    .logoutRequestMatcher(new AntPathRequestMatcher(routeProperties.getLogout()))
                    .logoutSuccessUrl(routeProperties.getIndex() + "?logout")
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(logoutSuccessHandler())
                    .and()
                .rememberMe()
                    .key("rememberMeKey")
                    .tokenValiditySeconds(86400)
                    .rememberMeParameter("remember-me");
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web
                .ignoring()
                .antMatchers("/resources/**", "/static/**", "/css/**", "/js/**", "/images/**", "/webjars/**");
    }

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
}
