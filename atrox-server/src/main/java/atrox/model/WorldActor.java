package atrox.model;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@Entity @NoArgsConstructor
public class WorldActor extends CanonicallyNamedEntity {

    public WorldActor (String name) { super(name); }

}
