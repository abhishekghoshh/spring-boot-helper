package com.ag.in.rest.client.providers;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;

@Service
public class CertificateSignatureVerifier {

    @Value("${rest-template.strategy.certificate-chain:false}")
    private String validateCertificateChainStrategy;

    public boolean verify(X509Certificate[] certificates, String authType) {
        if (StringUtils.isEmpty(validateCertificateChainStrategy) || "false".equalsIgnoreCase(validateCertificateChainStrategy))
            return true;

        return true;
    }

}
