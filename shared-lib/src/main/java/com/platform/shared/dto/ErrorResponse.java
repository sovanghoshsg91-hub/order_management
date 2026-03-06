package com.platform.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String errorCode;
    private String message;
    private String correlationId;
    private Map<String, Object> details;

    @Builder
    public ErrorResponse(String errorCode,
                         String message,
                         String correlationId,
                         Map<String, Object> details) {
        this.errorCode     = errorCode;
        this.message       = message;
        this.correlationId = correlationId;
        // defensive copy — fixes EI_EXPOSE_REP2 ✅
        this.details = details != null
                ? Collections.unmodifiableMap(new HashMap<>(details))
                : null;
    }

    // defensive copy on getter — fixes EI_EXPOSE_REP ✅
    public Map<String, Object> getDetails() {
        return details != null
                ? Collections.unmodifiableMap(details)
                : null;
    }

    // setter with defensive copy — fixes EI_EXPOSE_REP2 ✅
    public void setDetails(Map<String, Object> details) {
        this.details = details != null
                ? Collections.unmodifiableMap(new HashMap<>(details))
                : null;
    }
}