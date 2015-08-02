package org.eientei.video.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eientei.video.backend.dto.UserDTO;
import org.eientei.video.backend.dto.Util;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-29
 * Time: 21:58
 */
public class VideoAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Util.setupCsrfCookie(request, response);

        VideostreamerUser user = (VideostreamerUser) authentication.getPrincipal();
        UserDTO dto = new UserDTO(user.getUsername(), Util.determineHash(user, request), user.getEntity().getEmail());
        objectMapper.writeValue(response.getWriter(), dto);
    }
}
