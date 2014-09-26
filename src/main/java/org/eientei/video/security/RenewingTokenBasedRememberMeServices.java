package org.eientei.video.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-15
 * Time: 11:55
 */
public class RenewingTokenBasedRememberMeServices extends PersistentTokenBasedRememberMeServices {
    private SessionExpirer sessionExpirer;

    public RenewingTokenBasedRememberMeServices(SessionExpirer sessionExpirer, String rememberMe, AppUserDetailsService appUserDetailsService, PersistentTokenRepository repository) {
        super(rememberMe, appUserDetailsService, repository);
        this.sessionExpirer = sessionExpirer;
    }

    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request, HttpServletResponse response) {
        UserDetails userDetails = super.processAutoLoginCookie(cookieTokens, request, response);
        onLoginSuccess(request, response, createSuccessfulAuthentication(request, userDetails));
        sessionExpirer.expireSession(request);
        return userDetails;
    }
}
