package org.eientei.videostreamer.conf;

import org.eientei.videostreamer.mp4.Mp4Server;
import org.eientei.videostreamer.ws.WebsocketLiveHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
@Configuration
@EnableWebMvc
@EnableWebSocket
public class WebMvcConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {
    @Autowired
    private Mp4Server mp4Server;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveHandler(), "/live").setAllowedOrigins("*");
    }

    @Bean
    public WebsocketLiveHandler liveHandler() {
        return new WebsocketLiveHandler(mp4Server);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(-2);
        registry.addResourceHandler("/index.html").addResourceLocations("classpath:app/index.html");
        registry.addResourceHandler("/js/broadway/**").addResourceLocations("classpath:Broadway/Player/");
        registry.addResourceHandler("/js/app/**").addResourceLocations("classpath:app/js/");
        registry.addResourceHandler("/static/app/**").addResourceLocations("classpath:app/static/");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}
