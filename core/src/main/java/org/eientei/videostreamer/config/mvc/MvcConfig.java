package org.eientei.videostreamer.config.mvc;

import org.eientei.videostreamer.config.VideostreamerProperties;
import org.eientei.videostreamer.html5.Html5WebsocketHandler;
import org.eientei.videostreamer.rtmp.RtmpServerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Created by Alexander Tumin on 2016-10-02
 */
@Configuration
@EnableWebMvc
@EnableWebSocket
public class MvcConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {
    private final RtmpServerContext rtmpContext;
    private final VideostreamerProperties properties;

    @Autowired
    public MvcConfig(RtmpServerContext rtmpContext, VideostreamerProperties properties) {
        this.rtmpContext = rtmpContext;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(websocketHandler(), "/live").setAllowedOrigins("*");
    }

    @Bean
    public Html5WebsocketHandler websocketHandler() {
        return new Html5WebsocketHandler(rtmpContext);
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
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.setOrder(2);
        registry.addViewController("/**").setViewName("index");
    }

    @Bean
    public ViewResolver viewResolver() {
        return new InternalResourceViewResolver("/", ".html");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
}
