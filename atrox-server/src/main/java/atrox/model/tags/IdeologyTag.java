package atrox.model.tags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Accessors(chain=true)
public class IdeologyTag extends EntityTag {

    public static final String[] UNIQUES = {"ideology", "entity"};
    @Override @Transient @JsonIgnore public String[] getUniqueProperties() { return UNIQUES; }

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String ideology;

    // Can be just about anything
    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String entity;

}
