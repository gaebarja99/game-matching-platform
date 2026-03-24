package com.gamematcher.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.portone")
public class PaymentProperties {

    /** REST API key for server-side verification. */
    private String apiKey = "";

    /** REST API secret for server-side verification. */
    private String apiSecret = "";

    /** Legacy PortOne imp code used by IMP.init on the client. */
    private String impCode = "";

    /** Existing client identifier field kept for backward compatibility. */
    private String storeId = "";

    /** Default PG code. */
    private String pg = "html5_inicis.INIpayTest";

    /** Default pay method. */
    private String payMethod = "card";

    public String getClientInitKey() {
        if (impCode != null && !impCode.isBlank()) {
            return impCode;
        }
        return storeId != null ? storeId : "";
    }
}
