package org.eientei.videostreamer.config;

import org.eientei.videostreamer.config.security.VideostreamerCsrfFilter;
import org.eientei.videostreamer.config.security.VideostreamerSaveSessionFilter;
import org.eientei.videostreamer.config.security.VideostreamerUser;
import org.eientei.videostreamer.controller.Chat;
import org.eientei.videostreamer.orm.error.AlreadyExists;
import org.eientei.videostreamer.orm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
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
    private Md5PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private Chat chat;

    private UserDetails anonymous;

    public WebSecurityConfiguration() {
        super(true);
    }

    @PostConstruct
    public void postConstruct() throws AlreadyExists {
        try {
            anonymous = userDetailsService.loadUserByUsername("anonymous");
        } catch (UsernameNotFoundException e) {
            try {
                userService.createUser("anonymous", null, null, false);
                anonymous = userDetailsService.loadUserByUsername("anonymous");
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
        http
                .userDetailsService(userDetailsService)
                .csrf().requireCsrfProtectionMatcher(protectionMatcher()).csrfTokenRepository(csrfTokenRepository()).and()
                .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint()).and()
                .headers().and()
                .sessionManagement().and()
                .securityContext().and()
                .requestCache().and()
                .anonymous().principal(anonymous).and()
                .servletApi().and()
                .authorizeRequests().anyRequest().permitAll().and()
                .formLogin().failureHandler(failureHandler()).successHandler(loginSuccessHandler()).loginProcessingUrl("/security/login").passwordParameter("password").usernameParameter("username").and()
                .logout().logoutSuccessHandler(logoutSuccessHandler()).logoutUrl("/security/logout").deleteCookies("rememberme").and()
                .rememberMe().key("key").rememberMeServices(rememberMeServices("key")).and()
                .addFilter(new WebAsyncManagerIntegrationFilter())
                .addFilterAfter(new VideostreamerCsrfFilter(), CsrfFilter.class)
                .addFilterBefore(new VideostreamerSaveSessionFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new VideostreamerSaveSessionFilter(), LogoutFilter.class);
    }

    private RequestMatcher protectionMatcher() {
        return new RequestMatcher() {
            @Override
            public boolean matches(HttpServletRequest request) {
                List<String> strings = Arrays.asList("GET", "HEAD", "OPTIONS", "TRACE");
                if (strings.contains(request.getMethod())) {
                    return false;
                }
                return !request.getRequestURI().startsWith("/nginx");
            }
        };
    }

    private LogoutSuccessHandler logoutSuccessHandler() {
        return new LogoutSuccessHandler() {
            @Override
            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                chat.refreshSession((String) request.getAttribute("oldsessionid"), request.getSession().getId(), (VideostreamerUser) anonymous);
            }
        };
    }

    @Bean
    public Md5PasswordEncoder passwordEncoder() {
        return new Md5PasswordEncoder();
    }

    private AuthenticationFailureHandler failureHandler() {
        return new SimpleUrlAuthenticationFailureHandler();
    }

    private AuthenticationSuccessHandler loginSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                super.onAuthenticationSuccess(request, response, authentication);
                chat.refreshSession((String) request.getAttribute("oldsessionid"), request.getSession().getId(), (VideostreamerUser) authentication.getPrincipal());
            }

        };
        successHandler.setRedirectStrategy(new RedirectStrategy() {
            @Override
            public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
                // empty
            }
        });
        return successHandler;
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
