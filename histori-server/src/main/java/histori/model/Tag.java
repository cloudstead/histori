package histori.model;

import histori.ApiConstants;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.Column;
import javax.persistence.Entity;

import static histori.ApiConstants.NAME_MAXLEN;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class Tag extends CanonicalEntity implements Shardable {

    @Override public String getHashToShardField() { return "canonicalName"; }

    public Tag (String name) { super(name); }

    public Tag (String name, String type) { super(name); setTagType(type); }

    public Tag(String tagName, TagType tagType) {
        this(tagName, tagType == null ? null : tagType.getCanonicalName());
    }

    @Column(length=NAME_MAXLEN)
    private String tagType;

    public String getTagType() { return tagType == null ? null : canonicalize(tagType); }
    public void setTagType(String tagType) { this.tagType = (tagType != null) ? canonicalize(tagType) : null; }

    public boolean hasTagType() { return !empty(tagType); }

    @Override public String toString() {
        return getCanonicalName()+"/"+(hasTagType() ? getTagType() : ApiConstants.MATCH_NULL_TYPE);
    }
}
