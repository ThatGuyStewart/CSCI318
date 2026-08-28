package com.retail.common;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public final class ServiceUriBuilder {

    private ServiceUriBuilder() {
    }

    @NonNull
    public static String resourceBaseUri(String serviceUrl, String resourcePath,
                                        String serviceUrlProperty, String resourcePathProperty) {
        return resourceUri(serviceUrl, resourcePath, serviceUrlProperty, resourcePathProperty) + "/";
    }

    @NonNull
    public static String resourceUri(String serviceUrl, String resourcePath,
                                     String serviceUrlProperty, String resourcePathProperty) {
        String configuredServiceUrlProperty = Objects.requireNonNull(serviceUrlProperty,
            "serviceUrlProperty must not be null");
        String configuredResourcePathProperty = Objects.requireNonNull(resourcePathProperty,
            "resourcePathProperty must not be null");
        String configuredServiceUrl = Objects.requireNonNull(serviceUrl, configuredServiceUrlProperty + " must not be null");
        String configuredResourcePath = Objects.requireNonNull(resourcePath,
            configuredResourcePathProperty + " must not be null");
        if (!StringUtils.hasText(configuredServiceUrl)) {
            throw new IllegalArgumentException(configuredServiceUrlProperty + " must not be blank");
        }
        if (!StringUtils.hasText(configuredResourcePath)) {
            throw new IllegalArgumentException(configuredResourcePathProperty + " must not be blank");
        }
        UriComponentsBuilder serviceUriBuilder = UriComponentsBuilder.fromUriString(configuredServiceUrl);
        UriComponents serviceUriComponents = serviceUriBuilder.build();
        if (serviceUriComponents.getQuery() != null || serviceUriComponents.getFragment() != null) {
            throw new IllegalArgumentException(configuredServiceUrlProperty + " must not contain a query or fragment");
        }
        String[] pathSegments = StringUtils.tokenizeToStringArray(configuredResourcePath, "/");
        if (pathSegments.length == 0) {
            throw new IllegalArgumentException(configuredResourcePathProperty + " must contain a resource path");
        }
        return serviceUriBuilder
                .pathSegment(pathSegments)
                .build()
                .toUriString();
    }
}