package org.eientei.videostreamer.conf;


import org.eientei.videostreamer.server.ServerContext;
import org.eientei.videostreamer.ws.WebsocketCommHandler;
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
    private final ServerContext serverContext;

    @Autowired
    public WebMvcConfig(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(-2);
        registry.addResourceHandler("/js/lib/**").addResourceLocations("classpath:META-INF/resources/webjars/");
        registry.addResourceHandler("/index.html").addResourceLocations("classpath:app/index.html");
        registry.addResourceHandler("/app/**").addResourceLocations("classpath:app/");
        registry.addResourceHandler("/static/app/**").addResourceLocations("classpath:app/static/");
    }

    @Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }

    @Bean
    public WebsocketCommHandler commHandler(ServerContext serverContext) {
        return new WebsocketCommHandler(serverContext);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(commHandler(serverContext), "/comm").setAllowedOrigins("*");
    }
}
