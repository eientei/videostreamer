package org.eientei.videostreamer.frontend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-20
 * Time: 10:42
 */
@Configuration
@EnableWebMvc
public class DispatcherConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.setOrder(-1);
        registry.addViewController("/**").setViewName("/site/index.html");
        registry.addViewController("/favicon.ico").setViewName("/site/favicon.ico");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(-2);
        registry.addResourceHandler("/view/**").addResourceLocations("/view/");
        registry.addResourceHandler("/js/**").addResourceLocations("/js/");
        registry.addResourceHandler("/css/**").addResourceLocations("/css/");
        registry.addResourceHandler("/swf/**").addResourceLocations("/swf/");
        registry.addResourceHandler("/site/**").addResourceLocations("/site/");
        registry.addResourceHandler("/img/**").addResourceLocations("/img/");
    }
}
