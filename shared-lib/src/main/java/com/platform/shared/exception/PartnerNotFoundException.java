package com.platform.shared.exception;

public class PartnerNotFoundException extends RuntimeException {

    private final String partnerId;

    public PartnerNotFoundException(String partnerId) {
        super("Partner not found: " + partnerId);
        this.partnerId = partnerId;
    }

    public String getPartnerId() {
        return partnerId;
    }
}