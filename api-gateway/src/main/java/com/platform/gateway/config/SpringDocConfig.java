package com.platform.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

@Configuration
public class SpringDocConfig {

    @Bean
    public GroupedOpenApi partnerApi() {
        return GroupedOpenApi.builder()
                .group("partner-service")
                .pathsToMatch("/partners/**")
                .build();
    }

    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("order-service")
                .pathsToMatch("/orders/**")
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> swaggerUiRouter() {
        return RouterFunctions.route(
                RequestPredicates.GET("/swagger-ui.html"),
                request -> ServerResponse
                        .temporaryRedirect(URI.create("/webjars/swagger-ui/index.html"))
                        .build()
        );
    }
}