package com.finanzapp.lifehub.modules.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class FinanceInternalClientsConfiguration {

    private RestClient buildClient(String baseUrl) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(3000);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean(name = "usersRestClient")
    RestClient usersRestClient(@Value("${lifehub.internal-base-url}") String baseUrl) {
        return buildClient(baseUrl);
    }

    @Bean(name = "tagsRestClient")
    RestClient tagsRestClient(@Value("${lifehub.internal-base-url}") String baseUrl) {
        return buildClient(baseUrl);
    }

    @Bean(name = "movementsRestClient")
    RestClient movementsRestClient(@Value("${lifehub.internal-base-url}") String baseUrl) {
        return buildClient(baseUrl);
    }
}
