package org.eientei.videostreamer.conf;


import org.eientei.videostreamer.impl.core.GlobalContext;
import org.eientei.videostreamer.impl.ws.ChannelingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Created by Alexander Tumin on 2016-10-23
 */
@Configuration
@EnableWebMvc
@EnableWebSocket
@EnableAsync
public class WebMvcConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {
    private final GlobalContext globalContext;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(-1);
        configurer.setTaskExecutor(asyncTaskExecutor());
    }

    @Bean
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new SimpleAsyncTaskExecutor("async");
    }

    @Autowired
    public WebMvcConfig(GlobalContext globalContext) {
        this.globalContext = globalContext;
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
    public WebSocketHandler commHandler(GlobalContext globalContext) {
        return new ChannelingWebSocketHandler(globalContext);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(commHandler(globalContext), "/comm").setAllowedOrigins("*");
    }
}
