package com.platform.shared.exception;

public class PartnerRevokedException extends RuntimeException {

    private final String partnerId;

    public PartnerRevokedException(String partnerId) {
        super("Partner is revoked: " + partnerId);
        this.partnerId = partnerId;
    }

    public String getPartnerId() {
        return partnerId;
    }
}