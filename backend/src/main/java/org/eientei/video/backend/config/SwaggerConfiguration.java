package org.eientei.video.backend.config;

import com.fasterxml.classmate.TypeResolver;
import org.eientei.video.backend.config.security.VideostreamerUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * User: iamtakingiteasy
 * Date: 2015-07-28
 * Time: 01:16
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {
    @Autowired
    private TypeResolver typeResolver;

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.eientei.video.backend.controller"))
                .paths(PathSelectors.any())
                .build().ignoredParameterTypes(VideostreamerUser.class);
    }
}
