package com.newbie.core.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "newbie.storage")
@Data
public class StorageConfig {
    private String bucket = "upload";
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String service;
    public boolean isLocalStorage() {
        return this.getAccessKey().isEmpty() || this.getEndpoint().isEmpty() || this.getSecretKey().isEmpty();
    }
}
