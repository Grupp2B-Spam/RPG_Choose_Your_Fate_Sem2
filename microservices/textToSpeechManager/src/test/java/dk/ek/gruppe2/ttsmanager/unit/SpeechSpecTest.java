package dk.ek.gruppe2.ttsmanager.unit;

import dk.ek.gruppe2.ttsmanager.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SpeechSpecTest {
    private static final String VOICE = "JBFqnCBsd6RMkjVDRZzb";
    private static final String MODEL = "eleven_turbo_v2_5";
    private static final String FORMAT = "mp3_44100_128";

    @Test void unicodeIdentityUsesExactUtf8ByteLengths() {
        // Independent SHA-256 vector of the four-byte UTF-8 byte lengths and exact values.
        assertThat(SpeechSpec.hash("Your path \uD83C\uDF33", VOICE, MODEL, FORMAT))
                .isEqualTo("d350c2fb338764f05c777205fd7c12bba8c923abda437bd5552807f9a7e549bf");
    }

    @Test void textAndEverySynthesisSettingParticipateInTheTtsCacheIdentity() {
        String hash = SpeechSpec.hash("journey", VOICE, MODEL, FORMAT);
        assertThat(hash).hasSize(64).isEqualTo(SpeechSpec.hash("journey", VOICE, MODEL, FORMAT));
        assertThat(hash).isNotEqualTo(SpeechSpec.hash("new journey", VOICE, MODEL, FORMAT))
                .isNotEqualTo(SpeechSpec.hash("journey", "other voice", MODEL, FORMAT))
                .isNotEqualTo(SpeechSpec.hash("journey", VOICE, "new model", FORMAT))
                .isNotEqualTo(SpeechSpec.hash("journey", VOICE, MODEL, "mp3_22050_32"));
        assertThat(SpeechSpec.hash("ab", "c", "m", "o"))
                .isNotEqualTo(SpeechSpec.hash("a", "bc", "m", "o"));
    }

    @Test void requestTextIsCombinedWithTheServicesConfiguredSettings() {
        var policy = new SpeechPolicy(100, 5000, VOICE, MODEL, FORMAT);
        assertThat(policy.specification("Journey"))
                .isEqualTo(new SpeechSpec(SpeechSpec.hash("Journey", VOICE, MODEL, FORMAT), "Journey", VOICE, MODEL, FORMAT));
    }

    @Test void oversizedNarrativesAreRejectedBeforePaidGeneration() {
        String text = "Too much text";
        var policy = new SpeechPolicy(3, 5000, VOICE, MODEL, FORMAT);
        assertThatIllegalArgumentException().isThrownBy(() -> policy.specification(text));
        assertThatIllegalArgumentException().isThrownBy(() -> policy.specification(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> policy.specification(null));
    }
}
