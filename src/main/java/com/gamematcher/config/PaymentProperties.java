package com.gamematcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 포트원(아임포트) 결제 설정.
 * 포트원 콘솔에서 REST API 키·시크릿 발급 후 설정.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.portone")
public class PaymentProperties {
    /** REST API Key (서버 결제 검증용) */
    private String apiKey = "";
    /** REST API Secret (서버 결제 검증용) */
    private String apiSecret = "";
    /** 가맹점 식별자 (프론트 IMP.init용. 포트원 콘솔에서 확인) */
    private String storeId = "";
    private String customerCode = "";
    private String channelKey = "";
    private String signKey = "";
    /** PG사 설정값 (예: html5_inicis.INIpayTest, kakaopay.TC0ONETIME 등) */
    private String pg = "html5_inicis.INIpayTest";
    /** 결제수단 (card, trans, vbank, phone 등) */
    private String payMethod = "card";
}
