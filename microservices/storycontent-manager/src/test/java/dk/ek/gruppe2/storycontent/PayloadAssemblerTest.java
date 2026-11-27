package dk.ek.gruppe2.storycontent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import dk.ek.gruppe2.storycontent.domain.Chapter;
import dk.ek.gruppe2.storycontent.domain.Choice;
import dk.ek.gruppe2.storycontent.domain.ChoiceEffect;
import dk.ek.gruppe2.storycontent.domain.ChoiceRequirement;
import dk.ek.gruppe2.storycontent.domain.Enums.CharacterStat;
import dk.ek.gruppe2.storycontent.domain.Enums.DefinitionStatus;
import dk.ek.gruppe2.storycontent.domain.Enums.EquipmentSlot;
import dk.ek.gruppe2.storycontent.domain.Enums.ItemType;
import dk.ek.gruppe2.storycontent.domain.Item;
import dk.ek.gruppe2.storycontent.domain.Npc;
import dk.ek.gruppe2.storycontent.domain.Race;
import dk.ek.gruppe2.storycontent.domain.Scene;
import dk.ek.gruppe2.storycontent.messaging.payload.ChoicePayload;
import dk.ek.gruppe2.storycontent.messaging.payload.ItemDefinitionPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.RaceDefinitionPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.ScenePayload;
import dk.ek.gruppe2.storycontent.service.PayloadAssembler;

/**
 * Unit tests for the mapping from entities to published payloads. Ids are set by reflection
 * because they are database-generated and this test has no database.
 */
@DisplayName("Entities map to the published payload shapes")
class PayloadAssemblerTest {

    private final PayloadAssembler assembler = new PayloadAssembler();

    @Test
    @DisplayName("grantedItemIds is derived from the GRANT_ITEM effects, not stored twice")
    void grantedItemIdsAreDerivedFromEffects() {
        Scene scene = sceneWithOneChoice(choice -> {
            choice.addEffect(ChoiceEffect.grantItem(31, 0));
            choice.addEffect(ChoiceEffect.modifyStat(CharacterStat.CHARISMA, -1, 1));
            choice.addEffect(ChoiceEffect.grantItem(32, 2));
        });

        ChoicePayload choice = assembler.scene(scene).choices().get(0);

        // The old field and the new one cannot disagree, because there is one source.
        assertThat(choice.grantedItemIds()).containsExactly(31, 32);
        assertThat(choice.effects()).hasSize(3);
    }

    @Test
    @DisplayName("statistic names are published in lower case, as the contract has since 1.0.0")
    void statisticNamesArePublishedInLowerCase() {
        Scene scene = sceneWithOneChoice(choice ->
                choice.addEffect(ChoiceEffect.modifyStat(CharacterStat.INTELLIGENCE, 2, 0)));

        assertThat(assembler.scene(scene).choices().get(0).effects().get(0).stat())
                .isEqualTo("intelligence");
    }

    @Test
    @DisplayName("fields that do not belong to an effect's kind stay null")
    void onlyTheFieldsBelongingToTheKindAreSet() {
        Scene scene = sceneWithOneChoice(choice -> {
            choice.addEffect(ChoiceEffect.grantFlag("bruised", 0));
            choice.addRequirement(ChoiceRequirement.storyFlag("festival-access", 0));
        });

        var effect = assembler.scene(scene).choices().get(0).effects().get(0);
        assertThat(effect.kind()).isEqualTo("GRANT_FLAG");
        assertThat(effect.flag()).isEqualTo("bruised");
        assertThat(effect.itemId()).isNull();
        assertThat(effect.questId()).isNull();
        assertThat(effect.stat()).isNull();
        assertThat(effect.amount()).isNull();

        var requirement = assembler.scene(scene).choices().get(0).requirements().get(0);
        assertThat(requirement.kind()).isEqualTo("STORY_FLAG");
        assertThat(requirement.flag()).isEqualTo("festival-access");
        assertThat(requirement.itemId()).isNull();
    }

    @Test
    @DisplayName("the beat is carried through: it is what makes a recorded path self-contained")
    void theBeatIsCarried() {
        Scene scene = sceneWithOneChoice(choice -> {
        });

        assertThat(assembler.scene(scene).choices().get(0).beat())
                .isEqualTo("You chose to trust the innkeeper.");
    }

    @Test
    void sceneCarriesItsVersionAndChapter() {
        Scene scene = sceneWithOneChoice(choice -> {
        });
        ReflectionTestUtils.setField(scene, "version", 7);

        ScenePayload payload = assembler.scene(scene);

        assertThat(payload.sceneId()).isEqualTo(42);
        assertThat(payload.sceneVersion()).isEqualTo(7);
        assertThat(payload.chapterId()).isEqualTo(3);
        assertThat(payload.status()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("npcIds are published in a stable order")
    void npcIdsArePublishedInAStableOrder() {
        Scene scene = sceneWithOneChoice(choice -> {
        });
        Race race = withId(new Race("smalls", withId(new Chapter("A chapter"), 3)), 1);
        scene.getNpcs().add(withId(new Npc("Merchant Vonn", race), 12));
        scene.getNpcs().add(withId(new Npc("Captain Elira", race), 4));

        // An order that varies between publications of unchanged content makes every
        // republication look like a change to a consumer comparing payloads.
        assertThat(assembler.scene(scene).npcIds()).containsExactly(4, 12);
    }

    @Test
    void itemCarriesTypeSlotAndStatsInTheContractSpelling() {
        Item item = withId(new Item("Rusty Sword", "Old sword.", ItemType.EQUIPMENT,
                EquipmentSlot.MAINHAND), 31);
        item.getModifiers().put(CharacterStat.CHARISMA, 2);
        item.getRequirements().put(CharacterStat.FASHION, 1);

        ItemDefinitionPayload payload = assembler.item(item);

        assertThat(payload.type()).isEqualTo("EQUIPMENT");
        assertThat(payload.slot()).isEqualTo("MAINHAND");
        assertThat(payload.status()).isEqualTo(DefinitionStatus.ACTIVE.name());
        assertThat(payload.modifiers()).containsExactly(java.util.Map.entry("charisma", 2));
        assertThat(payload.requirements()).containsExactly(java.util.Map.entry("fashion", 1));
    }

    @Test
    @DisplayName("the race event carries the chapter's starting scene, so no second lookup is needed")
    void raceCarriesTheChaptersStartingScene() {
        Chapter chapter = withId(new Chapter("Tall forest, small people"), 4);
        chapter.setStartingSceneId(104);
        Race race = withId(new Race("smalls", chapter), 3);

        RaceDefinitionPayload payload = assembler.race(race);

        assertThat(payload.startingChapterId()).isEqualTo(4);
        assertThat(payload.startingSceneId()).isEqualTo(104);
    }

    @Test
    @DisplayName("a chapter with no starting scene yet publishes null rather than a guess")
    void raceWithoutAStartingSceneCarriesNull() {
        Chapter chapter = withId(new Chapter("A new chapter"), 9);
        Race race = withId(new Race("mediums", chapter), 5);

        // Unfinished content. Substituting a value here would put new characters somewhere
        // an author never chose.
        assertThat(assembler.race(race).startingSceneId()).isNull();
    }

    // --- fixtures ------------------------------------------------------------------

    private Scene sceneWithOneChoice(java.util.function.Consumer<Choice> customise) {
        Chapter chapter = withId(new Chapter("Moonlit Ruins"), 3);
        Scene scene = withId(new Scene(chapter, "The innkeeper",
                "A low fire burns in the hearth."), 42);
        Scene destination = withId(new Scene(chapter, "Market Square", null), 58);

        Choice choice = withId(new Choice(destination, "Trust him",
                "You chose to trust the innkeeper.", (short) 2, 0), 118);
        customise.accept(choice);
        scene.addChoice(choice);
        return scene;
    }

    private <T> T withId(T entity, int id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
