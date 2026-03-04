package com.platform.partner.config;

import com.platform.shared.audit.AuditRepository;
import com.platform.shared.audit.AuditService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Configuration
public class AuditConfig {

    @Bean
    public AuditRepository auditRepository(
            DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        return new AuditRepository(dynamoDbEnhancedClient);
    }

    @Bean
    public AuditService auditService(AuditRepository auditRepository) {
        return new AuditService(auditRepository, "partner-service");
    }
}