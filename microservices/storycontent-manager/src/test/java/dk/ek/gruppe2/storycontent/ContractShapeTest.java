package dk.ek.gruppe2.storycontent;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.yaml.snakeyaml.Yaml;

import dk.ek.gruppe2.storycontent.messaging.EventEnvelope;
import dk.ek.gruppe2.storycontent.messaging.payload.ChoicePayload;
import dk.ek.gruppe2.storycontent.messaging.payload.EffectPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.ItemDefinitionPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.RaceDefinitionPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.RequirementPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.ScenePayload;
import tools.jackson.databind.ObjectMapper;

/**
 * Checks that what this service serialises matches the AsyncAPI document in
 * microservices/contracts/.
 *
 * The contract is what other people are writing consumers against. The usual way it breaks
 * is silent: a field is renamed in a record, the code still compiles, and the mismatch is
 * found by whoever is debugging a consumer that reads null.
 *
 * Field names are compared both ways, so it fails whether a field is missing from the
 * contract or from the code. It uses the ObjectMapper Spring configures, not one built
 * here, so the inclusion and date settings are covered too.
 *
 * A unit test: it needs no broker and no database, so it runs in every build.
 */
@JsonTest
@DisplayName("Published payloads match the AsyncAPI contract")
class ContractShapeTest {

    private static final Path CONTRACT =
            Path.of("..", "contracts", "storycontent-manager.asyncapi.yaml");

    private static Map<String, Object> schemas;

    @Autowired
    private ObjectMapper json;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void loadContract() throws Exception {
        assertThat(CONTRACT)
                .as("the published contract must be readable from the service, since this "
                        + "test is what keeps the two in step")
                .exists();
        try (InputStream in = Files.newInputStream(CONTRACT)) {
            Map<String, Object> document = new Yaml().load(in);
            Map<String, Object> components = (Map<String, Object>) document.get("components");
            schemas = (Map<String, Object>) components.get("schemas");
        }
    }

    @Test
    void scenePayloadHasExactlyTheFieldsTheContractDeclares() {
        assertThat(fieldsOf(fullScenePayload()))
                .isEqualTo(propertiesOf("ScenePayload"));
    }

    @Test
    void choicePayloadHasExactlyTheFieldsTheContractDeclares() {
        assertThat(fieldsOf(fullChoicePayload()))
                .isEqualTo(propertiesOf("Choice"));
    }

    @Test
    void requirementPayloadHasExactlyTheFieldsTheContractDeclares() {
        RequirementPayload requirement = new RequirementPayload("ITEM", "a-flag", 1);
        assertThat(fieldsOf(requirement)).isEqualTo(propertiesOf("ChoiceRequirement"));
    }

    @Test
    void effectPayloadHasExactlyTheFieldsTheContractDeclares() {
        EffectPayload effect = new EffectPayload("MODIFY_STAT", 1, 2, "charisma", -1, "a-flag");
        assertThat(fieldsOf(effect)).isEqualTo(propertiesOf("ChoiceEffect"));
    }

    @Test
    void itemPayloadHasExactlyTheFieldsTheContractDeclares() {
        ItemDefinitionPayload item = new ItemDefinitionPayload(
                1, 1, "ACTIVE", "EQUIPMENT", "MAINHAND",
                Map.of("fashion", 2), Map.of("charisma", 2));
        assertThat(fieldsOf(item)).isEqualTo(propertiesOf("ItemDefinitionPayload"));
    }

    @Test
    void racePayloadHasExactlyTheFieldsTheContractDeclares() {
        RaceDefinitionPayload race = new RaceDefinitionPayload(1, 1, "ACTIVE", "smalls", 4, 4);
        assertThat(fieldsOf(race)).isEqualTo(propertiesOf("RaceDefinitionPayload"));
    }

    @Test
    @DisplayName("the envelope matches, and occurredAt is an ISO-8601 string, not a number")
    void envelopeMatchesTheContract() {
        EventEnvelope<ScenePayload> envelope =
                EventEnvelope.of("scene-content.updated", fullScenePayload());

        Map<String, Object> serialised = asMap(envelope);

        assertThat(serialised.keySet()).isEqualTo(propertiesOf("Envelope"));

        // The contract says date-time, and this is a default that could change under us on a
        // Jackson or Boot upgrade without anyone editing a line of our code.
        assertThat(serialised.get("occurredAt"))
                .as("occurredAt must be an ISO-8601 instant, not an epoch number")
                .isInstanceOf(String.class);
        assertThat((String) serialised.get("occurredAt")).endsWith("Z");
    }

    @Test
    @DisplayName("fields that do not belong to an effect's kind are absent, not null")
    void unusedFieldsAreOmittedRatherThanSerialisedAsNull() {
        EffectPayload grantFlag = new EffectPayload("GRANT_FLAG", null, null, null, null, "bruised");

        assertThat(fieldsOf(grantFlag)).containsExactlyInAnyOrder("kind", "flag");
        assertThat(json.writeValueAsString(grantFlag)).doesNotContain("null");
    }

    @Test
    @DisplayName("statistic names go on the wire in lower case, as 1.0.0 already published them")
    @SuppressWarnings("unchecked")
    void statisticNamesAreLowerCase() {
        List<String> allowed = (List<String>) ((Map<String, Object>)
                ((Map<String, Object>) ((Map<String, Object>) schemas.get("ChoiceEffect"))
                        .get("properties")).get("stat")).get("enum");

        assertThat(allowed).containsExactlyInAnyOrder("intelligence", "charisma", "fashion");

        // The same vocabulary is the key set of an item's requirements and modifiers, which
        // was published in 1.0.0 and cannot be respelled without breaking consumers.
        assertThat(allowedKeysOf("ItemDefinitionPayload", "requirements"))
                .containsExactlyInAnyOrderElementsOf(allowed);
        assertThat(allowedKeysOf("ItemDefinitionPayload", "modifiers"))
                .containsExactlyInAnyOrderElementsOf(allowed);
    }

    // --- helpers -------------------------------------------------------------------

    private ScenePayload fullScenePayload() {
        return new ScenePayload(
                42, 7, "PUBLISHED", 3, "The innkeeper", "A low fire burns in the hearth.",
                List.of(fullChoicePayload()), List.of(12), List.of(5));
    }

    private ChoicePayload fullChoicePayload() {
        return new ChoicePayload(
                118, "Trust him", "You chose to trust the innkeeper.", 58, 2,
                List.of(31),
                List.of(new RequirementPayload("ITEM", null, 31)),
                List.of(new EffectPayload("GRANT_ITEM", 31, null, null, null, null)));
    }

    private Set<String> fieldsOf(Object payload) {
        return asMap(payload).keySet();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object payload) {
        return new LinkedHashMap<>(json.convertValue(payload, Map.class));
    }

    @SuppressWarnings("unchecked")
    private Set<String> propertiesOf(String schemaName) {
        Map<String, Object> schema = (Map<String, Object>) schemas.get(schemaName);
        assertThat(schema).as("schema '%s' must exist in the contract", schemaName).isNotNull();
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties).as("schema '%s' must declare properties", schemaName).isNotNull();
        return properties.keySet();
    }

    @SuppressWarnings("unchecked")
    private List<String> allowedKeysOf(String schemaName, String property) {
        Map<String, Object> schema = (Map<String, Object>) schemas.get(schemaName);
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        Map<String, Object> field = (Map<String, Object>) properties.get(property);
        Map<String, Object> propertyNames = (Map<String, Object>) field.get("propertyNames");
        return (List<String>) propertyNames.get("enum");
    }
}
