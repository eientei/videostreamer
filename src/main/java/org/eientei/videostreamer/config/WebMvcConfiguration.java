package org.eientei.videostreamer.config;

import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-03
 * Time: 12:31
 */
@Configuration
public class WebMvcConfiguration extends WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.setOrder(3);
        registry.addViewController("/**").setViewName("/static/index.html");
        registry.addViewController("/favicon.ico").setViewName("/static/favicon.ico");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(2);
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }
}
