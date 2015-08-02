package org.eientei.video.backend.config;

import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-28
 * Time: 13:10
 */
@Configuration
public class WebMvcConfiguration extends WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(2);
        registry.addResourceHandler("/**").addResourceLocations("classpath:/META-INF/resources/");
    }
}
