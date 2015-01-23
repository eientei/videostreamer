package org.eientei.videostreamer.backend;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.eientei.videostreamer.backend.controllers.ChatController;
import org.eientei.videostreamer.backend.controllers.ConfigBootstrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-20
 * Time: 12:19
 */
@Configuration
@EnableWebSocket
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan(basePackages = "org.eientei.videostreamer.backend.controllers", includeFilters = @ComponentScan.Filter(Controller.class))
@EnableWebMvc
public class DispatcherConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {
    @Autowired
    private ChatController chatController;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(chatController, "/chat").addInterceptors(new HttpSessionHandshakeInterceptor(), new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
                attributes.put("httpSession", httpServletRequest.getSession().getId());
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                    attributes.put("principal", authentication.getPrincipal());
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

            }
        }).withSockJS();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        converters.add(jacksonConverter);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    }

    @Autowired
    @Bean
    public MailSender mailSender(ConfigBootstrap apiConfigBootstrap) {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(apiConfigBootstrap.getVideoMailHost());
        javaMailSender.setPassword(apiConfigBootstrap.getVideoMailPassword());
        return javaMailSender;
    }

    @Autowired
    @Bean
    public SimpleMailMessage simpleMailMessage(ConfigBootstrap apiConfigBootstrap) {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(apiConfigBootstrap.getVideoMailFrom());
        simpleMailMessage.setSubject("Password reset");
        return simpleMailMessage;
    }
}
