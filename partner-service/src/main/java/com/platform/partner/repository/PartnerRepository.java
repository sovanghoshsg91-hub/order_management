package com.platform.partner.repository;

import com.platform.partner.entity.Partner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PartnerRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<Partner> table() {
        return dynamoDbEnhancedClient.table("Partners", TableSchema.fromBean(Partner.class));
    }

    public Partner save(Partner partner) {
        log.info("Saving partner: {}", partner.getPartnerId());
        table().putItem(partner);
        return partner;
    }

    public Optional<Partner> findById(String partnerId) {
        log.info("Finding partner: {}", partnerId);
        Partner partner = table().getItem(
                Key.builder().partitionValue(partnerId).build()
        );
        return Optional.ofNullable(partner);
    }

    public Partner update(Partner partner) {
        log.info("Updating partner: {}", partner.getPartnerId());
        table().updateItem(partner);
        return partner;
    }
}