package org.eientei.video.config;

import org.eientei.video.security.AppUserDetailsService;
import org.eientei.video.security.RenewingTokenBasedRememberMeServices;
import org.eientei.video.security.SessionExpirer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-14
 * Time: 19:38
 */
@EnableWebSecurity
@Configuration
@ComponentScan(basePackages = "org.eientei.video.security")
public class SecurityAppConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private AppUserDetailsService appUserDetailsService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;

    @Autowired
    private RememberMeServices rememberMeServices;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(appUserDetailsService).passwordEncoder(new Md5PasswordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .userDetailsService(appUserDetailsService)
                .exceptionHandling()
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                    .and()
                .anonymous()
                    .principal(appUserDetailsService.loadUserByUsername("Anonymous"))
                    .and()
                .rememberMe()
                    .userDetailsService(appUserDetailsService)
                    .rememberMeServices(rememberMeServices)
                    .tokenRepository(persistentTokenRepository)
                    .key("rememberMe")
                    .and()
                .logout()
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true);
    }

    @Bean
    public SessionExpirer sessionExpirer() {
        SessionExpirer expirer = new SessionExpirer();
        expirer.setInterval(60 * 10);
        return expirer;
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }

    @Autowired
    @Bean
    public RememberMeServices rememberMeServices(SessionExpirer sessionExpirer) {
        PersistentTokenBasedRememberMeServices tokenBasedRememberMeServices = new RenewingTokenBasedRememberMeServices(sessionExpirer, "rememberMe", appUserDetailsService, persistentTokenRepository);
        tokenBasedRememberMeServices.setAlwaysRemember(true);
        tokenBasedRememberMeServices.setCookieName("rememberMe");
        tokenBasedRememberMeServices.setTokenValiditySeconds(60 * 60 * 24 * 180);
        return tokenBasedRememberMeServices;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }
}
