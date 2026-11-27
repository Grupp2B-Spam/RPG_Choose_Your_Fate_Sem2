package dk.ek.gruppe2.ttsmanager;

import java.io.IOException;
import java.util.Set;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Explicit offline local trust, compatible with RecapManager's existing LocalToken helper. */
@Configuration
@Profile("local")
public class LocalSecurityConfiguration {
    @Bean JwtSecurity localSecurity(Environment env, ResourceLoader resources) throws IOException {
        try (var key = resources.getResource(TtsConfiguration.required(env, "tts.local-public-key")).getInputStream()) {
            var decoder = NimbusJwtDecoder.withPublicKey(RsaKeyConverters.x509().convert(key))
                    .signatureAlgorithm(SignatureAlgorithm.RS256).build();
            decoder.setJwtValidator(TtsConfiguration.validators(TtsConfiguration.required(env, "tts.local-issuer"),
                    TtsConfiguration.required(env, "tts.local-audience")));
            return new JwtSecurity(decoder, decoder, Set.of("local-event-delivery"), "", "USER", "ADMIN", true);
        }
    }
}
