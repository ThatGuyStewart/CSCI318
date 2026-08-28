package com.retail.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.retail.common.ServiceUriBuilder;
import com.retail.notification.config.CustomerServiceProperties;

class CustomerClientTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(CustomerServicePropertiesConfiguration.class);

    @Test
    void normalizesLeadingAndTrailingSlashes() {
        String uri = ServiceUriBuilder.resourceUri("http://customer-service:8081/", "///customer///",
            "customer.service.url", "customer.service.path");

        assertThat(uri).isEqualTo("http://customer-service:8081/customer");
    }

    @Test
    void rejectsEmptyCustomerPath() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ServiceUriBuilder.resourceUri("http://customer-service:8081", "/",
                    "customer.service.url", "customer.service.path"))
                .withMessage("customer.service.path must contain a resource path");
    }

        @Test
        void rejectsBlankServiceUrl() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ServiceUriBuilder.resourceUri(" ", "/customer/",
                "customer.service.url", "customer.service.path"))
            .withMessage("customer.service.url must not be blank");
        }

        @Test
        void rejectsBlankCustomerPath() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ServiceUriBuilder.resourceUri("http://customer-service:8081", " ",
                "customer.service.url", "customer.service.path"))
            .withMessage("customer.service.path must not be blank");
        }

        @Test
        void rejectsServiceUrlWithQuery() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ServiceUriBuilder.resourceBaseUri("http://customer-service:8081?version=1",
                            "/customer/", "customer.service.url", "customer.service.path"))
                    .withMessage("customer.service.url must not contain a query or fragment");
        }

        @Test
        void rejectsServiceUrlWithFragment() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ServiceUriBuilder.resourceBaseUri("http://customer-service:8081#customers",
                            "/customer/", "customer.service.url", "customer.service.path"))
                    .withMessage("customer.service.url must not contain a query or fragment");
        }

    @Test
    void rejectsMissingCustomerPath() {
        assertThatNullPointerException()
                .isThrownBy(() -> ServiceUriBuilder.resourceUri("http://customer-service:8081", null,
                    "customer.service.url", "customer.service.path"))
                .withMessage("customer.service.path must not be null");
    }

    @Test
    void bindsDefaultCustomerPath() {
        contextRunner.run(context -> {
            CustomerServiceProperties properties = context.getBean(CustomerServiceProperties.class);

            assertThat(properties.getPath()).isEqualTo("/customer/");
        });
    }

    @Test
    void rejectsBlankConfiguredCustomerPath() {
        contextRunner.withPropertyValues("customer.service.path= ").run(context ->
                assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CustomerServiceProperties.class)
    static class CustomerServicePropertiesConfiguration {
    }
}