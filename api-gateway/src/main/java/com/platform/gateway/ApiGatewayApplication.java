package com.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
        return factory -> factory.addServerCustomizers(
                httpServer -> httpServer.httpRequestDecoder(
                        spec -> spec.maxHeaderSize(1048576)
                                .maxInitialLineLength(1048576)
                )
        );
    }
}