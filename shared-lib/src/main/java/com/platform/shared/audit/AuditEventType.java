package com.platform.shared.audit;

public class AuditEventType {

    // Partner events
    public static final String PARTNER_CREATED  = "PARTNER_CREATED";
    public static final String PARTNER_REVOKED  = "PARTNER_REVOKED";
    public static final String PARTNER_ACCESSED = "PARTNER_ACCESSED";

    // Order events
    public static final String ORDER_CREATED    = "ORDER_CREATED";
    public static final String ORDER_ACCESSED   = "ORDER_ACCESSED";
    public static final String ORDER_LISTED     = "ORDER_LISTED";
    public static final String ORDER_DUPLICATE  = "ORDER_DUPLICATE";
    public static final String ORDER_REJECTED   = "ORDER_REJECTED";

    // Fulfilment events
    public static final String FULFILMENT_COMPLETED = "FULFILMENT_COMPLETED";
    public static final String FULFILMENT_SKIPPED   = "FULFILMENT_SKIPPED";

    // Security events
    public static final String PARTNER_REVOKED_BLOCKED = "PARTNER_REVOKED_BLOCKED";
    public static final String IDEMPOTENCY_CONFLICT    = "IDEMPOTENCY_CONFLICT";

    private AuditEventType() {}
}