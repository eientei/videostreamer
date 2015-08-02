package org.eientei.video.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Map;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-30
 * Time: 08:18
 */
public class VideoLoginTranformFilter extends OncePerRequestFilter {
    private RequestMatcher requestMatcher;
    private ObjectMapper objectMapper = new ObjectMapper();

    public VideoLoginTranformFilter(String url) {
        this.requestMatcher = new AntPathRequestMatcher(url, "POST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (requestMatcher.matches(request)) {
            String data = CharStreams.toString(request.getReader());
            final Map<?,?> map = objectMapper.readValue(data, Map.class);
            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getParameter(String name) {
                    Object obj = map.get(name);
                    if (obj == null) {
                        return super.getParameter(name);
                    }
                    return obj.toString();
                }
            };
        }
        filterChain.doFilter(request, response);
    }
}
