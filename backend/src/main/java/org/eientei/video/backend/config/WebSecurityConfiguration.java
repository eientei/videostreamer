package org.eientei.video.backend.config;

import org.eientei.video.backend.config.security.*;
import org.eientei.video.backend.controller.Chat;
import org.eientei.video.backend.orm.error.AlreadyExists;
import org.eientei.video.backend.orm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-03
 * Time: 12:30
 */
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Chat chat;

    @Value("${videostreamer.smtpHost:}")
    private String smtpHost;

    private VideostreamerUser anonymous;

    public WebSecurityConfiguration() {
        super(true);
    }

    @PostConstruct
    public void postConstruct() throws AlreadyExists {
        try {
            anonymous = (VideostreamerUser) userDetailsService.loadUserByUsername("anonymous");
        } catch (UsernameNotFoundException e) {
            try {
                userService.createUser("anonymous", null, null, false);
                anonymous = (VideostreamerUser) userDetailsService.loadUserByUsername("anonymous");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CsrfTokenRepository csrfTokenRepository = csrfTokenRepository();
        http
                .userDetailsService(userDetailsService)
                .csrf().requireCsrfProtectionMatcher(protectionMatcher()).csrfTokenRepository(csrfTokenRepository).and()
                .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint()).and()
                .headers().and()
                .sessionManagement().and()
                .securityContext().and()
                .requestCache().and()
                .anonymous().principal(anonymous).and()
                .servletApi().and()
                .authorizeRequests().anyRequest().permitAll().and()
                .formLogin().failureHandler(failureHandler()).successHandler(successHandler()).loginProcessingUrl("/security/login").passwordParameter("password").usernameParameter("username").and()
                .logout().logoutUrl("/security/logout").logoutSuccessHandler(logoutSuccessHandler(csrfTokenRepository)).deleteCookies("rememberme").and()
                .rememberMe().key("key").rememberMeServices(rememberMeServices("key")).and()
                .addFilter(new WebAsyncManagerIntegrationFilter())
                .addFilterBefore(new VideoLoginTranformFilter("/security/login"), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new VideostreamerCsrfFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new VideostreamerSaveSessionFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new VideostreamerSaveSessionFilter(), LogoutFilter.class);
    }

    private RequestMatcher protectionMatcher() {
        return new RequestMatcher() {
            @Override
            public boolean matches(HttpServletRequest request) {
                List<String> strings = Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE");
                return !strings.contains(request.getMethod()) && !request.getRequestURI().contains("/control/") && !request.getRequestURI().contains("/chat/");
            }
        };
    }

    @Bean
    public JavaMailSender javaMailService() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(smtpHost);
        return javaMailSender;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        final Md5PasswordEncoder md5PasswordEncoder = new Md5PasswordEncoder();
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return md5PasswordEncoder.encodePassword(rawPassword.toString(),  null);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encode(rawPassword).equals(encodedPassword);
            }
        };
    }

    private LogoutSuccessHandler logoutSuccessHandler(CsrfTokenRepository csrfTokenRepository) {
        return new VideoLogoutSuccessHandler(anonymous, csrfTokenRepository) {
            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                chat.refreshSession((String) request.getAttribute("oldsessionid"), request.getSession().getId(), anonymous);
                super.onLogoutSuccess(request, response, authentication);
            }
        };
    }

    private AuthenticationFailureHandler failureHandler() {
        return new VideoAuthenticationFailureHandler();
    }

    private AuthenticationSuccessHandler successHandler() {
        return new VideoAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                chat.refreshSession((String) request.getAttribute("oldsessionid"), request.getSession().getId(), (VideostreamerUser) authentication.getPrincipal());
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

    private RememberMeServices rememberMeServices(String key) {
        PersistentTokenBasedRememberMeServices rememberMeServices = new PersistentTokenBasedRememberMeServices(key, userDetailsService, persistentTokenRepository);
        rememberMeServices.setParameter("rememberme");
        rememberMeServices.setCookieName("rememberme");
        rememberMeServices.setAlwaysRemember(true);
        rememberMeServices.setTokenValiditySeconds(60 * 60 * 24 * 365);
        return rememberMeServices;
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return new Http403ForbiddenEntryPoint();
    }

    private CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }
}
