package org.eientei.video.backend.config.security;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-11
 * Time: 10:58
 */
public class VideostreamerSaveSessionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute("oldsessionid", request.getSession().getId());
        filterChain.doFilter(request, response);
    }
}
