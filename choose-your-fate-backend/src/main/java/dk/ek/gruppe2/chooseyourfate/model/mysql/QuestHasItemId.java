package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class QuestHasItemId implements Serializable {

    private Integer questId;
    private Integer itemId;

    public QuestHasItemId() {}

    public QuestHasItemId(Integer questId, Integer itemId) {
        this.questId = questId;
        this.itemId = itemId;
    }

    public Integer getQuestId() { return questId; }
    public void setQuestId(Integer questId) { this.questId = questId; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestHasItemId that)) return false;
        return Objects.equals(questId, that.questId) &&
                Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questId, itemId);
    }
}
