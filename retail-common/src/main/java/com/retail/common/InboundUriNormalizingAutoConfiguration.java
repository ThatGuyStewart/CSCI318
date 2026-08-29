package com.retail.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InboundUriNormalizingAutoConfiguration {

    @Bean
    @SuppressWarnings("unused")
    FilterRegistrationBean<InboundUriNormalizingFilter> inboundUriNormalizingFilter() {
        FilterRegistrationBean<InboundUriNormalizingFilter> registration = new FilterRegistrationBean<>(
                new InboundUriNormalizingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}