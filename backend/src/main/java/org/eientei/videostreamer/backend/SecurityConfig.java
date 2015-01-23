package org.eientei.videostreamer.backend;

import org.eientei.videostreamer.backend.security.AppUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-20
 * Time: 12:17
 */
@EnableWebMvcSecurity
@EnableWebSecurity
@ComponentScan(basePackages = "org.eientei.videostreamer.backend.security")
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private AppUserDetailsService appUserDetailsService;

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;

    @Autowired
    private RememberMeServices rememberMeServices;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(appUserDetailsService).passwordEncoder(new Md5PasswordEncoder())
                .and().authenticationProvider(new RememberMeAuthenticationProvider("rememberMe"));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().userDetailsService(appUserDetailsService)
                .anonymous().principal(appUserDetailsService.loadUserByUsername("Anonymous")).key("Anonymous")
                .and().rememberMe().rememberMeServices(rememberMeServices).tokenRepository(persistentTokenRepository).tokenValiditySeconds(60*60*24*180);
    }

    @Autowired
    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }


    @Bean
    public RememberMeServices rememberMeServices() {
        PersistentTokenBasedRememberMeServices tokenBasedRememberMeServices = new PersistentTokenBasedRememberMeServices("rememberMe", appUserDetailsService, persistentTokenRepository);
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
