package org.eientei.video.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.video.backend.dto.UserDTO;
import org.eientei.video.backend.dto.Util;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-30
 * Time: 09:07
 */
public class VideoLogoutSuccessHandler implements LogoutSuccessHandler {
    private ObjectMapper objectMapper = new ObjectMapper();

    private VideostreamerUser anonymous;
    private CsrfTokenRepository csrfTokenRepository;

    public VideoLogoutSuccessHandler(VideostreamerUser anonymous, CsrfTokenRepository csrfTokenRepository) {
        this.anonymous = anonymous;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);
        request.setAttribute(CsrfToken.class.getName(), csrfToken);
        request.setAttribute(csrfToken.getParameterName(), csrfToken);
        Util.setupCsrfCookie(request, response);

        UserDTO dto = new UserDTO(anonymous.getUsername(), Util.determineHash(anonymous, request), anonymous.getEntity().getEmail());
        objectMapper.writeValue(response.getWriter(), dto);
    }
}
