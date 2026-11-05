package com.finanzapp.lifehub.modules.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class FinanceWebConfiguration implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/user", handlerType ->
                handlerType.getPackageName().startsWith("com.finanzapp.user.controller"));
        configurer.addPathPrefix("/movement", handlerType ->
                handlerType.getPackageName().startsWith("com.finanzapp.movements.controller"));
        configurer.addPathPrefix("/tag", handlerType ->
                handlerType.getPackageName().startsWith("com.finanzapp.tags.controller"));
    }
}
