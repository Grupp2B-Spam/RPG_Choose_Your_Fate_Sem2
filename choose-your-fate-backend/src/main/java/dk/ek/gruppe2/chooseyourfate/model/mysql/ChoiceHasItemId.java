package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ChoiceHasItemId implements Serializable {

    private Integer choiceId;
    private Integer itemId;

    public ChoiceHasItemId() {}

    public ChoiceHasItemId(Integer choiceId, Integer itemId) {
        this.choiceId = choiceId;
        this.itemId = itemId;
    }

    public Integer getChoiceId() { return choiceId; }
    public void setChoiceId(Integer choiceId) { this.choiceId = choiceId; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChoiceHasItemId that)) return false;
        return Objects.equals(choiceId, that.choiceId) &&
                Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(choiceId, itemId);
    }
}
