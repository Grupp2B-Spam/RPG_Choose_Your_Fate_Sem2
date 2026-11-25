package dk.ek.gruppe2.accountmanager;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static dk.ek.gruppe2.accountmanager.AccountSoapConfiguration.NAMESPACE;

@Endpoint
public class VerificationKeyEndpoint {
    private final VerificationKey key;

    VerificationKeyEndpoint(VerificationKey key) {
        this.key = key;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "GetVerificationKeyRequest")
    @ResponsePayload
    public Element getVerificationKey() throws ParserConfigurationException {
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        var response = document.createElementNS(NAMESPACE, "GetVerificationKeyResponse");
        document.appendChild(response);
        append(response, "issuer", key.issuer());
        append(response, "audience", key.audience());
        append(response, "algorithm", "RS256");
        append(response, "publicKey", key.publicKeyBase64());
        return response;
    }

    private static void append(Element parent, String name, String value) {
        var element = parent.getOwnerDocument().createElementNS(NAMESPACE, name);
        element.setTextContent(value);
        parent.appendChild(element);
    }
}
