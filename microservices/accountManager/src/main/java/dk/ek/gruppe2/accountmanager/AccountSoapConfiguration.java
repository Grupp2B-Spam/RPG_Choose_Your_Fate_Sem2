package dk.ek.gruppe2.accountmanager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.util.List;

@Configuration(proxyBeanMethods = false)
class AccountSoapConfiguration implements WsConfigurer {
    static final String NAMESPACE = "urn:choose-your-fate:account:v1";
    private static final ClassPathResource SCHEMA = new ClassPathResource("contracts/account-v1.xsd");
    private final PayloadValidatingInterceptor payloadValidator;

    AccountSoapConfiguration(PayloadValidatingInterceptor payloadValidator) {
        this.payloadValidator = payloadValidator;
    }

    @Bean(name = "account-v1")
    DefaultWsdl11Definition accountWsdl(XsdSchema accountSchema) {
        var wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("AccountManager");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace(NAMESPACE);
        wsdl.setSchema(accountSchema);
        return wsdl;
    }

    @Bean
    XsdSchema accountSchema() {
        return new SimpleXsdSchema(SCHEMA);
    }

    @Bean
    static PayloadValidatingInterceptor accountPayloadValidator() {
        var validator = new PayloadValidatingInterceptor();
        validator.setSchema(SCHEMA);
        validator.setValidateRequest(true);
        validator.setValidateResponse(true);
        return validator;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        interceptors.add(payloadValidator);
    }
}
